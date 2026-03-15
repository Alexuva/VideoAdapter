package com.alexuva.app.videoadapter.util;

import com.alexuva.app.videoadapter.exceptions.FfmpegLocatorException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FfmpegLocator {

    private static Path ffmpegPath;
    private static Path ffprobePath;

    public static Path ffmpeg() throws FfmpegLocatorException, IOException{
        if (ffmpegPath == null) ffmpegPath = extract("ffmpeg");
        return ffmpegPath;
    }

    public static Path ffprobe() throws FfmpegLocatorException, IOException{
        if (ffprobePath == null) ffprobePath = extract("ffprobe");
        return ffprobePath;
    }

    private static Path extract(String binary) throws FfmpegLocatorException, IOException {
        String resourcePath = getResourcePath(binary);
        Path tempDir = Files.createTempDirectory("videoadapter");
        Path output = tempDir.resolve(binary + (isWindows() ? ".exe" : ""));

        try (
                InputStream is = FfmpegLocator.class.getResourceAsStream(resourcePath)
        ) {
            if (is == null) throw new FfmpegLocatorException("Could not find binary" + resourcePath);
            Files.copy(is, output);
        }

        if (!output.toFile().setExecutable(true)) {
            throw new FfmpegLocatorException("Could not set executable permission" + output);
        }

        return output;
    }

    private static String getResourcePath(String binary) throws FfmpegLocatorException{
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("win")) {
            return "/bin/windows/" + binary + ".exe";
        } else if (os.contains("mac")) {
            return arch.contains("arm") ? "/bin/mac-arm/" + binary : "/bin/mac-intel/" + binary;
        }

        throw new FfmpegLocatorException("SO no soportado");

    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }



}
