package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.Base64;

public class ProfileController {
    @FXML private Label firstNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label nicknameLabel;
    @FXML private ImageView avatarImageView;

    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = AppManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            updateProfileInfo();
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
}