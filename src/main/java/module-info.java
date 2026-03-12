module com.alexuva.app.videoadapter {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires static lombok;
    requires tools.jackson.databind;
    requires java.sql;

    opens com.alexuva.app.videoadapter.ffmpeg to tools.jackson.databind;

    opens com.alexuva.app.videoadapter to javafx.fxml;
    exports com.alexuva.app.videoadapter;
}