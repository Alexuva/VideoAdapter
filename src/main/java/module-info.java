module com.alexuva.app.videoadapter {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires static lombok;
    requires tools.jackson.databind;
    requires java.sql;
    requires atlantafx.base;

    opens com.alexuva.app.videoadapter.ffmpeg to tools.jackson.databind;

    opens com.alexuva.app.videoadapter to javafx.fxml;
    exports com.alexuva.app.videoadapter;
}