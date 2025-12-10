package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class RegistrationController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ImageView avatarImageView;
    @FXML private Button uploadAvatarButton;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;

    private String avatarImagePath;


    @FXML
    public void initialize() throws IOException {
        uploadAvatarButton.setOnAction(event -> uploadAvatar());
        registerButton.setOnAction(event -> registerUser());
        avatarImagePath = null;
    }

    private void uploadAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение профиля");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(uploadAvatarButton.getScene().getWindow());
        if (file != null) {
            try {
                avatarImagePath = file.getAbsolutePath();
                Image image = new Image(file.toURI().toString());
                avatarImageView.setImage(image);
            } catch (Exception e) {
                showError("Ошибка при загрузке изображения: " + e.getMessage());
            }
        }
    }

    private void registerUser() {
        if (firstNameField.getText().isEmpty() ||
                lastNameField.getText().isEmpty() ||
                emailField.getText().isEmpty() ||
                usernameField.getText().isEmpty() ||
                passwordField.getText().isEmpty()) {

            showError("Все поля обязательны для заполнения!");
            return;
        }

        if (!emailField.getText().contains("@")) {
            showError("Введите корректный email!");
            return;
        }

        if (passwordField.getText().length() < 6) {
            showError("Пароль должен содержать минимум 6 символов!");
            return;
        }

        if (UserStorage.userExists(usernameField.getText())) {
            showError("Пользователь с таким логином уже существует!");
            return;
        }

        try {
            String avatarBase64 = null;
            if (avatarImagePath != null) {
                avatarBase64 = encodeImageToBase64(avatarImagePath);
            }

            User user = new User(
                    usernameField.getText(),
                    passwordField.getText(),
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    avatarBase64
            );

            UserStorage.saveUser(user);

            statusLabel.setText("Регистрация успешна! Профиль сохранен.");
            statusLabel.setStyle("-fx-text-fill: green;");

            AppManager.getInstance().loadMessengerScene(user);

        } catch (Exception e) {
            showError("Ошибка при регистрации: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String encodeImageToBase64(String imagePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Path.of(imagePath));
        return Base64.getEncoder().encodeToString(bytes);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }
}