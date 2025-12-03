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

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    @FXML
    public void initialize() {
        sendButton.setDisable(true);
        disconnectButton.setDisable(true);
        messageField.setDisable(true);

        // Загружаем последнего зарегистрированного пользователя
        loadCurrentUser();
    }

    // Метод для установки текущего пользователя (вызывается из RegisterController и AppManager)
    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateUserInfo();
    }

    private void loadCurrentUser() {
        // Пытаемся загрузить из UserStorage
        currentUser = UserStorage.getCurrentUser();
        if (currentUser != null) {
            updateUserInfo();
        }
    }

    private void updateUserInfo() {
        if (currentUser != null && userNameLabel != null) {
            userNameLabel.setText(currentUser.getFullName());

            // Устанавливаем аватар, если есть
            if (currentUser.getAvatarBase64() != null && !currentUser.getAvatarBase64().isEmpty()) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(currentUser.getAvatarBase64());
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    userAvatarView.setImage(image);
                } catch (Exception e) {
                    System.err.println("Ошибка загрузки аватара: " + e.getMessage());
                }
            }

            // Заполняем поле имени пользователя
            if (usernameField != null) {
                usernameField.setText(currentUser.getNick());
            }
        }
    }

    @FXML
    public void connectToServer() {
        try {
            String username = currentUser != null ? currentUser.getNick() : usernameField.getText().trim();

            if (username.isEmpty()) {
                showAlert("Ошибка", "Введите имя пользователя");
                return;
            }

            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(username);

            Thread messageReader = new Thread(this::readMessages);
            messageReader.setDaemon(true);
            messageReader.start();

            connectButton.setDisable(true);
            if (usernameField != null) usernameField.setDisable(true);
            sendButton.setDisable(false);
            disconnectButton.setDisable(false);
            messageField.setDisable(false);

            appendMessage("Система", "Подключено к серверу");

        } catch (IOException e) {
            showAlert("Ошибка подключения", "Не удалось подключиться к серверу: " + e.getMessage());
        }
    }

    @FXML
    public void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            messageField.clear();
        }
    }

    @FXML
    public void disconnectFromServer() {
        try {
            if (out != null) {
                out.println("/quit");
            }
            if (socket != null) {
                socket.close();
            }

            connectButton.setDisable(false);
            if (usernameField != null) {
                usernameField.setDisable(false);
                usernameField.clear();
            }
            sendButton.setDisable(true);
            disconnectButton.setDisable(true);
            messageField.setDisable(true);

            appendMessage("Система", "Отключено от сервера");
        } catch (IOException e) {
            System.err.println("Ошибка при отключении: " + e.getMessage());
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}