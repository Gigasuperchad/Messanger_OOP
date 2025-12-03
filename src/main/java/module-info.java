module com.example.messanger_oop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.prefs;

    opens com.example.messanger_oop to javafx.fxml;
    exports com.example.messanger_oop;
}