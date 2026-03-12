package com.alexuva.app.videoadapter.ffmpeg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaStreamInfo {
    private VideoStreamInfo video;
    private List<AudioStreamInfo> audio;
    private List<SubtitleStreamInfo> subtitle;
    private String duration;
    private String size;
}
