package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

import java.util.Optional;
import java.util.List;
import java.util.Date;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class ChatController {
    @FXML
    private ListView<String> messageListView;

    @FXML
    private TextArea messageTextArea;

    @FXML
    private Button sendButton;

    @FXML
    private Button attachImageButton;

    @FXML
    private Button attachFileButton;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private Label chatStatusLabel;

    @FXML
    private ListView<String> onlineUsersListView;

    private Repository repository;
    private Chat chat;
    private User currentUser;
    private ObservableList<String> messages;
    private ObservableList<String> onlineUsers;
    private boolean isSending = false;

    private File selectedFile;
    private String selectedFileName;
    private String selectedFileType;
    private long selectedFileSize;

    @FXML
    public void initialize() {
        messages = FXCollections.observableArrayList();
        messageListView.setItems(messages);

        onlineUsers = FXCollections.observableArrayList();
        onlineUsersListView.setItems(onlineUsers);

        selectedFile = null;
        updateSelectedFileLabel();

        setupEventHandlers();
        startStatusUpdates();
    }

    private void setupEventHandlers() {
        sendButton.setOnAction(event -> handleSendMessage());
        attachImageButton.setOnAction(event -> handleAttachImage());
        attachFileButton.setOnAction(event -> handleAttachFile());

        messageTextArea.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                handleSendMessage();
            }
        });

        messageListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleMessageDoubleClick(event);
            }
        });

        onlineUsersListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleOnlineUserDoubleClick();
            }
        });
    }

    private void startStatusUpdates() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(10),
                        e -> updateChatStatus()
                )
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void updateChatStatus() {
        if (chat != null && currentUser != null) {
            updateChatStatusLabel();
            updateOnlineUsersList();
        }
    }

    private void updateChatStatusLabel() {
        if (chat == null) return;

        StringBuilder statusText = new StringBuilder();
        int totalUsers = chat.getUsers().size();
        int onlineCount = 0;

        for (User user : chat.getUsers()) {
            if (StatusManager.getInstance().isUserOnline(user.getNick())) {
                onlineCount++;
            }
        }

        statusText.append("👥 Участников: ").append(totalUsers)
                .append(" | 🟢 Онлайн: ").append(onlineCount);

        if (chatStatusLabel != null) {
            chatStatusLabel.setText(statusText.toString());
            chatStatusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");
        }
    }

    private void updateOnlineUsersList() {
        if (chat == null || onlineUsersListView == null) return;

        onlineUsers.clear();

        for (User user : chat.getUsers()) {
            String username = user.getNick();
            UserStatus status = StatusManager.getInstance().getUserStatus(username);

            if (status != null && status.isOnline()) {
                String display = status.getIcon() + " " + user.getFullName();
                if (!status.getCustomMessage().isEmpty()) {
                    display += " - " + status.getCustomMessage();
                }
                onlineUsers.add(display);
            }
        }

        if (onlineUsers.isEmpty()) {
            onlineUsers.add("😴 Никто не в сети");
        }
    }

    private void handleOnlineUserDoubleClick() {
        String selectedUserString = onlineUsersListView.getSelectionModel().getSelectedItem();
        if (selectedUserString != null && !selectedUserString.equals("😴 Никто не в сети")) {
            String displayText = selectedUserString;
            String userInfo = displayText.replaceFirst("^[\\p{So}\\p{Cn}]\\s*", "").trim();

            if (userInfo.contains(" - ")) {
                userInfo = userInfo.substring(0, userInfo.indexOf(" - ")).trim();
            }

            User selectedUser = findUserByDisplayName(userInfo);
            if (selectedUser != null) {
                openUserProfile(selectedUser);
            }
        }
    }

    private User findUserByDisplayName(String displayName) {
        if (chat != null && chat.getUsers() != null) {
            for (User user : chat.getUsers()) {
                if (user.getFullName().equals(displayName) ||
                        user.getNick().equals(displayName) ||
                        (user.getFirstName() + " " + user.getLastName()).equals(displayName)) {
                    return user;
                }
            }
        }
        return null;
    }

    private void openUserProfile(User user) {
        try {
            Stage profileStage = new Stage();
            profileStage.setTitle("Профиль: " + user.getFullName());
            profileStage.setResizable(false);

            VBox mainVBox = new VBox(20);
            mainVBox.setPadding(new Insets(30));
            mainVBox.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9ff, #eef1ff);");

            Label titleLabel = new Label("👤 Профиль пользователя");
            titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: linear-gradient(to right, #667eea, #764ba2);");

            StackPane avatarPane = new StackPane();
            Circle circle = new Circle(55);
            circle.setFill(Color.WHITE);
            circle.setStroke(Color.web("#667eea"));
            circle.setStrokeWidth(3);

            ImageView avatarImageView = new ImageView();
            avatarImageView.setFitWidth(100);
            avatarImageView.setFitHeight(100);

            try {
                // Используем правильный путь к ресурсам
                Image avatarImage = new Image(getClass().getResource("/default-avatar.png").toExternalForm());
                avatarImageView.setImage(avatarImage);
            } catch (Exception e) {
                // Если изображение не найдено, создаем цветной круг с инициалами
                System.err.println("Не удалось загрузить аватар: " + e.getMessage());
                // Создаем круг с цветом и инициалами
                createAvatarPlaceholder(avatarImageView, user);
            }

            Circle clipCircle = new Circle(50, 50, 50);
            avatarImageView.setClip(clipCircle);
            avatarPane.getChildren().addAll(circle, avatarImageView);

            VBox infoCard = new VBox(15);
            infoCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
            infoCard.setPrefWidth(350);

            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(12);

            ColumnConstraints col1 = new ColumnConstraints();
            col1.setMinWidth(80);
            col1.setPrefWidth(80);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setMinWidth(200);
            col2.setPrefWidth(200);
            grid.getColumnConstraints().addAll(col1, col2);

            Label firstNameLabel = new Label("Имя:");
            firstNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
            Label firstNameValue = new Label(user.getFirstName());
            firstNameValue.setStyle("-fx-text-fill: #333; -fx-font-size: 14;");
            grid.add(firstNameLabel, 0, 0);
            grid.add(firstNameValue, 1, 0);

            Label lastNameLabel = new Label("Фамилия:");
            lastNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
            Label lastNameValue = new Label(user.getLastName());
            lastNameValue.setStyle("-fx-text-fill: #333; -fx-font-size: 14;");
            grid.add(lastNameLabel, 0, 1);
            grid.add(lastNameValue, 1, 1);

            Label emailLabel = new Label("Email:");
            emailLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
            Label emailValue = new Label(user.getEmail());
            emailValue.setStyle("-fx-text-fill: #333; -fx-font-size: 14;");
            grid.add(emailLabel, 0, 2);
            grid.add(emailValue, 1, 2);

            Label nicknameLabel = new Label("Логин:");
            nicknameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
            Label nicknameValue = new Label(user.getNick());
            nicknameValue.setStyle("-fx-text-fill: #333; -fx-font-size: 14;");
            grid.add(nicknameLabel, 0, 3);
            grid.add(nicknameValue, 1, 3);

            infoCard.getChildren().add(grid);

            Button closeButton = new Button("✖ Закрыть");
            closeButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 10 30; -fx-background-radius: 20; -fx-font-size: 14; -fx-cursor: hand;");
            closeButton.setOnAction(e -> profileStage.close());

            mainVBox.getChildren().addAll(titleLabel, avatarPane, infoCard, closeButton);

            Scene scene = new Scene(mainVBox, 450, 550);
            profileStage.setScene(scene);
            profileStage.show();

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось открыть профиль: " + e.getMessage());
        }
    }

    private void createAvatarPlaceholder(ImageView imageView, User user) {
        try {
            // Создаем изображение с цветным кругом и инициалами
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(100, 100);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

            // Цвет на основе хеша имени пользователя
            int colorIndex = Math.abs(user.getNick().hashCode()) % 5;
            Color[] colors = {
                    Color.web("#FF6B6B"), // Красный
                    Color.web("#4ECDC4"), // Бирюзовый
                    Color.web("#FFD166"), // Желтый
                    Color.web("#06D6A0"), // Зеленый
                    Color.web("#118AB2")  // Синий
            };
            Color bgColor = colors[colorIndex];

            // Рисуем круг
            gc.setFill(bgColor);
            gc.fillOval(0, 0, 100, 100);

            // Инициалы
            String initials = "";
            if (!user.getFirstName().isEmpty()) {
                initials += user.getFirstName().charAt(0);
            }
            if (!user.getLastName().isEmpty()) {
                initials += user.getLastName().charAt(0);
            }
            if (initials.isEmpty()) {
                initials = user.getNick().substring(0, Math.min(2, user.getNick().length())).toUpperCase();
            }

            // Рисуем инициалы (упрощенный подход - центрируем примерно)
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 30));

            // Для 1 символа - позиция 38, для 2 символов - 28 (примерно)
            double xPos = initials.length() == 1 ? 38 : 28;
            gc.fillText(initials, xPos, 60);

            // Создаем изображение из canvas
            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            javafx.scene.image.WritableImage image = canvas.snapshot(params, null);
            imageView.setImage(image);

        } catch (Exception e) {
            // Если что-то пошло не так, создаем простой цветной круг без текста
            System.err.println("Не удалось создать аватар: " + e.getMessage());

            // Просто цветной круг без текста
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(100, 100);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

            int colorIndex = Math.abs(user.getNick().hashCode()) % 5;
            Color[] colors = {
                    Color.web("#FF6B6B"), Color.web("#4ECDC4"), Color.web("#FFD166"),
                    Color.web("#06D6A0"), Color.web("#118AB2")
            };

            gc.setFill(colors[colorIndex]);
            gc.fillOval(0, 0, 100, 100);

            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            javafx.scene.image.WritableImage image = canvas.snapshot(params, null);
            imageView.setImage(image);
        }
    }

    private void handleAttachImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        File file = fileChooser.showOpenDialog(attachImageButton.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            selectedFileName = file.getName();
            selectedFileType = getFileType(file);
            selectedFileSize = file.length();
            updateSelectedFileLabel();
        }
    }

    private void handleAttachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Все файлы", "*.*"),
                new FileChooser.ExtensionFilter("Документы", "*.pdf", "*.doc", "*.docx", "*.txt", "*.rtf"),
                new FileChooser.ExtensionFilter("Архивы", "*.zip", "*.rar", "*.7z")
        );

        File file = fileChooser.showOpenDialog(attachFileButton.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            selectedFileName = file.getName();
            selectedFileType = getFileType(file);
            selectedFileSize = file.length();
            updateSelectedFileLabel();
        }
    }

    private String getFileType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
                fileName.endsWith(".bmp")) {
            return "image/" + fileName.substring(fileName.lastIndexOf(".") + 1);
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            return "application/msword";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }

    private void updateSelectedFileLabel() {
        if (selectedFile != null) {
            selectedFileLabel.setText("📎 " + selectedFileName + " (" + formatFileSize(selectedFileSize) + ")");
            selectedFileLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } else {
            selectedFileLabel.setText("");
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " Б";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f КБ", bytes / 1024.0);
        } else {
            return String.format("%.1f МБ", bytes / (1024.0 * 1024.0));
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
        updateMessageList();
        updateChatStatus();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            StatusManager.getInstance().setUserOnline(user.getNick());
        }
    }

    private void updateMessageList() {
        if (chat != null) {
            messages.clear();
            List<Message> chatMessages = chat.getMessages();
            if (chatMessages != null) {
                for (Message message : chatMessages) {
                    String timestamp = formatTimestamp(message.getTimestamp());
                    String senderNick = message.getSender() != null ?
                            message.getSender().getNick() : "Неизвестный";

                    UserStatus senderStatus = StatusManager.getInstance().getUserStatus(senderNick);
                    String statusIcon = senderStatus != null ? senderStatus.getIcon() : "⚫";

                    String deliveryStatus = "";
                    if (message.getDeliveryStatus() != null) {
                        deliveryStatus = " " + message.getDeliveryStatus().getStatus().getIcon();
                    }

                    String messageDisplay;
                    if (message.hasAttachment()) {
                        if (message.getFileType() != null && message.getFileType().startsWith("image/")) {
                            String fileInfo = message.getShortFileInfo();

                            if (!message.getContent().isEmpty()) {
                                messageDisplay = String.format("[%s] %s%s: %s | %s%s",
                                        timestamp, statusIcon, senderNick, fileInfo,
                                        message.getContent(), deliveryStatus);
                            } else {
                                messageDisplay = String.format("[%s] %s%s: %s%s",
                                        timestamp, statusIcon, senderNick, fileInfo, deliveryStatus);
                            }
                        } else {
                            String fileInfo = String.format("%s %s (%s)",
                                    message.getFileIcon(),
                                    message.getFileName(),
                                    message.getFormattedFileSize());

                            if (!message.getContent().isEmpty()) {
                                messageDisplay = String.format("[%s] %s%s: %s | %s%s",
                                        timestamp, statusIcon, senderNick, fileInfo,
                                        message.getContent(), deliveryStatus);
                            } else {
                                messageDisplay = String.format("[%s] %s%s: %s%s",
                                        timestamp, statusIcon, senderNick, fileInfo, deliveryStatus);
                            }
                        }
                    } else {
                        String content = message.getContent() != null ? message.getContent() : "";
                        String editedMark = message.isEdited() ? " (изменено)" : "";

                        String readCount = "";
                        int readCountNum = message.getReadCount();
                        if (readCountNum > 0) {
                            readCount = " 👁️" + readCountNum;
                        }

                        messageDisplay = String.format("[%s] %s%s: %s%s%s%s",
                                timestamp, statusIcon, senderNick,
                                content, editedMark, deliveryStatus, readCount);
                    }
                    messages.add(messageDisplay);
                }
            }
        }
    }

    private String formatTimestamp(Date timestamp) {
        if (timestamp == null) return "Неизвестное время";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        return sdf.format(timestamp);
    }

    private void handleMessageDoubleClick(javafx.scene.input.MouseEvent event) {
        int selectedIndex = messageListView.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0 && chat != null && currentUser != null) {
            List<Message> chatMessages = chat.getMessages();
            if (selectedIndex < chatMessages.size()) {
                Message message = chatMessages.get(selectedIndex);

                if (message.hasAttachment() && message.getFilePath() != null) {
                    openAttachment(message);
                    return;
                }

                if (event.isControlDown() || event.isShortcutDown()) {
                    showDeliveryDetails(message);
                    return;
                }

                if (message.getSender() != null &&
                        currentUser.getNick().equals(message.getSender().getNick())) {
                    openEditDialog(message, selectedIndex);
                } else {
                    showAlert("Ошибка", "Вы можете редактировать только свои сообщения");
                }
            }
        }
    }

    private void showDeliveryDetails(Message message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Статус доставки сообщения");
        alert.setHeaderText("Информация о доставке");

        StringBuilder content = new StringBuilder();
        content.append("Сообщение: ").append(message.getContent()).append("\n\n");

        if (message.getDeliveryStatus() != null) {
            content.append(message.getDeliveryStatus().getDetailedStatus());
        }

        content.append("\n\nПрочитали (").append(message.getReadCount()).append("):\n");
        for (String username : message.getReadBy().keySet()) {
            if (message.isReadBy(username)) {
                content.append("• ").append(username).append("\n");
            }
        }

        alert.setContentText(content.toString());
        alert.getDialogPane().setPrefSize(400, 300);
        alert.showAndWait();
    }

    private void openAttachment(Message message) {
        try {
            if (message.getFilePath() != null) {
                File file = new File(message.getFilePath());
                if (file.exists()) {
                    if (message.getFileType().startsWith("image/")) {
                        showImagePreview(message);
                    } else {
                        try {
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop.getDesktop().open(file);
                            } else {
                                showAlert("Файл",
                                        "Файл: " + message.getFileName() +
                                                "\nПуть: " + message.getFilePath() +
                                                "\nРазмер: " + message.getFormattedFileSize());
                            }
                        } catch (Exception e) {
                            showAlert("Файл",
                                    "Файл: " + message.getFileName() +
                                            "\nПуть: " + message.getFilePath() +
                                            "\nРазмер: " + message.getFormattedFileSize());
                        }
                    }
                } else {
                    showAlert("Файл не найден", "Файл " + message.getFileName() + " не найден.");
                }
            }
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось открыть файл: " + e.getMessage());
        }
    }

    private void showImagePreview(Message message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Просмотр изображения: " + message.getFileName());
        alert.setHeaderText("Изображение от " + message.getSender().getNick());

        try {
            javafx.scene.image.Image image = new javafx.scene.image.Image("file:" + message.getFilePath());
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
            imageView.setFitWidth(400);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            alert.getDialogPane().setContent(imageView);
            alert.getDialogPane().setPrefSize(420, 450);
        } catch (Exception e) {
            alert.setContentText("Не удалось загрузить изображение: " + e.getMessage());
        }

        alert.showAndWait();
    }

    private void openEditDialog(Message message, int index) {
        TextInputDialog dialog = new TextInputDialog(message.getContent());
        dialog.setTitle("Редактирование сообщения");
        dialog.setHeaderText("Редактируйте ваше сообщение");
        dialog.setContentText("Новый текст сообщения:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newText -> {
            if (!newText.trim().isEmpty() && !newText.equals(message.getContent())) {
                Message updatedMessage = new Message(message.getSender(), newText, message.getTimestamp());
                updatedMessage.setEdited(true);

                if (message.hasAttachment()) {
                    updatedMessage.setFilePath(message.getFilePath());
                    updatedMessage.setFileName(message.getFileName());
                    updatedMessage.setFileType(message.getFileType());
                    updatedMessage.setFileSize(message.getFileSize());
                    updatedMessage.setHasAttachment(true);
                }

                updatedMessage.setDeliveryStatus(message.getDeliveryStatus());
                updatedMessage.setReadBy(message.getReadBy());

                chat.getMessages().set(index, updatedMessage);

                if (repository != null) {
                    repository.updateMessage(chat, index, updatedMessage);
                }

                updateMessageList();
            }
        });
    }

    @FXML
    private void handleSendMessage() {
        if (isSending) return;

        String text = messageTextArea.getText().trim();

        if ((!text.isEmpty() || selectedFile != null) && chat != null && currentUser != null) {
            isSending = true;

            try {
                Message message;

                if (selectedFile != null) {
                    String chatFilesDir = "chat_files/chat_" + chat.getId();
                    File chatDir = new File(chatFilesDir);
                    if (!chatDir.exists()) {
                        chatDir.mkdirs();
                    }

                    String uniqueFileName = System.currentTimeMillis() + "_" + selectedFileName;
                    String filePath = chatFilesDir + "/" + uniqueFileName;
                    Files.copy(selectedFile.toPath(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);

                    message = new Message(currentUser, text, new Date(),
                            filePath, selectedFileName, selectedFileType, selectedFileSize);
                } else {
                    message = new Message(currentUser, text, new Date());
                }

                message.getDeliveryStatus().setStatus(MessageDeliveryStatus.Status.SENDING);

                if (repository != null) {
                    if (repository instanceof LocalRepository) {
                        ((LocalRepository) repository).saveMessage(message, chat);
                    } else {
                        repository.send_msg(chat, text);
                    }

                    message.getDeliveryStatus().setStatus(MessageDeliveryStatus.Status.SENT);
                    message.markAsRead(currentUser.getNick());
                } else {
                    chat.send_message(message);
                    message.getDeliveryStatus().setStatus(MessageDeliveryStatus.Status.SENT);
                    message.markAsRead(currentUser.getNick());
                }

                updateMessageList();

                messageTextArea.clear();
                selectedFile = null;
                selectedFileName = null;
                selectedFileType = null;
                selectedFileSize = 0;
                updateSelectedFileLabel();

            } catch (Exception e) {
                System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
            } finally {
                isSending = false;
            }
        } else {
            isSending = false;
        }
    }

    @FXML
    private void handleBackToChatList() {
        if (currentUser != null) {
            StatusManager.getInstance().updateLastSeen(currentUser.getNick());
        }
        AppManager.getInstance().switchToChatList();
    }

    @FXML
    private void handleStatusButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StatusWindow.fxml"));
            Parent root = loader.load();

            Stage statusStage = new Stage();
            statusStage.setTitle("Настройка статуса");
            statusStage.setScene(new Scene(root, 400, 300));
            statusStage.show();

        } catch (Exception e) {
            System.err.println("Ошибка открытия окна статуса: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}