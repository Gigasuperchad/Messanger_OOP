module com.example.messanger_oop {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.messanger_oop to javafx.fxml;
    exports com.example.messanger_oop;
}