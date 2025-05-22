package dev.dreiling.videodl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MainController {

    @FXML
    private TextField urlField;

    @FXML
    private Button downloadButton;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<String> qualitySelector;

    @FXML
    public void handleDownload(ActionEvent event) {
        String url = urlField.getText();
        if (url == null || url.isEmpty()) {
            statusLabel.setText("Please enter a YouTube URL.");
            return;
        }

        String selected = qualitySelector.getValue();
        if (selected == null) {
            selected = "360p"; // default fallback
        }
        String selectedQuality = selected;

        downloadButton.setDisable(true);
        statusLabel.setText("Downloading...");

        new Thread(() -> {
            try {
                DownloadService.downloadVideo(url, selectedQuality);
                Platform.runLater(() -> {
                    statusLabel.setText("Download completed!");
                    downloadButton.setDisable(false);
                });
            }
            catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    downloadButton.setDisable(false);
                });
            }
        }).start();
    }
}