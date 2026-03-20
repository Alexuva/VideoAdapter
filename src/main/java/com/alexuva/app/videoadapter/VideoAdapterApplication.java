package com.alexuva.app.videoadapter;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class VideoAdapterApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        FXMLLoader fxmlLoader = new FXMLLoader(VideoAdapterApplication.class.getResource("home.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 700);
        VideoAdapterController controller = fxmlLoader.getController();
        stage.setOnCloseRequest(event -> controller.cancel());
        stage.setTitle("Video Adapter v0.0.8");
        stage.setScene(scene);
        stage.show();
    }
}
