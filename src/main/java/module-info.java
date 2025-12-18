module com.example.calcnotepad {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    requires javafx.swing;
    requires tess4j;
    requires java.desktop;
    requires MathParser.org.mXparser;

    opens com.example.calcnotepad to javafx.fxml;
    exports com.example.calcnotepad;
}