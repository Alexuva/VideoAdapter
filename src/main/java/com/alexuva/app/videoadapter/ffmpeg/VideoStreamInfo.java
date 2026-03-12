package com.alexuva.app.videoadapter.ffmpeg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoStreamInfo {
    private String codecName;
    private String title;
    private int width;
    private int height;
    private String colorTransfer;
    private String colorPrimaries;
    private String pixFmt;
    private Integer bitsPerRawSample;
    private String avgFrameRate;
    private String duration;
    private String size;

    public boolean isHDR() {
        if (colorTransfer == null || colorPrimaries == null) return false;
        return (colorTransfer.contains("smpte2084") || colorTransfer.contains("arib-std-b67")) && colorPrimaries.contains("bt2020");
    }
    public boolean isHighBitDepth() {
        return (bitsPerRawSample != null && bitsPerRawSample > 8) || (pixFmt != null && pixFmt.matches(".*p(9|1[0-6]).*"));
    }
    public double totalFrames() {
        String[] fpsText = avgFrameRate.split("/");
        double fps = Double.parseDouble(fpsText[0]) / Double.parseDouble(fpsText[1]);
        return Double.parseDouble(duration) * fps;
    }
    public String formattedSize() {
        double parsedSize = Double.parseDouble(size);
        double formattedSizeMB = parsedSize / (1000 * 1000);
        double formattedSizeGB = parsedSize / (1000 * 1000 * 1000);

        if(formattedSizeMB <= 1000) return String.format("%.2f", formattedSizeMB).concat(" ").concat("MB");
        return String.format("%.2f", formattedSizeGB).concat(" ").concat("GB");
    }
    public String formattedDuration() {
        long totalSeconds = (long) Double.parseDouble(duration);
        Duration duration = Duration.ofSeconds(totalSeconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
