package com.alexuva.app.videoadapter.ffmpeg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubtitleStreamInfo {
    private String codecName;
    private String language;
}
