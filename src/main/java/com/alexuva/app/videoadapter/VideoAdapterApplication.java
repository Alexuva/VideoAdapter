package com.alexuva.app.videoadapter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class VideoAdapterApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(VideoAdapterApplication.class.getResource("home.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 600);
        VideoAdapterController controller = fxmlLoader.getController();
        stage.setOnCloseRequest(event -> controller.cancel());
        stage.setTitle("Video Adapter");
        stage.setScene(scene);
        stage.show();
    }
}
