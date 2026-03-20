package com.alexuva.app.videoadapter.ffmpeg;

import com.alexuva.app.videoadapter.exceptions.FfmpegLocatorException;
import com.alexuva.app.videoadapter.exceptions.FfprobeException;
import com.alexuva.app.videoadapter.util.FfmpegLocator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FfprobeRunner {
    private static Process process;
    private static final List<String> BASE_CMD = new ArrayList<>(List.of(
            "ffprobe",
            "-v",
            "error",
            "-show_entries",
            "stream=codec_type,codec_name,width,height,color_transfer,color_primaries,pix_fmt,bits_per_raw_sample,channels,channel_layout,avg_frame_rate,profile:stream_tags:format_tags=title",
            "-show_entries",
            "format=duration,size",
            "-of",
            "json"
    ));

    public static MediaStreamInfo analyze(Path filePath) throws FfprobeException {
        try {

            List<String> cmd = new ArrayList<>();
            cmd.add(FfmpegLocator.ffprobe().toString());
            cmd.add("-v");
            cmd.add("error");
            cmd.add("-show_entries");
            cmd.add("stream=codec_type,codec_name,width,height,color_transfer,color_primaries,pix_fmt,bits_per_raw_sample,channels,channel_layout,avg_frame_rate,profile:stream_tags:format_tags=title");
            cmd.add("-show_entries");
            cmd.add("format=duration,size");
            cmd.add("-of");
            cmd.add("json");
            cmd.add(filePath.toString());

            process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();

            ObjectMapper mapper = JsonMapper.builder()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .build();

            return buildMediaInfo(mapper.readValue(output, FfprobeResult.class));

        }catch (FfmpegLocatorException e){

            throw new FfprobeException("Error locating FFprobe binary: " + e.getMessage());

        }catch (IOException e){

            throw new FfprobeException("Error executing command: " + e.getMessage());

        }catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new FfprobeException("Command execution interrupted: " + e.getMessage());

        }
    }
    private static MediaStreamInfo buildMediaInfo(FfprobeResult result) {
        MediaStreamInfo mediaInfo = new MediaStreamInfo();

        //Video stream
        VideoStreamInfo videoStreams = new VideoStreamInfo();

        //Audio list of streams
        List<AudioStreamInfo> audioStreams = new ArrayList<>();

        //Subtitle list of streams
        List<SubtitleStreamInfo> subtitleStreams = new ArrayList<>();

        result.getStreams()
            .forEach(stream -> {
                if (stream.getCodecType() != null && !stream.getCodecType().equals("attachment")) {
                    switch (stream.getCodecType()) {
                        case "video" -> {
                            videoStreams.setCodecName(stream.getCodecName());
                            videoStreams.setWidth(stream.getWidth());
                            videoStreams.setHeight(stream.getHeight());
                            videoStreams.setAvgFrameRate(stream.getAvgFrameRate());
                            videoStreams.setColorTransfer(stream.getColorTransfer());
                            videoStreams.setColorPrimaries(stream.getColorPrimaries());
                            videoStreams.setPixFmt(stream.getPixFmt());
                            videoStreams.setBitsPerRawSample(stream.getBitsPerRawSample());
                            videoStreams.setDuration(result.getFormat().getDuration());
                            videoStreams.setSize(result.getFormat().getSize());
                            result.getFormat().getTags().forEach((key, value) -> {
                               if (key.equals("title")) videoStreams.setTitle(value);
                            });
                        }
                        case "audio" -> {
                            AudioStreamInfo audioInfo = new AudioStreamInfo(
                                    stream.getCodecName(),
                                    stream.getChannels(),
                                    stream.getChannelLayout(),
                                    stream.getProfile(),
                                    stream.getTags() != null
                                            ? stream.getTags().get("title") + " (" + stream.getTags().get("language") + ")"
                                            : "Desconocido",
                                    stream.getTags()
                            );
                            audioStreams.add(audioInfo);
                        }
                        case "subtitle" -> {
                            SubtitleStreamInfo subtitleInfo = new SubtitleStreamInfo(
                                    stream.getCodecName(),
                                    stream.getTags() != null ? stream.getTags().get("language") : "Desconocido"
                            );
                            subtitleStreams.add(subtitleInfo);
                        }
                    }
                }
            }
        );

        //Size and duration
        mediaInfo.setSize(result.getFormat().getSize());
        mediaInfo.setDuration(result.getFormat().getDuration());

        mediaInfo.setVideo(videoStreams);
        mediaInfo.setAudio(audioStreams);
        mediaInfo.setSubtitle(subtitleStreams);

        return mediaInfo;
    };
    public static void stop() {
        if (process != null) process.destroy();
    }
}
