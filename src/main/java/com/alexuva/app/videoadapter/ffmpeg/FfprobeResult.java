package com.alexuva.app.videoadapter.ffmpeg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FfprobeResult {

    private List<RawStream> streams;
    private FormatInfo format;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RawStream {
        @JsonProperty("codec_type")
        private String codecType;
        @JsonProperty("codec_name")
        private String codecName;
        @JsonProperty("width")
        private Integer width;
        @JsonProperty("height")
        private Integer height;
        @JsonProperty("color_transfer") //HDR
        private String colorTransfer;
        @JsonProperty("color_primaries")
        private String colorPrimaries;
        @JsonProperty("pix_fmt")
        private String pixFmt;
        @JsonProperty("bits_per_raw_sample")
        private Integer bitsPerRawSample;
        @JsonProperty("channels")
        private Integer channels;
        @JsonProperty("channel_layout")
        private String channelLayout;
        @JsonProperty("avg_frame_rate")
        private String avgFrameRate;
        @JsonProperty("profile")
        private String profile;
        @JsonProperty("tags")
        private Map<String, String> tags;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormatInfo {
        private String duration;
        private String size;
        @JsonProperty("tags")
        private Map<String, String> tags;
    }

}

