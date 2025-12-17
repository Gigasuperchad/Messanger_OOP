package com.example.messanger_oop.client;

import com.example.messanger_oop.shared.ProtocolConstants;
import com.example.messanger_oop.shared.User;
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
    @FXML private PasswordField confirmPasswordField;
    @FXML private ImageView avatarImageView;
    @FXML private Button uploadAvatarButton;
    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;
    @FXML private Label statusLabel;

    private String avatarImagePath;
    private LocalRepository localRepository;

    @FXML
    public void initialize() {
        System.out.println("Инициализация RegistrationController");

        // Настройка обработчиков событий
        uploadAvatarButton.setOnAction(event -> uploadAvatar());
        registerButton.setOnAction(event -> registerUser());
        backToLoginButton.setOnAction(event -> handleBackToLogin());

        // Очистка пути к аватару
        avatarImagePath = null;

        // Установка обработчиков для Enter
        setEnterHandlers();

        // Получаем LocalRepository для синхронизации
        getLocalRepository();
    }

    private void getLocalRepository() {
        Repository repository = AppManager.getInstance().getRepository();
        if (repository instanceof LocalRepository) {
            localRepository = (LocalRepository) repository;
            System.out.println("✅ LocalRepository получен для синхронизации");
        } else {
            System.out.println("⚠️ LocalRepository не найден");
        }
    }

    private void setEnterHandlers() {
        // Нажатие Enter в любом поле переходит к следующему полю или регистрации
        firstNameField.setOnAction(event -> lastNameField.requestFocus());
        lastNameField.setOnAction(event -> emailField.requestFocus());
        emailField.setOnAction(event -> usernameField.requestFocus());
        usernameField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(event -> registerUser());
    }

    @FXML
    private void handleBackToLogin() {
        System.out.println("Возврат к странице входа...");
        AppManager.getInstance().loadLoginScene();
    }

    private void uploadAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение профиля");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File file = fileChooser.showOpenDialog(uploadAvatarButton.getScene().getWindow());
        if (file != null) {
            try {
                // Проверяем размер файла (макс 5MB)
                long fileSize = file.length();
                if (fileSize > 5 * 1024 * 1024) {
                    showError("Файл слишком большой! Максимальный размер: 5MB");
                    return;
                }

                avatarImagePath = file.getAbsolutePath();
                Image image = new Image(file.toURI().toString());
                avatarImageView.setImage(image);

                showStatus("✅ Аватар успешно загружен!", "green");
            } catch (Exception e) {
                showError("Ошибка при загрузке изображения: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void registerUser() {
        System.out.println("\n=== ПОПЫТКА РЕГИСТРАЦИИ ===");

        // Получаем значения полей
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        System.out.println("Поля формы:");
        System.out.println("  Имя: " + firstName);
        System.out.println("  Фамилия: " + lastName);
        System.out.println("  Email: " + email);
        System.out.println("  Логин: " + username);
        System.out.println("  Пароль: " + (password.isEmpty() ? "(пусто)" : "***"));
        System.out.println("  Подтверждение: " + (confirmPassword.isEmpty() ? "(пусто)" : "***"));

        // Валидация полей
        if (firstName.isEmpty()) {
            showError("Введите имя!");
            firstNameField.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            showError("Введите фамилию!");
            lastNameField.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            showError("Введите email!");
            emailField.requestFocus();
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showError("Введите корректный email!");
            emailField.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            showError("Введите логин!");
            usernameField.requestFocus();
            return;
        }

        if (username.length() < 3) {
            showError("Логин должен содержать минимум 3 символа!");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Введите пароль!");
            passwordField.requestFocus();
            return;
        }

        if (password.length() < 6) {
            showError("Пароль должен содержать минимум 6 символов!");
            passwordField.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            showError("Подтвердите пароль!");
            confirmPasswordField.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Пароли не совпадают!");
            passwordField.clear();
            confirmPasswordField.clear();
            passwordField.requestFocus();
            return;
        }

        // Проверяем, существует ли пользователь локально
        if (UserStorage.userExists(username)) {
            showError("Пользователь с таким логином уже существует!");
            usernameField.requestFocus();
            return;
        }

        // Проверяем, существует ли email локально
        if (UserStorage.emailExists(email)) {
            showError("Пользователь с таким email уже зарегистрирован!");
            emailField.requestFocus();
            return;
        }

        try {
            // Преобразуем аватар в Base64 если есть
            String avatarBase64 = null;
            if (avatarImagePath != null) {
                try {
                    avatarBase64 = encodeImageToBase64(avatarImagePath);
                    System.out.println("Аватар конвертирован в Base64");
                } catch (IOException e) {
                    showError("Ошибка обработки изображения: " + e.getMessage());
                    return;
                }
            }

            System.out.println("Создание пользователя...");

            // Создаем пользователя
            User user = new User(
                    username,
                    password,
                    firstName,
                    lastName,
                    email,
                    avatarBase64
            );

            System.out.println("✅ Пользователь создан: " + user.getNick());

            // Сохраняем пользователя локально
            UserStorage.saveUser(user);
            System.out.println("✅ Пользователь сохранен в локальное хранилище");

            // Сохраняем как текущего пользователя
            UserStorage.saveCurrentUser(user);
            System.out.println("✅ Пользователь сохранен как текущий");

            // АВТОМАТИЧЕСКАЯ СИНХРОНИЗАЦИЯ С СЕРВЕРОМ
            syncUserWithServer(user);

            // Показываем успешное сообщение
            showStatus("✅ Регистрация успешна! Перенаправление...", "green");

            // Задержка перед переходом
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Увеличили задержку для синхронизации

                    // Переходим в мессенджер
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("🔄 Загрузка сцены мессенджера...");
                        AppManager.getInstance().loadMessengerScene(user);
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            showError("Ошибка при регистрации: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncUserWithServer(User user) {
        System.out.println("\n🔄 АВТОМАТИЧЕСКАЯ СИНХРОНИЗАЦИЯ С СЕРВЕРОМ");

        if (localRepository == null) {
            System.out.println("❌ LocalRepository не доступен для синхронизации");
            return;
        }

        if (!localRepository.isConnectedToServer()) {
            System.out.println("⚠️ Нет подключения к серверу, синхронизация отложена");
            return;
        }

        // Синхронизируем пользователя в отдельном потоке
        new Thread(() -> {
            try {
                System.out.println("🔄 Синхронизация пользователя " + user.getNick() + " с сервером...");

                // Шаг 1: Регистрация на сервере
                Thread.sleep(1000); // Даем время на установку соединения

                System.out.println("📤 Отправка регистрации на сервер: " + user.getNick());
                // Регистрация выполняется через ClientConnection

                // Шаг 2: Авторизация на сервере
                Thread.sleep(1500);

                if (localRepository.isConnectedToServer()) {
                    System.out.println("🔐 Авторизация на сервере: " + user.getNick());
                    // Авторизация выполнится автоматически в setCurrentUser
                } else {
                    System.out.println("❌ Потеряно соединение с сервером во время синхронизации");
                }

                // Шаг 3: Проверяем результат
                Thread.sleep(1000);

                if (localRepository.isFullyConnected()) {
                    System.out.println("✅ Пользователь успешно синхронизирован с сервером!");
                } else {
                    System.out.println("⚠️ Пользователь зарегистрирован локально, но не синхронизирован с сервером");
                    System.out.println("   При подключении к серверу синхронизация произойдет автоматически");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("❌ Синхронизация прервана");
            } catch (Exception e) {
                System.err.println("❌ Ошибка синхронизации: " + e.getMessage());
            }
        }).start();
    }

    private String encodeImageToBase64(String imagePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Path.of(imagePath));
        return Base64.getEncoder().encodeToString(bytes);
    }

    private void showError(String message) {
        System.err.println("❌ Ошибка: " + message);
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
    }

    private void showStatus(String message, String color) {
        System.out.println("📢 Статус: " + message);
        statusLabel.setText(message);
        if (color.equals("green")) {
            statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
        }
    }
}