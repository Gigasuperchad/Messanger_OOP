package com.example.messanger_oop;

import javafx.application.Application;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        AppManager manager = AppManager.getInstance();
        manager.setStage(stage);
    }

    public static void main(String[] args) {
        launch();
    }
}