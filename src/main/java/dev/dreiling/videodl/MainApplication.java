package dev.dreiling.videodl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApplication extends Application {

	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
			Parent root = loader.load();
			primaryStage.setTitle("VideoDL");

			Image image = new Image(this.getClass().getResourceAsStream("/icon.png"));
			primaryStage.getIcons().add(image);

			primaryStage.setScene(new Scene(root));
			primaryStage.show();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
