package dev.dreiling.videodl;

import javafx.application.Platform;
import java.io.*;
import java.util.function.Consumer;

public class DownloadService {

    private static volatile Process currentProcess;
    private static volatile boolean cancelled = false;

    public static void cancel() {
        cancelled = true;
        if (currentProcess != null) {
            currentProcess.destroy();
        }
    }

    public static boolean downloadVideo(String downloadsDir, String videoUrl, String quality,
                                     Consumer<Double> progressCallback, Consumer<String> statusCallback) throws Exception {
        cancelled = false;

        // Extract yt-dlp.exe and ffmpeg.exe from resources/bin to temp files
        File ytDlpExe = Utils.extractExecutable("yt-dlp.exe");
        File ffmpegExe = Utils.extractExecutable("ffmpeg.exe");

        // Extract title and sanitize
        String rawTitle = Utils.getVideoTitle(ytDlpExe, videoUrl);
        String sanitizedTitle = Utils.sanitizeTitle(rawTitle);

        // Set output path using sanitized title
        String outputPattern = sanitizedTitle + ".%(ext)s";
        String outputPath = new File(downloadsDir, outputPattern).getAbsolutePath();

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
        currentProcess = builder.start();

        // Read and Update Progress and Log output
        BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
        StringBuilder outputLog = new StringBuilder();
        String line;

        try {
            while ((line = reader.readLine()) != null) {

                if (cancelled) return false;

                // Clean status message and filter for log
                String parsed = Utils.cleanStatus(line.trim());
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
            int exitCode = currentProcess.waitFor();

            // Write log and history
            Utils.writeLog(outputLog.toString());
            Utils.writeHistory(exitCode, sanitizedTitle, videoUrl);

            if (exitCode != 0) {
                throw new Exception(Utils.extractErrorMessage(outputLog.toString()));
            }
            return true;
        }
        finally {
            reader.close();

            if (cancelled) {
                try {
                    if (cancelled && currentProcess.isAlive()) {
                        currentProcess.destroyForcibly();
                        currentProcess.waitFor();
                    }
                    Thread.sleep(200); // give OS time to release file locks
                }
                catch (InterruptedException ignored) {}

                Utils.cleanupPartialFiles(downloadsDir, sanitizedTitle);
            }
        }
    }
}