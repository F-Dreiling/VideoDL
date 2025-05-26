package dev.dreiling.videodl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;

public class MainController {

    private DirectoryChooser directoryChooser;
    private String outputDirectory;
    private boolean isStyleChanged = false;

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
    public void handleDownload(ActionEvent event) {

        // Validate URL
        String url = urlField.getText();
        if (!Utils.isValidUrl(url)) {
            progressLabel.setText("Please enter a valid Video URL");
            return;
        }

        // Change progressbar style upon starting a download
        if (!isStyleChanged) {
            progressBar.getStyleClass().remove("progressbarInit");
            progressBar.getStyleClass().add("progressbar");
            progressBar.setProgress(0);
            isStyleChanged = true;
        }

        // Get selected quality, 360p as fallback
        String selected = qualitySelector.getValue();
        if (selected == null) {
            selected = "360p";
            String finalSelected = selected;
            Platform.runLater(() -> qualitySelector.setValue(finalSelected));
        }
        String quality = selected;

        // Prepare Download
        qualitySelector.setDisable(true);
        downloadButton.setDisable(true);
        directoryButton.setDisable(true);
        progressLabel.setText("Downloading...");

        // Start Download
        new Thread(() -> {
            try {
                DownloadService.downloadVideo(outputDirectory, url, quality,
                        progress -> Platform.runLater(() -> progressBar.setProgress(progress)),
                        status -> Platform.runLater(() -> progressLabel.setText(status))
                );
                Platform.runLater(() -> {
                    progressLabel.setText("Download completed!");
                    qualitySelector.setDisable(false);
                    downloadButton.setDisable(false);
                    directoryButton.setDisable(false);
                });
            }
            catch (Exception e) {
                Platform.runLater(() -> {
                    progressLabel.setText(e.getMessage());
                    qualitySelector.setDisable(false);
                    downloadButton.setDisable(false);
                    directoryButton.setDisable(false);
                });
            }
        }).start();
    }

    public void handleOutput(ActionEvent event) {
        // Get the current stage from UI control
        Stage stage = (Stage) directoryButton.getScene().getWindow();

        // Get folder from Chooser dialog and validate
        File selectedFolder = directoryChooser.showDialog(stage);
        if (selectedFolder != null && selectedFolder.isDirectory()) {
            outputDirectory = selectedFolder.getAbsolutePath();
            directoryLabel.setText("Output: " + outputDirectory);
        }
        else {
            directoryLabel.setText("Output: Select valid folder");
        }
    }

    public void initialize() {
        // Initialize DirectoryChooser with the default folder (Windows Downloads)
        String userHome = System.getProperty("user.home");
        File downloadsFolder = new File(userHome, "Downloads");
        directoryChooser = new DirectoryChooser();

        // If valid, set to default downloads folder
        if (downloadsFolder.exists() && downloadsFolder.isDirectory()) {
            directoryChooser.setInitialDirectory(downloadsFolder);
            outputDirectory = downloadsFolder.getAbsolutePath();
            directoryLabel.setText("Output: " + outputDirectory);
        }
        // If not valid, set to {root}/downloads folder and create if necessary
        else {
            downloadsFolder = new File(System.getProperty("user.dir"), "downloads");
            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdirs();
            }

            directoryChooser.setInitialDirectory(downloadsFolder);
            outputDirectory = downloadsFolder.getAbsolutePath();
            directoryLabel.setText("Output: " + outputDirectory);
        }

        // Make sure there's a valid output folder
        if (outputDirectory == null || outputDirectory.isEmpty()) {
            directoryLabel.setText("Output: No valid folder selected");
            return;
        }

        // Populate quality options
        qualitySelector.getItems().addAll("1080p", "720p", "480p", "360p", "Audio only");
    }
}