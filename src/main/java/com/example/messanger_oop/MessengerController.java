package com.example.messanger_oop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class MessengerController {
    @FXML private TextField usernameField;
    @FXML private TextArea messagesArea;
    @FXML private TextField messageField;
    @FXML private ListView<String> usersList;
    @FXML private Button connectButton;
    @FXML private Button sendButton;
    @FXML private Button disconnectButton;
    @FXML private Label userNameLabel;
    @FXML private ImageView userAvatarView;
    // @FXML private PasswordField passwordField;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    @FXML
    public void initialize() {
        sendButton.setDisable(true);
        disconnectButton.setDisable(true);
        messageField.setDisable(true);

        loadCurrentUser();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateUserInfo();
    }

    private void loadCurrentUser() {
        currentUser = UserStorage.getCurrentUser();
        if (currentUser != null) {
            updateUserInfo();
        }
    }

    private void updateUserInfo() {
        if (currentUser != null && userNameLabel != null) {
            userNameLabel.setText(currentUser.getFullName());

            if (currentUser.getAvatarBase64() != null && !currentUser.getAvatarBase64().isEmpty()) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(currentUser.getAvatarBase64());
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    userAvatarView.setImage(image);
                } catch (Exception e) {
                    System.err.println("Ошибка загрузки аватара: " + e.getMessage());
                }
            }

            if (usernameField != null) {
                usernameField.setText(currentUser.getNick());
            }
        }
    }

    @FXML
    public void connectToServer() {
        try {
            String username = currentUser != null ? currentUser.getNick() : usernameField.getText().trim();

            String password = null;
            if (currentUser != null && currentUser.getPassword() != null) {
                password = currentUser.getPassword();
            } else {
                // Если вы хотите вводить пароль вручную через UI — добавьте PasswordField и используйте:
                // password = passwordField.getText().trim();
                // На текущий момент попробуем взять из usernameField (как запасной вариант) — лучше добавить PasswordField.
                password = ""; // явное пустое, чтобы не отправлять строку-заглушку
            }

            if (username.isEmpty()) {
                showAlert("Ошибка", "Введите имя пользователя");
                return;
            }

            socket = new Socket("10.209.209.41", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                appendMessage("Сервер", serverResponse);

                if (serverResponse.toLowerCase().contains("введите номер")) {
                    out.println("2");
                    continue;
                }

                if (serverResponse.toLowerCase().contains("введите логин")) {
                    out.println(username);
                    continue;
                }

                if (serverResponse.toLowerCase().contains("введите пароль")) {
                    out.println(password != null ? password : "");
                    continue;
                }

                if (serverResponse.toLowerCase().contains("вход выполнен") ||
                        serverResponse.toLowerCase().contains("добро пожаловать")) {
                    break;
                }

                if (serverResponse.toLowerCase().contains("неверный логин")
                        || serverResponse.toLowerCase().contains("неверный пароль")
                        || serverResponse.toLowerCase().contains("аутентификация не удалась")) {
                    showAlert("Ошибка", "Неверный логин или пароль");
                    disconnectFromServer();
                    return;
                }
            }

            Thread messageReader = new Thread(this::readMessages);
            messageReader.setDaemon(true);
            messageReader.start();

            connectButton.setDisable(true);
            if (usernameField != null) usernameField.setDisable(true);
            sendButton.setDisable(false);
            disconnectButton.setDisable(false);
            messageField.setDisable(false);

            appendMessage("Система", "Успешное подключение к серверу");

        } catch (IOException e) {
            showAlert("Ошибка подключения", "Не удалось подключиться к серверу: " + e.getMessage());
            cleanupSocket();
        }
    }

    @FXML
    public void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && out != null) {
            if (message.startsWith("/")) {
                out.println(message);
            } else {
                out.println(message);
                appendMessage("Вы", message);
            }
            messageField.clear();
        }
    }

    @FXML
    public void disconnectFromServer() {
        try {
            if (out != null) {
                out.println("/quit");
            }
            cleanupSocket();

            connectButton.setDisable(false);
            if (usernameField != null) {
                usernameField.setDisable(false);
                usernameField.clear();
            }
            sendButton.setDisable(true);
            disconnectButton.setDisable(true);
            messageField.setDisable(true);

            appendMessage("Система", "Отключено от сервера");
        } catch (Exception e) {
            System.err.println("Ошибка при отключении: " + e.getMessage());
            cleanupSocket();
        }
    }

    private void readMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String msg = message;
                Platform.runLater(() -> {
                    if (msg.startsWith("Подключенные пользователи:")) {
                        updateUsersList(msg);
                    } else {
                        appendMessage("", msg);
                    }
                });
            }
        } catch (IOException e) {
            Platform.runLater(() -> {
                if (socket != null && !socket.isClosed()) {
                    appendMessage("Система", "Соединение с сервером потеряно");
                    disconnectFromServer();
                }
            });
        }
    }

    private void appendMessage(String sender, String message) {
        if (!sender.isEmpty()) {
            messagesArea.appendText(sender + ": " + message + "\n");
        } else {
            messagesArea.appendText(message + "\n");
        }
    }

    private void updateUsersList(String usersMessage) {
        usersList.getItems().clear();
        String usersString = usersMessage.replace("Подключенные пользователи: ", "").replace("[", "").replace("]", "");
        String[] users = usersString.split(", ");
        for (String user : users) {
            if (!user.isEmpty()) {
                usersList.getItems().add(user);
            }
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void cleanupSocket() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        if (out != null) out.close();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        in = null;
        out = null;
        socket = null;
    }
}
