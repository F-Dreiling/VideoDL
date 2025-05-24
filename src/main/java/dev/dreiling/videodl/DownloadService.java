package dev.dreiling.videodl;

import javafx.application.Platform;
import java.io.*;
import java.util.function.Consumer;

public class DownloadService {

    // Utility method to extract native executables from the jar resource /bin
    private static File extractExe(String exeName) throws IOException {
        return Utils.extractExecutable(exeName);
    }

    // Extract the error occurred during download
    private static String extractErrorMessage(String output) {
        // Find lines containing "error" or "unable"
        StringBuilder errors = new StringBuilder();
        for (String line : output.split("\\R")) {
            if (line.toLowerCase().contains("error") || line.toLowerCase().contains("unable")) {
                errors.append(line);
            }
        }
        return errors.length() > 0 ? errors.toString().trim() : "Unknown error occurred.";
    }

    public static void downloadVideo(String downloadsDir, String videoUrl, String quality, Consumer<Double> progressCallback, Consumer<String> statusCallback) throws Exception {

        // Extract yt-dlp.exe and ffmpeg.exe from resources/bin to temp files
        File ytDlpExe = extractExe("yt-dlp.exe");
        File ffmpegExe = extractExe("ffmpeg.exe");

        // Format Selected Quality
        String formatCode = switch (quality) {
            case "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]";
            case "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]";
            case "480p" -> "bestvideo[height<=480]+bestaudio/best[height<=480]";
            case "360p" -> "bestvideo[height<=360]+bestaudio/best[height<=360]";
            case "Audio only" -> "bestaudio";
            default -> "best[height<=360]";
        };

        // Set file output
        String outputPath = new File(downloadsDir, "%(title)s.%(ext)s").getAbsolutePath();

        // Build Process
        ProcessBuilder builder = new ProcessBuilder(
                ytDlpExe.getAbsolutePath(),
                "-f", formatCode,
                "--merge-output-format", "mp4",
                "--ffmpeg-location", ffmpegExe.getAbsolutePath(),
                "-o", outputPath,
                videoUrl
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // Read and Update Progress and Log output
        StringBuilder outputLog = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            String parsed = line.trim();
            outputLog.append(parsed).append(System.lineSeparator());

            // Call back status for logging
            Platform.runLater(() -> statusCallback.accept(parsed));

            // Example line: [download]  42.1% of 5.00MiB at 1.23MiB/s ETA 00:10
            if (parsed.startsWith("[download]")) {
                int percentIndex = parsed.indexOf('%');
                if (percentIndex != -1) {
                    try {
                        int start = parsed.lastIndexOf(' ', percentIndex - 1) + 1;
                        String percentage = parsed.substring(start, percentIndex).trim();
                        double progress = Double.parseDouble(percentage) / 100.0;

                        Platform.runLater(() -> progressCallback.accept(progress));
                    }
                    catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {}
                }
            }
        }

        // Wait for process to finish and throw exception on error
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new Exception(extractErrorMessage(outputLog.toString()));
        }
    }
}