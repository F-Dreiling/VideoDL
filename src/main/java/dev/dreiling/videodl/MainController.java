package dev.dreiling.videodl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;

public class MainController {

    private boolean animationOn = true;
    private boolean isDownloading = false;
    private DirectoryChooser directoryChooser;
    private File outputDirectory;
    private Animations progressAnimation;

    @FXML
    private TextField urlField;
    @FXML
    private ComboBox<String> qualitySelector;
    @FXML
    private Button downloadButton;
    @FXML
    private Button directoryButton;
    @FXML
    private Label directoryLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private ProgressBar progressBar;

    @FXML
    public void handleOutput(ActionEvent event) {
        // Get the current stage from UI control
        Stage stage = (Stage) directoryButton.getScene().getWindow();

        // Get folder from Chooser dialog and validate
        File selectedFolder = directoryChooser.showDialog(stage);
        if (selectedFolder != null && selectedFolder.isDirectory()) {
            outputDirectory = selectedFolder;
        }

        // Complain if no new folder was selected and the previous one wasn't valid
        if (outputDirectory == null) {
            directoryLabel.setText("Output: Select valid folder");
        }
        else {
            directoryLabel.setText("Output: " + outputDirectory.getAbsolutePath());
        }
    }

    @FXML
    public void handleDownload(ActionEvent event) {

        // If downloading, this is a cancel request
        if (isDownloading) {
            DownloadService.cancel();
            progressLabel.setText("Cancelling...");
            downloadButton.setDisable(true); // prevent double click while cancelling
            return;
        }

        // Validate URL
        String url = urlField.getText();
        if (!isValidUrl(url)) {
            progressLabel.setText("Please enter a valid Video URL");
            return;
        }

        // Output validation
        if (!isValidDirectory(outputDirectory)) {
            progressLabel.setText("Please select a valid output folder");
            return;
        }

        // Get selected quality
        String quality = setQuality(qualitySelector.getValue());
        Platform.runLater(() -> qualitySelector.setValue(quality));

        // Stop progress bar animation
        if (animationOn) {
            progressAnimation.stopAndResetStyle();
            animationOn = false;
        }

        // Prepare Download
        isDownloading = true;
        qualitySelector.setDisable(true);
        directoryButton.setDisable(true);
        downloadButton.setText("Cancel Download");
        progressLabel.setText("Downloading...");

        // Start Download
        new Thread(() -> {
            try {
                boolean finished = DownloadService.downloadVideo(outputDirectory.getAbsolutePath(), url, quality,
                        progress -> Platform.runLater(() -> progressBar.setProgress(progress)),
                        status -> Platform.runLater(() -> progressLabel.setText(status))
                );
                if (finished) Platform.runLater(() -> onDownloadFinished("Download completed"));
                else Platform.runLater(() -> onDownloadFinished("Download cancelled by user"));
            }
            catch (Exception e) {
                Platform.runLater(() -> onDownloadFinished(e.getMessage()));
            }
        }).start();
    }

    private void onDownloadFinished(String message) {
        // Reset GUI after download, cancellation or error
        isDownloading = false;
        qualitySelector.setDisable(false);
        directoryButton.setDisable(false);
        downloadButton.setDisable(false);
        downloadButton.setText("Download Video");
        progressLabel.setText(message);
        animationOn = true;
        progressAnimation = new Animations(progressBar);
        progressAnimation.start();
    }

    public void initialize() {
        // Initialize DirectoryChooser with the system default folder (Windows Downloads)
        String userHome = System.getProperty("user.home");
        File downloadsFolder = new File(userHome, "Downloads");
        directoryChooser = new DirectoryChooser();

        // If not valid, set to {app}/downloads folder and create if necessary
        if (!isValidDirectory(downloadsFolder)) {
            downloadsFolder = new File(System.getProperty("user.dir"), "downloads");
            if (!isValidDirectory(downloadsFolder)) {
                downloadsFolder.mkdirs();
            }
        }

        // Make sure there's a valid output folder
        if (isValidDirectory(downloadsFolder)) {
            directoryChooser.setInitialDirectory(downloadsFolder);
            outputDirectory = downloadsFolder;
            directoryLabel.setText("Output: " + outputDirectory);
        }
        else {
            directoryLabel.setText("Output: No valid folder selected");
        }

        // Populate quality options
        qualitySelector.getItems().addAll("1080p", "720p", "480p", "360p", "Audio only");

        // Start idle animation
        progressAnimation = new Animations(progressBar);
        progressAnimation.start();
    }

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

    // Validate Folder
    public static boolean isValidDirectory(File directory) {
        return directory != null && directory.exists() && directory.isDirectory();
    }

    // Set Quality
    public String setQuality(String selected) {
        if (selected == null) {
            return "360p";
        }
        return selected;
    }
}