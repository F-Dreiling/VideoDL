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

    @FXML
    private TextField urlField;
    @FXML
    private Button downloadButton;
    @FXML
    private Label progressLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private ComboBox<String> qualitySelector;
    @FXML
    private Label directoryLabel;
    @FXML
    private Button directoryButton;

    @FXML
    public void handleDownload(ActionEvent event) {

        String url = urlField.getText();
        if (url == null || url.isEmpty()) {
            progressLabel.setText("Please enter a valid Video URL.");
            return;
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
        directoryButton.setDisable(true);
        downloadButton.setDisable(true);
        qualitySelector.setDisable(true);
        progressLabel.setText("Downloading...");
        progressBar.setProgress(0);

        // Start Download
        new Thread(() -> {
            try {
                DownloadService.downloadVideo(outputDirectory, url, quality,
                        progress -> Platform.runLater(() -> progressBar.setProgress(progress)),
                        status -> Platform.runLater(() -> progressLabel.setText(status))
                );
                Platform.runLater(() -> {
                    progressLabel.setText("Download completed!");
                    directoryButton.setDisable(false);
                    downloadButton.setDisable(false);
                    qualitySelector.setDisable(false);
                });
            }
            catch (Exception e) {
                Platform.runLater(() -> {
                    progressLabel.setText(e.getMessage());
                    directoryButton.setDisable(false);
                    downloadButton.setDisable(false);
                    qualitySelector.setDisable(false);
                });
            }
        }).start();
    }

    public void handleOutput(ActionEvent event) {
        // Get the current stage from UI control
        Stage stage = (Stage) directoryButton.getScene().getWindow();

        // Get folder from Chooser dialog and validate
        File selectedDir = directoryChooser.showDialog(stage);
        if (selectedDir != null && selectedDir.isDirectory()) {
            outputDirectory = selectedDir.getAbsolutePath();
            directoryLabel.setText(outputDirectory);
        }
        else {
            directoryLabel.setText("Select valid directory");
        }
    }

    public void initialize() {
        // Initialize DirectoryChooser with default folder (Windows Downloads)
        String userHome = System.getProperty("user.home");
        File downloadsFolder = new File(userHome, "Downloads");
        directoryChooser = new DirectoryChooser();

        // If valid, set to default downloads folder
        if (downloadsFolder.exists() && downloadsFolder.isDirectory()) {
            directoryChooser.setInitialDirectory(downloadsFolder);
            outputDirectory = downloadsFolder.getAbsolutePath();
            directoryLabel.setText(outputDirectory);
        }
        // If not valid, set to {root}/downloads folder and create if necessary
        else {
            downloadsFolder = new File(System.getProperty("user.dir"), "downloads");
            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdirs();
            }

            directoryChooser.setInitialDirectory(downloadsFolder);
            outputDirectory = downloadsFolder.getAbsolutePath();
            directoryLabel.setText(outputDirectory);
        }

        // Make sure we have valid output folder
        if (outputDirectory == null || outputDirectory.isEmpty()) {
            directoryLabel.setText("No output directory selected");
            return;
        }

        // Populate quality options
        qualitySelector.getItems().addAll("1080p", "720p", "480p", "360p", "Audio only");
    }
}