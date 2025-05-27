package dev.dreiling.videodl;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

public class Animations {

    private boolean alt = false;
    private final ProgressBar progressBar;
    private final Timeline timeline;

    public Animations(ProgressBar progressBar) {
        this.progressBar = progressBar;
        this.progressBar.setProgress(1.0); // Instantly filled
        progressBar.getStyleClass().remove("progressbar");
        this.progressBar.getStyleClass().add("progressbarInit");

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> swapStyle())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void swapStyle() {
        progressBar.getStyleClass().removeAll("progressbarInit", "progressbarAlt");
        progressBar.getStyleClass().add(alt ? "progressbarInit" : "progressbarAlt");
        alt = !alt;
    }

    public void start() {
        timeline.play();
    }

    public void stopAndResetStyle() {
        timeline.stop();
        progressBar.getStyleClass().removeAll("progressbarInit", "progressbarAlt");
        if (!progressBar.getStyleClass().contains("progressbar")) {
            progressBar.getStyleClass().add("progressbar");
        }
        progressBar.setProgress(0);
    }
}