package com.alexuva.app.videoadapter.util;

import com.alexuva.app.videoadapter.exceptions.GpuDetectorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class GpuDetector {

    public static GpuInfo detect() throws GpuDetectorException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String output;
        if (os.contains("win")) {
            output = runCommand("powershell", "-Command", "Get-CimInstance Win32_VideoController | Select-Object -ExpandProperty Name");
        } else if (os.contains("mac")) {
            output = runCommand("system_profiler", "SPDisplaysDataType");
        } else {
            output = runCommand("lspci");
        }

        return parseGpuOutput(output);
    }

    private static GpuInfo parseGpuOutput(String output) throws GpuDetectorException {
        if (output == null || output.trim().isEmpty()) throw new GpuDetectorException("No GPU detected");

        String lower = output.toLowerCase(Locale.ROOT);

        if (lower.contains("apple m")) {
            return new GpuInfo(GpuVendor.APPLE, extractLine(output, "apple m"));
        }
        if (lower.contains("amd") || lower.contains("radeon")) {
            return new GpuInfo(GpuVendor.AMD, extractLine(output, "amd", "radeon"));

        }
        if (lower.contains("nvidia") || lower.contains("geforce") || lower.contains("quadro")) {
            return new GpuInfo(GpuVendor.NVIDIA, extractLine(output, "nvidia", "geforce", "quadro"));
        }

        throw new GpuDetectorException("No GPU detected");
    };

    private static String runCommand(String... cmd) throws GpuDetectorException {
        try {
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder string = new StringBuilder();

            try(
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    string.append(line).append("\n");
                }
            };

            process.waitFor();
            return string.toString();
        } catch (IOException e) {
            throw new GpuDetectorException("Error executing command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GpuDetectorException("Command execution interrupted: " + e.getMessage());
        }
    };

    private static String extractLine (String output, String... keywords) {
      for (String line : output.split("\n")){
          String lower = line.toLowerCase(Locale.ROOT);
          for (String keyword : keywords) {
              if (lower.contains(keyword)) return line.trim().replace(":", "");
          }
      }
      return "Unknown";
    };

}