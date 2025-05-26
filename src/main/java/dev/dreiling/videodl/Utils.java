package dev.dreiling.videodl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    private static final File LOG_DIR = new File("log");
    private static final File HISTORY_FILE = new File("history.txt");

    // Validate URL
    public static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) return false;

        try {
            new java.net.URL(url).toURI(); // Validates both syntax and URI rules
            return url.startsWith("http://") || url.startsWith("https://");
        }
        catch (Exception e) {
            return false;
        }
    }

    // Extracts a native executable from the jar resource /bin to a temporary file
    public static File extractExecutable(String exeName) throws IOException {
        String resourcePath = "/bin/" + exeName;
        InputStream in = Utils.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        // Create a temp file with .exe suffix
        File tempFile = File.createTempFile(exeName, ".exe");
        tempFile.deleteOnExit();

        // Copy resource contents to temp file
        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        in.close();

        // Make sure it is executable (Windows normally doesn't need this, but no harm)
        tempFile.setExecutable(true);

        return tempFile;
    }

    // Get Video title from YT-DLP
    public static String getVideoTitle(File ytDlpExe, String videoUrl) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                ytDlpExe.getAbsolutePath(),
                "--get-title",
                "--no-warnings",
                "--no-playlist",
                videoUrl
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        String title = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            // Typically, the title should be the first non-empty line
            if (!line.isEmpty()) {
                title = line;
                break;
            }
        }

        int exitCode = process.waitFor();

        if (exitCode == 0 && title != null && !title.toLowerCase().startsWith("warning")) {
            return title;
        } else {
            return "Unknown";
        }
    }

    // Remove hashtags and IDs at the end of the title
    public static String sanitizeTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "Unknown";
        }

        // Remove hashtags (e.g., #tag)
        title = title.replaceAll("#\\S+", "");

        // Remove trailing .f### (e.g., .f606)
        title = title.replaceAll("\\.f\\d+$", "");

        // Replace invalid filename characters on Windows and others
        title = title.replaceAll("[\\\\/:*?\"<>|]", "_");

        // Collapse multiple spaces and trim
        title = title.replaceAll("\\s+", " ").trim();

        return title;
    }

    // Format Selected Quality
    public static String getFormatCode(String quality) {
        return switch (quality) {
            case "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]";
            case "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]";
            case "480p" -> "bestvideo[height<=480]+bestaudio/best[height<=480]";
            case "360p" -> "bestvideo[height<=360]+bestaudio/best[height<=360]";
            case "Audio only" -> "bestaudio";
            default -> "best[height<=360]";
        };
    }

    // Filter the log for relevant messages
    public static String filterMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String lower = message.toLowerCase();

        // Ignore all generic messages
        if (lower.contains("[generic]")) {
            return "";
        }

        if (lower.contains("error") ||
                lower.contains("extracting") ||
                lower.contains("merging") ||
                lower.contains("unable") ||
                lower.contains("warning") ||
                lower.contains("failed") ||
                lower.contains("complete") ||
                lower.contains("successful") ||
                lower.contains("extraction") ||
                lower.contains("destination") ||
                lower.contains("skipping") ||
                lower.contains("unsupported")) {
            return message.trim();
        }

        return "";
    }

    // Extract the error occurred during download
    public static String extractErrorMessage(String output) {
        // Find lines containing "error" or "unable"
        StringBuilder errors = new StringBuilder();
        for (String line : output.split("\\R")) {
            if (line.toLowerCase().contains("error")) {
                errors.append(line);
            }
        }
        return !errors.isEmpty() ? errors.toString().trim() : "Unknown error occurred.";
    }

    // Parse progress, Example line: [download] 42.1% of 5.00MiB at 1.23MiB/s ETA 00:10
    public static Double parseProgress(String line) {
        if (line == null || !line.startsWith("[download]")) {
            return null;
        }

        int percentIndex = line.indexOf('%');
        if (percentIndex != -1) {
            try {
                int start = line.lastIndexOf(' ', percentIndex - 1) + 1;
                String percentage = line.substring(start, percentIndex).trim();
                return Double.parseDouble(percentage) / 100.0;
            }
            catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
            }
        }
        return null;
    }

    // Write daily log files
    public static void writeLog(String content) {
        try {
            ensureLogDirExists();

            String filename = "log-" + getCurrentDate() + ".txt";
            File logFile = new File(LOG_DIR, filename);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write("[Log " + getTimestamp() + "]\n");
                writer.write(content);
                writer.write("\n");
            }
        }
        catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    // Write Result of Download to the history file
    public static void writeHistory(int exitCode, String title, String url) {
        try {
            String status = exitCode == 0 ? "Download Successful" : "Download Failed";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_FILE, true))) {
                writer.write("[" + getTimestamp() + "] " + status + ": " + title + " (" + url + ")\n");
            }
        }
        catch (IOException e) {
            System.err.println("Failed to write history: " + e.getMessage());
        }
    }

    private static void ensureLogDirExists() {
        if (!LOG_DIR.exists()) {
            LOG_DIR.mkdirs();
        }
    }

    private static String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
