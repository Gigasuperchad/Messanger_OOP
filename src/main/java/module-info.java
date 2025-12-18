module com.example.messanger_oop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.prefs;
    requires java.desktop;


    opens com.example.messanger_oop to javafx.fxml;
    exports com.example.messanger_oop;
    exports com.example.messanger_oop.client;
    opens com.example.messanger_oop.client to javafx.fxml;
    exports com.example.messanger_oop.server;
    opens com.example.messanger_oop.server to javafx.fxml;
    exports com.example.messanger_oop.shared;
    opens com.example.messanger_oop.shared to javafx.fxml;
}