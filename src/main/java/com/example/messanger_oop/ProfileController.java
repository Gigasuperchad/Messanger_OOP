package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.util.Base64;

public class ProfileController {
    @FXML private Label firstNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label nicknameLabel;
    @FXML private ImageView avatarImageView;
    @FXML private Button changePasswordButton;

    // Поля для смены пароля
    @FXML private VBox changePasswordPanel;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordStatusLabel;
    @FXML private Button savePasswordButton;
    @FXML private Button cancelPasswordButton;

    private User currentUser;
    private boolean passwordPanelVisible = false;

    @FXML
    public void initialize() {
        currentUser = AppManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            updateProfileInfo();
        }

        // Скрываем панель смены пароля
        if (changePasswordPanel != null) {
            changePasswordPanel.setVisible(false);
            changePasswordPanel.setManaged(false);
        }

        // Настраиваем обработчики кнопок
        if (changePasswordButton != null) {
            changePasswordButton.setOnAction(event -> showPasswordChangePanel());
        }

        if (savePasswordButton != null) {
            savePasswordButton.setOnAction(event -> handleSavePassword());
        }

        if (cancelPasswordButton != null) {
            cancelPasswordButton.setOnAction(event -> hidePasswordChangePanel());
        }
    }

    @FXML
    private void handleBackToChats() {
        System.out.println("Возврат к списку чатов из профиля...");
        AppManager.getInstance().switchToChatList();
    }

    private void updateProfileInfo() {
        if (firstNameLabel != null) firstNameLabel.setText(currentUser.getFirstName());
        if (lastNameLabel != null) lastNameLabel.setText(currentUser.getLastName());
        if (emailLabel != null) emailLabel.setText(currentUser.getEmail());
        if (nicknameLabel != null) nicknameLabel.setText(currentUser.getNick());

        if (currentUser.getAvatarBase64() != null && !currentUser.getAvatarBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(currentUser.getAvatarBase64());
                Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));
                avatarImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Ошибка загрузки аватара: " + e.getMessage());
            }
        }
    }

    private void showPasswordChangePanel() {
        if (changePasswordPanel != null) {
            changePasswordPanel.setVisible(true);
            changePasswordPanel.setManaged(true);
            passwordPanelVisible = true;

            // Очищаем поля
            if (currentPasswordField != null) currentPasswordField.clear();
            if (newPasswordField != null) newPasswordField.clear();
            if (confirmPasswordField != null) confirmPasswordField.clear();
            if (passwordStatusLabel != null) {
                passwordStatusLabel.setText("");
                passwordStatusLabel.setStyle("");
            }
        }
    }

    private void hidePasswordChangePanel() {
        if (changePasswordPanel != null) {
            changePasswordPanel.setVisible(false);
            changePasswordPanel.setManaged(false);
            passwordPanelVisible = false;
        }
    }

    private void handleSavePassword() {
        if (currentUser == null) {
            showPasswordError("Пользователь не авторизован!");
            return;
        }

        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Валидация
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showPasswordError("Заполните все поля!");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showPasswordError("Новые пароли не совпадают!");
            return;
        }

        if (newPassword.length() < 6) {
            showPasswordError("Пароль должен содержать минимум 6 символов!");
            return;
        }

        // Проверяем текущий пароль
        if (!currentUser.checkPassword(currentPassword)) {
            showPasswordError("Текущий пароль неверен!");
            return;
        }

        // Обновляем пароль
        currentUser.setPassword(newPassword);

        // Сохраняем пользователя с новым паролем
        try {
            UserStorage.saveUser(currentUser);
            showPasswordSuccess("Пароль успешно изменен!");

            // Очищаем поля
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();

            // Автоматически скрываем панель через 2 секунды
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::hidePasswordChangePanel);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            showPasswordError("Ошибка сохранения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showPasswordError(String message) {
        if (passwordStatusLabel != null) {
            passwordStatusLabel.setText(message);
            passwordStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void showPasswordSuccess(String message) {
        if (passwordStatusLabel != null) {
            passwordStatusLabel.setText(message);
            passwordStatusLabel.setStyle("-fx-text-fill: green;");
        }
    }

    @FXML
    private void handleStatusButton() {
        System.out.println("Открытие окна статуса из профиля...");
        AppManager.getInstance().openStatusWindow();
    }
}