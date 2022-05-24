module at.woolph.caco {
    requires javafx.controls;
    requires javafx.graphics;
    requires tornadofx;
    requires kotlin.stdlib;
    requires java.desktop;
    requires org.joda.time;
    requires boxable;
    requires java.prefs;
    requires java.scripting;
    requires exposed;
    requires org.apache.pdfbox;
    requires spring.shell.standard;
    requires org.jsoup;
    opens at.woolph.caco.view;
    opens at.woolph.caco.gui;
    opens at.woolph.caco.cli;
}
