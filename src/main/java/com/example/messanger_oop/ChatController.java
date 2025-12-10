package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

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

    // Переменные для хранения выбранного файла
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

        // Инициализация выбранного файла
        selectedFile = null;
        updateSelectedFileLabel();

        // Обработчики кнопок
        sendButton.setOnAction(event -> handleSendMessage());
        attachImageButton.setOnAction(event -> handleAttachImage());
        attachFileButton.setOnAction(event -> handleAttachFile());

        // Обработка Enter в поле ввода
        messageTextArea.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                handleSendMessage();
            }
        });

        // Обработчик двойного клика для сообщений и файлов
        messageListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleMessageDoubleClick(event);
            }
        });

        // Обновляем статусы каждые 10 секунд
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
            System.out.println("Выбрано изображение: " + selectedFileName + " (" + selectedFileSize + " байт)");
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
            System.out.println("Выбран файл: " + selectedFileName + " (" + selectedFileSize + " байт)");
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
        System.out.println("Установлен текущий пользователь: " +
                (user != null ? user.getNick() : "null"));

        // Устанавливаем статус онлайн для пользователя
        if (user != null) {
            StatusManager.getInstance().setUserOnline(user.getNick());
        }
    }

    private void updateMessageList() {
        if (chat != null) {
            messages.clear();
            List<Message> chatMessages = chat.getMessages();
            if (chatMessages != null) {
                for (int i = 0; i < chatMessages.size(); i++) {
                    Message message = chatMessages.get(i);
                    String timestamp = formatTimestamp(message.getTimestamp());
                    String senderNick = message.getSender() != null ?
                            message.getSender().getNick() : "Неизвестный";

                    // Получаем статус отправителя
                    UserStatus senderStatus = StatusManager.getInstance().getUserStatus(senderNick);
                    String statusIcon = senderStatus != null ? senderStatus.getIcon() : "⚫";

                    // Статус доставки
                    String deliveryStatus = "";
                    if (message.getDeliveryStatus() != null) {
                        deliveryStatus = " " + message.getDeliveryStatus().getStatus().getIcon();
                    }

                    // Форматирование сообщения
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
                        String content = message.getContent() != null ?
                                message.getContent() : "";
                        String editedMark = message.isEdited() ? " (изменено)" : "";

                        // Количество прочитавших
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
            } else {
                System.err.println("Сообщения в чате null");
            }
        } else {
            System.err.println("Чат null");
        }
    }

    private String formatTimestamp(Date timestamp) {
        if (timestamp == null) return "Неизвестное время";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        return sdf.format(timestamp);
    }

    private void handleMessageDoubleClick(javafx.scene.input.MouseEvent event) {
        int selectedIndex = messageListView.getSelectionModel().getSelectedIndex();

        System.out.println("\n=== ДВОЙНОЙ КЛИК НА СООБЩЕНИИ ===");
        System.out.println("Выбранный индекс: " + selectedIndex);

        if (selectedIndex >= 0 && chat != null && currentUser != null) {
            List<Message> chatMessages = chat.getMessages();
            if (selectedIndex < chatMessages.size()) {
                Message message = chatMessages.get(selectedIndex);

                // Если сообщение содержит файл - открываем его
                if (message.hasAttachment() && message.getFilePath() != null) {
                    openAttachment(message);
                    return;
                }

                // Проверяем, зажата ли клавиша Ctrl (через событие мыши)
                if (event.isControlDown() || event.isShortcutDown()) {
                    // Ctrl+двойной клик для статуса доставки
                    showDeliveryDetails(message);
                    return;
                }

                System.out.println("Проверка прав доступа:");
                System.out.println("   Отправитель сообщения: " +
                        (message.getSender() != null ? message.getSender().getNick() : "null"));
                System.out.println("   Текущий пользователь: " + currentUser.getNick());

                // Проверяем, что сообщение принадлежит текущему пользователю
                if (message.getSender() != null &&
                        currentUser.getNick().equals(message.getSender().getNick())) {

                    openEditDialog(message, selectedIndex);
                } else {
                    showAlert("Ошибка", "Вы можете редактировать только свои сообщения");
                    System.out.println("Отказ в доступе: сообщение принадлежит другому пользователю");
                }
            } else {
                System.err.println("Индекс сообщения вне диапазона");
            }
        } else {
            System.err.println("Невозможно редактировать: " +
                    "selectedIndex=" + selectedIndex + ", " +
                    "chat=" + (chat != null) + ", " +
                    "currentUser=" + (currentUser != null));
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
                    // Для изображений показываем превью
                    if (message.getFileType().startsWith("image/")) {
                        showImagePreview(message);
                    } else {
                        // Для других файлов пытаемся открыть системным приложением
                        try {
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop.getDesktop().open(file);
                                System.out.println("Файл открыт: " + message.getFileName());
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
            e.printStackTrace();
        }
    }

    private void showImagePreview(Message message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Просмотр изображения: " + message.getFileName());
        alert.setHeaderText("Изображение от " + message.getSender().getNick());

        // Создаем ImageView для показа изображения
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
        System.out.println("Открытие диалога редактирования для сообщения " + index);

        TextInputDialog dialog = new TextInputDialog(message.getContent());
        dialog.setTitle("Редактирование сообщения");
        dialog.setHeaderText("Редактируйте ваше сообщение");
        dialog.setContentText("Новый текст сообщения:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newText -> {
            if (!newText.trim().isEmpty() && !newText.equals(message.getContent())) {
                System.out.println("Сообщение будет обновлено:");
                System.out.println("   Старый текст: '" + message.getContent() + "'");
                System.out.println("   Новый текст: '" + newText + "'");

                // Создаем обновленное сообщение
                Message updatedMessage = new Message(message.getSender(), newText, message.getTimestamp());
                updatedMessage.setEdited(true);

                // Если было вложение, сохраняем его
                if (message.hasAttachment()) {
                    updatedMessage.setFilePath(message.getFilePath());
                    updatedMessage.setFileName(message.getFileName());
                    updatedMessage.setFileType(message.getFileType());
                    updatedMessage.setFileSize(message.getFileSize());
                    updatedMessage.setHasAttachment(true);
                }

                // Сохраняем статус доставки
                updatedMessage.setDeliveryStatus(message.getDeliveryStatus());
                updatedMessage.setReadBy(message.getReadBy());

                // Обновляем в чате
                chat.getMessages().set(index, updatedMessage);

                // Сохраняем через репозиторий
                if (repository != null) {
                    repository.updateMessage(chat, index, updatedMessage);
                }

                // Обновляем отображение
                updateMessageList();

                System.out.println("Сообщение успешно отредактировано!");
            } else {
                System.out.println("Текст не изменился, редактирование отменено");
            }
        });
    }

    @FXML
    private void handleSendMessage() {
        if (isSending) {
            System.out.println("Отправка уже выполняется, пропускаем...");
            return;
        }

        String text = messageTextArea.getText().trim();

        System.out.println("=== Отправка сообщения ===");
        System.out.println("Текст: '" + text + "'");
        System.out.println("Чат: " + (chat != null ? chat.getChatName() : "null"));
        System.out.println("Текущий пользователь: " +
                (currentUser != null ? currentUser.getNick() : "null"));

        if ((!text.isEmpty() || selectedFile != null) && chat != null && currentUser != null) {
            isSending = true;

            try {
                Message message;

                if (selectedFile != null) {
                    // Создаем папку для файлов чата, если её нет
                    String chatFilesDir = "chat_files/chat_" + chat.getId();
                    File chatDir = new File(chatFilesDir);
                    if (!chatDir.exists()) {
                        chatDir.mkdirs();
                        System.out.println("Создана директория для файлов чата: " + chatFilesDir);
                    }

                    // Копируем файл в папку чата
                    String uniqueFileName = System.currentTimeMillis() + "_" + selectedFileName;
                    String filePath = chatFilesDir + "/" + uniqueFileName;
                    Files.copy(selectedFile.toPath(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("Файл скопирован: " + filePath);

                    // Создаем сообщение с файлом
                    message = new Message(currentUser, text, new Date(),
                            filePath, selectedFileName, selectedFileType, selectedFileSize);
                } else {
                    // Создаем обычное текстовое сообщение
                    message = new Message(currentUser, text, new Date());
                }

                // Устанавливаем начальный статус отправки
                message.getDeliveryStatus().setStatus(MessageDeliveryStatus.Status.SENDING);

                if (repository != null) {
                    // Проверяем, поддерживает ли репозиторий новый метод
                    if (repository instanceof LocalRepository) {
                        ((LocalRepository) repository).saveMessage(message, chat);
                    } else {
                        // Если не поддерживает, используем старый метод
                        repository.send_msg(chat, text);
                    }

                    // Обновляем статус на "отправлено"
                    message.getDeliveryStatus().setStatus(MessageDeliveryStatus.Status.SENT);

                    // Помечаем сообщение как доставленное для отправителя
                    message.markAsRead(currentUser.getNick());
                } else {
                    chat.send_message(message);
                    message.getDeliveryStatus().setStatus(MessageDeliveryStatus.Status.SENT);
                    message.markAsRead(currentUser.getNick());
                }

                updateMessageList();

                // Очищаем поле ввода и выбранный файл
                messageTextArea.clear();
                selectedFile = null;
                selectedFileName = null;
                selectedFileType = null;
                selectedFileSize = 0;
                updateSelectedFileLabel();

                System.out.println("Сообщение успешно отправлено!");

            } catch (Exception e) {
                System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isSending = false;
            }
        } else {
            System.err.println("Не могу отправить сообщение:");
            System.err.println("  Текст пустой: " + text.isEmpty());
            System.err.println("  Файл не выбран: " + (selectedFile == null));
            System.err.println("  Чат null: " + (chat == null));
            System.err.println("  Пользователь null: " + (currentUser == null));
            isSending = false;
        }
    }

    @FXML
    private void handleBackToChatList() {
        System.out.println("Возврат к списку чатов...");

        // Обновляем статус последнего посещения
        if (currentUser != null) {
            StatusManager.getInstance().updateLastSeen(currentUser.getNick());
        }

        AppManager.getInstance().switchToChatList();
    }

    @FXML
    private void handleStatusButton() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("StatusWindow.fxml"));
            javafx.scene.Parent root = loader.load();

            Stage statusStage = new Stage();
            statusStage.setTitle("Настройка статуса");
            statusStage.setScene(new javafx.scene.Scene(root, 400, 300));
            statusStage.show();

        } catch (Exception e) {
            System.err.println("Ошибка открытия окна статуса: " + e.getMessage());
            e.printStackTrace();
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