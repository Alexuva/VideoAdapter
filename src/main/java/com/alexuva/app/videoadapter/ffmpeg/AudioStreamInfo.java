package com.alexuva.app.videoadapter.ffmpeg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudioStreamInfo {
    private String codecName;
    private int channels;
    private String channelLayout;
    private String profile;
    private String language;
    private Map<String, String> tags;

    public boolean isAtmos() {
        if (profile != null && (profile.contains("Atmos") || profile.contains("JOC"))) return true;
        if (codecName.equals("eac3") && channels == 16) return true;
        if (codecName.equals("trueHD") && profile != null && profile.contains("TrueHD")) return true;
        return false;
    }
}
