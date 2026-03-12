package com.alexuva.app.videoadapter.ffmpeg;

import com.alexuva.app.videoadapter.exceptions.FfmpegException;
import com.alexuva.app.videoadapter.exceptions.FfprobeException;
import com.alexuva.app.videoadapter.util.GpuInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public class FfmpegRunner {

    private Path filePath;
    private Path outputPath;
    private String mode;
    private int qualityLvl;
    private boolean isPreserveHDR;
    private GpuInfo gpuInfo;
    private MediaStreamInfo mediaStreamInfo;
    private Process process;

    public void run(Consumer<Double> onProgress) throws FfmpegException {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("ffmpeg");
            cmd.add("-i");
            cmd.add(filePath.toString());
            buildVideoArgs(cmd);
            buildAudioArgs(cmd);
            buildSubtitleArgs(cmd);
            cmd.add("-progress");
            cmd.add("pipe:1");
            cmd.add("-nostats");
            cmd.add(outputPath.toString());

            process = new ProcessBuilder(cmd).redirectErrorStream(true).start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("frame=")) {
                        String frameText = line.substring("frame=".length()).trim().split("\\s+")[0];
                        int actualFrame = Integer.parseInt(frameText);

                        double progress = Math.min((actualFrame / mediaStreamInfo.getVideo().totalFrames()) * 100.0, 100.0);
                        onProgress.accept(progress);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 && exitCode != 255) throw new FfmpegException("FFmpeg process exited with code " + exitCode);

        } catch (IOException e) {
            throw new FfmpegException("Error executing command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FfmpegException("Command execution interrupted: " + e.getMessage());
        }
    }
    public void stop() {
        if (process != null) process.destroy();
    }
    private void buildVideoArgs(List<String> args) {
        switch (mode) {
            case "best" -> {
                //Common
                args.add("-c:v");
                args.add(gpuInfo.videoCodec("best"));
                args.addAll(gpuInfo.videoConfig(mode, String.valueOf(qualityLvl)));

                if (isPreserveHDR) {
                    //HDR
                    args.add("-color_primaries"); args.add("bt2020");
                    args.add("-color_trc"); args.add("smpte2084");
                    args.add("-colorspace"); args.add("bt2020nc");
                } else if(mediaStreamInfo.getVideo().isHDR()){
                    //HDR -> SDR
                    args.add("-vf"); args.add("zscale=transfer=bt709:matrix=bt709:primaries=bt709");
                }
            }
            case "better" -> {
                //Common
                args.add("-c:v");
                args.add(gpuInfo.videoCodec("better"));
                args.addAll(gpuInfo.videoConfig(mode, String.valueOf(qualityLvl)));

                if (!mediaStreamInfo.getVideo().isHDR()) {
                    //SDR
                    args.add("-level"); args.add("4.1");

                    if (mediaStreamInfo.getVideo().isHighBitDepth()) {
                        //SDR 10bit -> 8bit
                        args.add("-pix_fmt"); args.add("yuv420p");
                        args.add("-vf"); args.add("format=nv12,setpts=PTS-STARTPTS");
                    }
                } else {
                    //HDR -> SDR
                    args.add("-vf"); args.add("zscale=transfer=bt709:matrix=bt709:primaries=bt709");
                }
            }
            case "compatible" -> {
                //Common
                args.add("-c:v");
                args.add(gpuInfo.videoCodec("compatible"));
                args.addAll(gpuInfo.videoConfig(mode, String.valueOf(qualityLvl)));

                if (!mediaStreamInfo.getVideo().isHDR()) {
                    //SDR
                    args.add("-movflags"); args.add("faststart");
                    args.add("-level"); args.add("4.1");

                    if (mediaStreamInfo.getVideo().isHighBitDepth()) {
                        //SDR 10bit -> 8bit
                        args.add("-pix_fmt"); args.add("yuv420p");
                        args.add("-vf"); args.add("format=nv12,setpts=PTS-STARTPTS");
                    }
                } else {
                    //HDR -> SDR
                    args.add("-pix_fmt"); args.add("yuv420p");
                    args.add("-vf"); args.add("zscale=transfer=bt709:matrix=bt709:primaries=bt709");
                }

            }
        }
    }
    private void buildAudioArgs(List<String> args) {
        switch (mode) {
            case "best" -> {
                args.add("-map"); args.add("0");
                args.add("-c:a"); args.add("copy");
            }
            case "better" -> {
                List<AudioStreamInfo> audios = mediaStreamInfo.getAudio();
                for(int i = 0; i < audios.size(); i++) {

                    AudioStreamInfo audio = audios.get(i);
                    int channels = audio.getChannels();
                    String channelLayout = audio.getChannelLayout();
                    boolean isAtmos = audio.isAtmos();

                    args.add("-c:a:" + i); args.add("ac3");

                    if (channels >= 6 || isAtmos) {
                        channels = 6;

                        args.add("-b:a:" + i); args.add("640k");
                        args.add("-ac:a:" + i); args.add(String.valueOf(channels));

                        if (isAtmos) {
                            args.add("-filter:a:" + i);
                            args.add("pan=5.1|FL=FL|FR=FR|FC=FC|LFE=LFE|BL=SL|BR=SR");
                        } else if (channelLayout.matches(".*5\\.1\\(side\\).*")) {
                            args.add("-filter:a:" + i);
                            args.add("channelmap=0|1|2|3|4|5:5.1");
                        }
                    } else if (channels >= 3) {
                        channels = 2;
                        args.add("-b:a:" + i); args.add("384k");
                        args.add("-ac:a:" + i); args.add(String.valueOf(channels));
                    } else {
                        channels = Math.max(channels, 1);
                        args.add("-b:a:" + i); args.add("256k");
                        args.add("-ac:a:" + i); args.add(String.valueOf(channels));
                    }
                }
            }
            case "compatible" -> {
                args.add("-map"); args.add("0:v:0");
                args.add("-map"); args.add("0:a:0");
                args.add("-c:a"); args.add("aac");
                args.add("-b:a"); args.add("128k");
                args.add("-ac"); args.add("2");
            }
        }
    }
    private void buildSubtitleArgs(List<String> args) {
        switch (mode) {
            case "best","better","compatible" -> {
                args.add("-map"); args.add("0");
                args.add("-map"); args.add("-0:s:m:codec:hdmv_pgs_subtitle");
                args.add("-c:s"); args.add("copy");
            }
        }
    }
}
