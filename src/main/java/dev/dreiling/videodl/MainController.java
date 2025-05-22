package dev.dreiling.videodl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.*;

public class MainController {

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
    public void handleDownload(ActionEvent event) {
        String url = urlField.getText();
        if (url == null || url.isEmpty()) {
            progressLabel.setText("Please enter a YouTube URL.");
            return;
        }

        String selected = qualitySelector.getValue();
        if (selected == null) {
            selected = "360p";
        }
        String selectedQuality = selected;

        downloadButton.setDisable(true);
        progressLabel.setText("Downloading...");
        progressBar.setProgress(0);

        new Thread(() -> {
            try {
                DownloadService.downloadVideo(url, selectedQuality,
                        progress -> Platform.runLater(() -> progressBar.setProgress(progress)),
                        status -> Platform.runLater(() -> progressLabel.setText(status))
                );
                Platform.runLater(() -> {
                    progressLabel.setText("Download completed!");
                    downloadButton.setDisable(false);
                });
            }
            catch (Exception e) {
                Platform.runLater(() -> {
                    progressLabel.setText("Error: " + e.getMessage());
                    downloadButton.setDisable(false);
                });
            }
        }).start();
    }

    // Initialize GUI
    public void initialize() {
        qualitySelector.getItems().addAll("1080p", "720p", "480p", "360p", "Audio only");
    }
}