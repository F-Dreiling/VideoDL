package dev.dreiling.videodl;

import javafx.application.Platform;
import java.io.*;
import java.util.function.Consumer;

public class DownloadService {

    public static void downloadVideo(String downloadsDir, String videoUrl, String quality, Consumer<Double> progressCallback, Consumer<String> statusCallback) throws Exception {

        // Extract yt-dlp.exe and ffmpeg.exe from resources/bin to temp files
        File ytDlpExe = Utils.extractExecutable("yt-dlp.exe");
        File ffmpegExe = Utils.extractExecutable("ffmpeg.exe");

        // Extract title and sanitize
        String rawTitle = Utils.getVideoTitle(ytDlpExe, videoUrl);
        String sanitizedTitle = Utils.sanitizeTitle(rawTitle);

        // Set output path using sanitized title
        String outputPath = new File(downloadsDir, sanitizedTitle + ".%(ext)s").getAbsolutePath();

        // Format Selected Quality
        String formatCode = Utils.getFormatCode(quality);

        // Build Process and start
        ProcessBuilder builder = new ProcessBuilder(
                ytDlpExe.getAbsolutePath(),
                "-f", formatCode,
                "--no-warnings",
                "--merge-output-format", "mp4",
                "--ffmpeg-location", ffmpegExe.getAbsolutePath(),
                "-o", outputPath,
                videoUrl
        );
        builder.redirectErrorStream(true);
        Process process = builder.start();

        // Read and Update Progress and Log output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder outputLog = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            String parsed = line.trim();
            String message = Utils.filterMessage(parsed);

            if (!message.isEmpty()) {
                outputLog.append(message).append(System.lineSeparator());
            }

            // Call back progress and status for UI updates
            Double progress = Utils.parseProgress(parsed);
            if (progress != null) {
                Platform.runLater(() -> progressCallback.accept(progress));
            }
            Platform.runLater(() -> statusCallback.accept(parsed));
        }

        // Wait for the process to finish
        int exitCode = process.waitFor();

        // Write log and history
        Utils.writeLog(outputLog.toString());
        Utils.writeHistory(exitCode, sanitizedTitle, videoUrl);

        if (exitCode != 0) {
            throw new Exception(Utils.extractErrorMessage(outputLog.toString()));
        }
    }
}