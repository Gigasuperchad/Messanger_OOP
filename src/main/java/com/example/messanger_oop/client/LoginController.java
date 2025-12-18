package com.example.messanger_oop.client;

import com.example.messanger_oop.shared.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Base64;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label firstNameLabel;

    @FXML
    private Label lastNameLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private ImageView avatarImageView;

    private User currentUser;

    @FXML
    private VBox profileInfoPanel;

    @FXML
    public void initialize() {
        System.out.println("Инициализация LoginController");
        hideProfileInfo();

//        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
//            System.out.println("Поле логина: " + newVal);
//        });
//
//        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
//            System.out.println("Поле пароля: " + (newVal != null ? "*".repeat(newVal.length()) : "null"));
//        });

        loginButton.setOnAction(event -> handleLogin());
        registerButton.setOnAction(event -> handleRegister());


        usernameField.setOnAction(event -> handleLogin());
        passwordField.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        System.out.println("\nНачало обработки входа...");

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        System.out.println("Введенные данные:");
        System.out.println("   Логин: '" + username + "'");
        System.out.println("   Пароль: '" + password + "'");

        if (username.isEmpty() || password.isEmpty()) {
            showError("Введите логин и пароль");
            System.out.println("Пустые поля");
            return;
        }

        statusLabel.setText("Проверка пользователя...");
        statusLabel.setStyle("-fx-text-fill: blue;");

        System.out.println("Проверка существования пользователя...");

        if (!UserStorage.userExists(username)) {
            showError("Пользователь не найден");
            System.out.println("Пользователь не найден в UserStorage: " + username);

            System.out.println("Все пользователи в хранилище:");
            for (User user : UserStorage.getAllUsers()) {
                System.out.println("   👤 " + user.getNick() + " - " + user.getFullName());
            }
            return;
        }

        System.out.println("Пользователь найден в хранилище");

        System.out.println("Загрузка пользователя из хранилища...");
        User user = UserStorage.loadUser(username);
        if (user == null) {
            showError("Ошибка загрузки пользователя");
            System.out.println("Ошибка загрузки пользователя");
            return;
        }

        System.out.println("Пользователь загружен: " + user.getNick());
        System.out.println("Проверка пароля...");

        if (!user.checkPassword(password)) {
            showError("Неверный пароль");
            System.out.println("Неверный пароль");
            System.out.println("   Ожидаемый пароль: '" + user.getPassword() + "'");
            System.out.println("   Введенный пароль: '" + password + "'");
            return;
        }

        System.out.println("Пароль верный!");

        currentUser = user;
        showSuccess("Вход выполнен успешно!");
        System.out.println("Аутентификация успешна для: " + user.getFullName());

        showProfileInfo(user);

        loginButton.setText("Войти в мессенджер");
        loginButton.setOnAction(event -> enterMessenger());

        System.out.println("LoginController готов к переходу в мессенджер");
    }

    @FXML
    private void handleRegister() {
        System.out.println("Переход к регистрации...");
        AppManager.getInstance().loadRegistrationScene();
    }

    private void enterMessenger() {
        System.out.println("\nПереход в мессенджер...");

        if (currentUser != null) {
            System.out.println("Сохранение пользователя как текущего...");

            UserStorage.saveUser(currentUser);

            System.out.println("Текущий пользователь сохранен: " + currentUser.getNick());

            System.out.println("Загрузка сцены мессенджера...");
            AppManager.getInstance().loadMessengerScene(currentUser);

            System.out.println("Переход выполнен");
        } else {
            showError("Сначала выполните вход");
            System.out.println("Нет текущего пользователя");
        }
    }

    private void showProfileInfo(User user) {
        System.out.println("👤 Отображение информации о профиле...");

        if (firstNameLabel == null) {
            System.err.println("firstNameLabel is null!");
            return;
        }
        if (lastNameLabel == null) {
            System.err.println("lastNameLabel is null!");
            return;
        }
        if (emailLabel == null) {
            System.err.println("emailLabel is null!");
            return;
        }

        if (profileInfoPanel != null) {
            profileInfoPanel.setVisible(true);
        }

        firstNameLabel.setText(user.getFirstName());
        lastNameLabel.setText(user.getLastName());
        emailLabel.setText(user.getEmail());

        System.out.println("   Имя: " + user.getFirstName());
        System.out.println("   Фамилия: " + user.getLastName());
        System.out.println("   Email: " + user.getEmail());

        if (user.getAvatarBase64() != null && !user.getAvatarBase64().isEmpty()) {
            try {
                System.out.println("Загрузка аватара из Base64...");
                byte[] imageBytes = Base64.getDecoder().decode(user.getAvatarBase64());
                Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));
                avatarImageView.setImage(image);
                System.out.println("Аватар загружен");
            } catch (Exception e) {
                System.err.println("Ошибка загрузки аватара: " + e.getMessage());
                setDefaultAvatar();
            }
        } else {
            System.out.println("Аватар не указан, установка по умолчанию");
            setDefaultAvatar();
        }

        firstNameLabel.setVisible(true);
        lastNameLabel.setVisible(true);
        emailLabel.setVisible(true);
        avatarImageView.setVisible(true);

        System.out.println("Информация о профиле отображена");
    }

    private void hideProfileInfo() {
        System.out.println("Скрытие информации о профиле...");

        if (firstNameLabel != null) firstNameLabel.setVisible(false);
        if (lastNameLabel != null) lastNameLabel.setVisible(false);
        if (emailLabel != null) emailLabel.setVisible(false);
        if (avatarImageView != null) avatarImageView.setVisible(false);
        if (profileInfoPanel != null) {
            profileInfoPanel.setVisible(false);
        }
    }

    private void setDefaultAvatar() {
        try {
            File defaultAvatar = new File("default_avatar.png");
            if (defaultAvatar.exists()) {
                Image image = new Image(defaultAvatar.toURI().toString());
                avatarImageView.setImage(image);
                System.out.println("Установлен аватар по умолчанию из файла");
            } else {
                avatarImageView.setImage(null);
                System.out.println("Файл аватара по умолчанию не найден");
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки аватара по умолчанию: " + e.getMessage());
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        System.err.println("Ошибка: " + message);
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        System.out.println("Успех: " + message);
    }
}