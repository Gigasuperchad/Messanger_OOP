package com.example.messanger_oop.client;

import com.example.messanger_oop.shared.ProtocolConstants;
import com.example.messanger_oop.shared.User;
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
    @FXML private PasswordField passwordField;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;
    private boolean connected = false;

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
            String password = currentUser != null ? currentUser.getPassword() : passwordField.getText().trim();

            if (username.isEmpty()) {
                showAlert("Ошибка", "Введите имя пользователя");
                return;
            }

            socket = new Socket("127.0.0.1", 12345);
            socket.setSoTimeout(10000);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // Пытаемся войти
            String loginCommand = ProtocolConstants.CMD_LOGIN + ProtocolConstants.DELIMITER +
                    username + ProtocolConstants.DELIMITER + password;
            out.println(loginCommand);

            // Читаем ответ сервера
            String response = in.readLine();
            if (response != null && response.startsWith(ProtocolConstants.RESP_AUTH_SUCCESS)) {
                connected = true;

                // Запускаем поток чтения сообщений
                Thread messageReader = new Thread(this::readMessages);
                messageReader.setDaemon(true);
                messageReader.start();

                connectButton.setDisable(true);
                usernameField.setDisable(true);
                passwordField.setDisable(true);
                sendButton.setDisable(false);
                disconnectButton.setDisable(false);
                messageField.setDisable(false);

                appendMessage("Система", "Успешное подключение к серверу");

                // Запрашиваем список чатов
                out.println(ProtocolConstants.CMD_GET_CHATS);

            } else {
                showAlert("Ошибка", "Неверный логин или пароль");
                disconnectFromServer();
            }

        } catch (IOException e) {
            showAlert("Ошибка подключения", "Не удалось подключиться к серверу: " + e.getMessage());
            cleanupSocket();
        }
    }

    @FXML
    public void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && out != null && connected) {
            // Для простоты отправляем в общий чат
            String command = ProtocolConstants.CMD_SEND_MESSAGE + ProtocolConstants.DELIMITER +
                    "0" + ProtocolConstants.DELIMITER + message;
            out.println(command);
            appendMessage("Вы", message);
            messageField.clear();
        }
    }

    @FXML
    public void disconnectFromServer() {
        try {
            if (out != null && connected) {
                out.println("ОТКЛЮЧЕНИЕ ЗДЕСЯ");
                out.println(ProtocolConstants.CMD_QUIT);
            }
            cleanupSocket();

            connectButton.setDisable(false);
            if (usernameField != null) {
                usernameField.setDisable(false);
            }
            if (passwordField != null) {
                passwordField.setDisable(false);
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
                    if (msg.startsWith(ProtocolConstants.RESP_ONLINE_USERS)) {
                        updateUsersList(msg);
                    } else {
                        appendMessage("Сервер", msg);
                    }
                });
            }
        } catch (IOException e) {
            Platform.runLater(() -> {
                if (connected) {
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
        // Формат: ONLINE_USERS;username1:status:message;username2:status:message
        String[] parts = usersMessage.split("\\" + ProtocolConstants.DELIMITER, 2);
        if (parts.length < 2) return;

        String usersStr = parts[1];
        if (usersStr.isEmpty()) return;

        String[] userEntries = usersStr.split(ProtocolConstants.LIST_DELIMITER);
        for (String entry : userEntries) {
            if (entry.isEmpty()) continue;

            String[] fields = entry.split(ProtocolConstants.FIELD_DELIMITER);
            if (fields.length > 0) {
                usersList.getItems().add(fields[0]);
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
        connected = false;
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