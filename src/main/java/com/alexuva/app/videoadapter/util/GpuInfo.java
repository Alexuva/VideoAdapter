package com.alexuva.app.videoadapter.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class GpuInfo {
    private GpuVendor vendor;
    private String name;

    public String videoCodec(String mode) {
        return switch (vendor) {
            case AMD    -> mode.equals("best") ? "hevc_amf"          : "h264_amf";
            case NVIDIA -> mode.equals("best") ? "hevc_nvenc"        : "h264_nvenc";
            case APPLE  -> mode.equals("best") ? "hevc_videotoolbox" : "h264_videotoolbox";
            case CPU    -> mode.equals("best") ? "libx265"           : "libx264";
        };
    };

    public List<String> videoConfig(String mode, String qualityLvl) {
        return switch (vendor) {
            case AMD -> switch (mode) {
                case "best"     -> new ArrayList<>(List.of("-rc", "cqp", "-qp_b", qualityLvl, "-qp_i", "14", "-qp_p", "16"));
                case "better"   -> new ArrayList<>(List.of("-rc", "cqp", "-qp_b", qualityLvl, "-qp_i", "16", "-qp_p", "18", "-profile:v", "high"));
                default         -> new ArrayList<>(List.of("-rc", "cqp", "-qp_b", qualityLvl, "-qp_i", "18", "-qp_p", "20", "-profile:v", "high"));
            };
            case NVIDIA -> switch (mode) {
                case "best"   -> new ArrayList<>(List.of("-rc", "cqp", "-quality", "quality", "-qp_b", qualityLvl, "-qp_i", "14", "-qp_p", "16"));
                case "better" -> new ArrayList<>(List.of("-rc", "cqp", "-quality", "quality", "-qp_b", qualityLvl, "-qp_i", "16", "-qp_p", "18", "-profile:v", "high"));
                default       -> new ArrayList<>(List.of("-rc", "cqp", "-quality", "quality", "-qp_b", qualityLvl, "-qp_i", "18", "-qp_p", "20", "-profile:v", "high"));
            };
            case APPLE -> new ArrayList<>(List.of("-q:v", "75", "-realtime", "false"));
            case CPU -> switch (mode) {
                case "best"   -> new ArrayList<>(List.of("-crf", qualityLvl, "-preset", "slow"));
                case "better" -> new ArrayList<>(List.of("-crf", qualityLvl, "-preset", "medium", "-profile:v", "high"));
                default       -> new ArrayList<>(List.of("-crf", qualityLvl, "-preset", "fast", "-profile:v", "high"));
            };
        };
    }
}
