package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.*;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.effect.*;

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

    @FXML
    private VBox chatContainer;

    @FXML
    private HBox topBar;

    @FXML
    private VBox rightPanel;

    private Repository repository;
    private Chat chat;
    private User currentUser;
    private ObservableList<String> messages;
    private ObservableList<String> onlineUsers;
    private boolean isSending = false;
    private Timeline messagePulse;

    private File selectedFile;
    private String selectedFileName;
    private String selectedFileType;
    private long selectedFileSize;

    @FXML
    public void initialize() {
        messages = FXCollections.observableArrayList();
        messageListView.setItems(messages);

        // Кастомный рендерер для сообщений с анимациями
        messageListView.setCellFactory(lv -> new AnimatedMessageCell());

        onlineUsers = FXCollections.observableArrayList();
        onlineUsersListView.setItems(onlineUsers);

        // Кастомный рендерер для онлайн-пользователей
        onlineUsersListView.setCellFactory(lv -> new AnimatedUserCell());

        selectedFile = null;
        updateSelectedFileLabel();

        setupEventHandlers();
        setupAnimations();
        startStatusUpdates();
    }

    private void setupAnimations() {
        // Убираем все анимации кнопок - простые стили без анимаций
        if (sendButton != null) {
            sendButton.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; " +
                    "-fx-background-radius: 20; -fx-font-size: 14; -fx-cursor: hand;");
        }

        if (attachImageButton != null) {
            attachImageButton.setStyle("-fx-background-color: #4CAF50; " +
                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; " +
                    "-fx-background-radius: 15; -fx-font-size: 13; -fx-cursor: hand;");
        }

        if (attachFileButton != null) {
            attachFileButton.setStyle("-fx-background-color: #2196F3; " +
                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; " +
                    "-fx-background-radius: 15; -fx-font-size: 13; -fx-cursor: hand;");
        }

        // Анимация фона чата - только если chatContainer не null
        if (chatContainer != null) {
            setupBackgroundAnimation();
        }

        // Пульсация новых сообщений
        messagePulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(messageListView.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(500),
                        new KeyValue(messageListView.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(1000),
                        new KeyValue(messageListView.opacityProperty(), 1.0))
        );
        messagePulse.setCycleCount(Timeline.INDEFINITE);
    }

    private void setupBackgroundAnimation() {
        // Проверяем, что chatContainer не равен null
        if (chatContainer == null) {
            return;
        }

        // Анимация градиента фона
        Timeline backgroundAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(chatContainer.styleProperty(),
                                "-fx-background-color: linear-gradient(to bottom right, #f0f4ff, #e3e9ff);")),
                new KeyFrame(Duration.seconds(5),
                        new KeyValue(chatContainer.styleProperty(),
                                "-fx-background-color: linear-gradient(to bottom right, #e3e9ff, #d6deff);")),
                new KeyFrame(Duration.seconds(10),
                        new KeyValue(chatContainer.styleProperty(),
                                "-fx-background-color: linear-gradient(to bottom right, #f0f4ff, #e3e9ff);"))
        );
        backgroundAnimation.setCycleCount(Timeline.INDEFINITE);
        backgroundAnimation.play();
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

    // Кастомная ячейка для сообщений с анимациями
    private class AnimatedMessageCell extends ListCell<String> {
        private Label messageLabel;
        private VBox container;
        private ScaleTransition appearAnimation;
        private FadeTransition fadeAnimation;

        public AnimatedMessageCell() {
            messageLabel = new Label();
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(550);
            messageLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #333; -fx-padding: 8 12;");

            container = new VBox(messageLabel);
            container.setPadding(new Insets(5));

            // Анимация появления
            appearAnimation = new ScaleTransition(Duration.millis(300), this);
            appearAnimation.setFromX(0.8);
            appearAnimation.setFromY(0.8);
            appearAnimation.setToX(1.0);
            appearAnimation.setToY(1.0);

            // Анимация при наведении
            fadeAnimation = new FadeTransition(Duration.millis(200), this);
            fadeAnimation.setFromValue(1.0);
            fadeAnimation.setToValue(0.95);

            // Эффект тени
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.1));
            shadow.setRadius(5);
            shadow.setSpread(0.1);

            setOnMouseEntered(e -> {
                fadeAnimation.setRate(1);
                fadeAnimation.play();
                container.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; " +
                        "-fx-border-color: #e0e0e0; -fx-border-radius: 15;");
                container.setEffect(shadow);
            });

            setOnMouseExited(e -> {
                fadeAnimation.setRate(-1);
                fadeAnimation.play();
                container.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                        "-fx-border-color: transparent; -fx-border-radius: 15;");
                container.setEffect(null);
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                messageLabel.setText(item);

                // Стилизация в зависимости от типа сообщения
                if (item.contains("🖼️") || item.contains("📎")) {
                    container.setStyle("-fx-background-color: linear-gradient(to right, #e3f2fd, #bbdefb); " +
                            "-fx-background-radius: 15; -fx-border-color: #90caf9; -fx-border-radius: 15;");
                } else if (item.contains("изменено")) {
                    container.setStyle("-fx-background-color: linear-gradient(to right, #fff8e1, #ffecb3); " +
                            "-fx-background-radius: 15; -fx-border-color: #ffd54f; -fx-border-radius: 15;");
                } else if (item.contains(currentUser != null ? currentUser.getNick() : "")) {
                    container.setStyle("-fx-background-color: linear-gradient(to right, #e8f5e9, #c8e6c9); " +
                            "-fx-background-radius: 15; -fx-border-color: #a5d6a7; -fx-border-radius: 15;");
                } else {
                    container.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                            "-fx-border-color: #e0e0e0; -fx-border-radius: 15;");
                }

                setGraphic(container);

                // Анимация появления
                if (appearAnimation != null) {
                    appearAnimation.play();
                }
            }
        }
    }

    // Кастомная ячейка для онлайн-пользователей
    private class AnimatedUserCell extends ListCell<String> {
        private HBox container;
        private Circle statusCircle;
        private Label nameLabel;
        private Timeline pulseAnimation;

        public AnimatedUserCell() {
            statusCircle = new Circle(4);
            statusCircle.setFill(Color.LIMEGREEN);

            nameLabel = new Label();
            nameLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #555;");

            container = new HBox(8, statusCircle, nameLabel);
            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            container.setPadding(new Insets(8, 12, 8, 12));

            // Анимация пульсации статуса
            pulseAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(statusCircle.radiusProperty(), 4)),
                    new KeyFrame(Duration.millis(750),
                            new KeyValue(statusCircle.radiusProperty(), 5)),
                    new KeyFrame(Duration.millis(1500),
                            new KeyValue(statusCircle.radiusProperty(), 4))
            );
            pulseAnimation.setCycleCount(Timeline.INDEFINITE);

            // Эффект при наведении
            FadeTransition hoverFade = new FadeTransition(Duration.millis(200), container);
            hoverFade.setFromValue(1.0);
            hoverFade.setToValue(0.9);

            setOnMouseEntered(e -> {
                hoverFade.setRate(1);
                hoverFade.play();
                container.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 10;");
                pulseAnimation.play();
            });

            setOnMouseExited(e -> {
                hoverFade.setRate(-1);
                hoverFade.play();
                container.setStyle("-fx-background-color: transparent;");
                pulseAnimation.stop();
                statusCircle.setRadius(4);
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                pulseAnimation.stop();
            } else {
                nameLabel.setText(item);

                // Цвет статуса в зависимости от активности
                if (item.contains("🟢")) {
                    statusCircle.setFill(Color.LIMEGREEN);
                } else if (item.contains("🟡")) {
                    statusCircle.setFill(Color.GOLD);
                } else if (item.contains("🔴")) {
                    statusCircle.setFill(Color.CRIMSON);
                } else if (item.contains("👻")) {
                    statusCircle.setFill(Color.GRAY);
                } else {
                    statusCircle.setFill(Color.LIGHTGRAY);
                }

                setGraphic(container);

                // Запускаем анимацию только для онлайн пользователей
                if (!item.contains("😴") && !item.contains("⚫")) {
                    pulseAnimation.play();
                }
            }
        }
    }

    private void startStatusUpdates() {
        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(5),
                        e -> updateChatStatus()
                )
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void updateChatStatus() {
        if (chat != null && currentUser != null) {
            // Анимация обновления статуса
            FadeTransition ft = new FadeTransition(Duration.millis(300), chatStatusLabel);
            ft.setFromValue(0.5);
            ft.setToValue(1.0);
            ft.play();

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
            chatStatusLabel.setStyle("-fx-text-fill: linear-gradient(to right, #667eea, #764ba2); " +
                    "-fx-font-size: 14px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");
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

        // Анимация обновления списка
        FadeTransition ft = new FadeTransition(Duration.millis(300), onlineUsersListView);
        ft.setFromValue(0.7);
        ft.setToValue(1.0);
        ft.play();
    }

    private void handleOnlineUserDoubleClick() {
        String selectedUserString = onlineUsersListView.getSelectionModel().getSelectedItem();

        if (selectedUserString != null && !selectedUserString.equals("😴 Никто не в сети")) {
            try {
                // Извлекаем только имя пользователя без эмодзи и статусного сообщения
                String userDisplayName = selectedUserString;

                // Удаляем эмодзи статуса в начале (все до первого пробела)
                int firstSpaceIndex = userDisplayName.indexOf(' ');
                if (firstSpaceIndex > 0) {
                    userDisplayName = userDisplayName.substring(firstSpaceIndex + 1);
                }

                // Удаляем статусное сообщение, если есть
                int dashIndex = userDisplayName.indexOf(" - ");
                if (dashIndex > 0) {
                    userDisplayName = userDisplayName.substring(0, dashIndex);
                }

                // Ищем пользователя по полному имени
                User selectedUser = findUserByDisplayName(userDisplayName.trim());

                if (selectedUser != null) {
                    openUserProfile(selectedUser);
                } else {
                    // Пробуем найти по никнейму, если полное имя не совпадает
                    selectedUser = findUserByNick(userDisplayName.trim());
                    if (selectedUser != null) {
                        openUserProfile(selectedUser);
                    } else {
                        showAlert("Ошибка", "Пользователь не найден: " + userDisplayName,
                                Alert.AlertType.WARNING);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Не удалось открыть профиль: " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        }
    }

    private User findUserByDisplayName(String displayName) {
        if (chat != null && chat.getUsers() != null) {
            for (User user : chat.getUsers()) {
                if (user.getFullName().equals(displayName)) {
                    return user;
                }
            }
        }
        return null;
    }

    private User findUserByNick(String nick) {
        if (chat != null && chat.getUsers() != null) {
            for (User user : chat.getUsers()) {
                if (user.getNick().equals(nick)) {
                    return user;
                }
            }
        }
        return null;
    }

    private void openUserProfile(User user) {
        try {
            // Получаем главное окно через любой другой элемент, который точно не null
            Stage mainStage = null;

            // Пробуем получить Stage из разных элементов UI
            if (onlineUsersListView != null && onlineUsersListView.getScene() != null) {
                mainStage = (Stage) onlineUsersListView.getScene().getWindow();
            } else if (messageListView != null && messageListView.getScene() != null) {
                mainStage = (Stage) messageListView.getScene().getWindow();
            } else if (sendButton != null && sendButton.getScene() != null) {
                mainStage = (Stage) sendButton.getScene().getWindow();
            } else if (chatContainer != null && chatContainer.getScene() != null) {
                mainStage = (Stage) chatContainer.getScene().getWindow();
            }

            if (mainStage == null) {
                throw new IllegalStateException("Не удалось получить главное окно");
            }

            Stage profileStage = new Stage();
            profileStage.setTitle("Профиль: " + user.getFullName());
            profileStage.setResizable(true);
            profileStage.initOwner(mainStage);
            profileStage.initModality(javafx.stage.Modality.WINDOW_MODAL);

            VBox mainVBox = new VBox(20);
            mainVBox.setPadding(new Insets(30));
            mainVBox.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9ff); " +
                    "-fx-background-radius: 20; -fx-border-color: linear-gradient(to bottom, #667eea, #764ba2); " +
                    "-fx-border-width: 3; -fx-border-radius: 20;");

            Label titleLabel = new Label("👤 Профиль пользователя");
            titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; " +
                    "-fx-text-fill: linear-gradient(to right, #667eea, #764ba2); " +
                    "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 10, 0, 0, 5);");

            StackPane avatarPane = new StackPane();
            Circle circle = new Circle(60);
            circle.setFill(Color.WHITE);
            circle.setStroke(Color.web("#667eea"));
            circle.setStrokeWidth(3);

            ImageView avatarImageView = new ImageView();
            avatarImageView.setFitWidth(110);
            avatarImageView.setFitHeight(110);

            try {
                java.net.URL avatarUrl = getClass().getResource("/default-avatar.png");
                if (avatarUrl != null) {
                    Image avatarImage = new Image(avatarUrl.toExternalForm());
                    avatarImageView.setImage(avatarImage);
                } else {
                    createAvatarPlaceholder(avatarImageView, user);
                }
            } catch (Exception e) {
                createAvatarPlaceholder(avatarImageView, user);
            }

            Circle clipCircle = new Circle(55, 55, 55);
            avatarImageView.setClip(clipCircle);
            avatarPane.getChildren().addAll(circle, avatarImageView);

            VBox infoCard = new VBox(15);
            infoCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25; " +
                    "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.2), 15, 0, 0, 8);");
            infoCard.setPrefWidth(350);

            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(15);
            grid.setPadding(new Insets(10));

            ColumnConstraints col1 = new ColumnConstraints();
            col1.setMinWidth(90);
            col1.setPrefWidth(90);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setMinWidth(220);
            col2.setPrefWidth(220);
            grid.getColumnConstraints().addAll(col1, col2);

            addInfoRow(grid, "Имя:", user.getFirstName(), 0);
            addInfoRow(grid, "Фамилия:", user.getLastName(), 1);
            addInfoRow(grid, "Email:", user.getEmail(), 2);
            addInfoRow(grid, "Логин:", user.getNick(), 3);

            infoCard.getChildren().add(grid);

            Button closeButton = new Button("✖ Закрыть");
            closeButton.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 35; " +
                    "-fx-background-radius: 20; -fx-font-size: 14; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 8, 0, 0, 4);");

            closeButton.setOnAction(e -> profileStage.close());

            mainVBox.getChildren().addAll(titleLabel, avatarPane, infoCard, closeButton);
            mainVBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);

            Scene scene = new Scene(mainVBox, 500, 600);
            profileStage.setScene(scene);

            profileStage.show();

            profileStage.setX(mainStage.getX() + (mainStage.getWidth() - 500) / 2);
            profileStage.setY(mainStage.getY() + (mainStage.getHeight() - 600) / 2);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось открыть профиль: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addInfoRow(GridPane grid, String labelText, String value, int row) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 14;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 14; " +
                "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 5 10;");

        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private void createAvatarPlaceholder(ImageView imageView, User user) {
        try {
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(110, 110);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

            int colorIndex = Math.abs(user.getNick().hashCode()) % 6;
            Color[] colors = {
                    Color.web("#FF6B6B"), Color.web("#4ECDC4"), Color.web("#FFD166"),
                    Color.web("#06D6A0"), Color.web("#118AB2"), Color.web("#9B5DE5")
            };
            Color bgColor = colors[colorIndex];
            Color endColor = bgColor.darker();

            gc.setFill(new javafx.scene.paint.LinearGradient(0, 0, 1, 1, true,
                    CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, bgColor),
                    new javafx.scene.paint.Stop(1, endColor)));
            gc.fillOval(5, 5, 100, 100);

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

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 36));

            double xPos = initials.length() == 1 ? 37 : 27;
            gc.fillText(initials, xPos, 70);

            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            javafx.scene.image.WritableImage image = canvas.snapshot(params, null);
            imageView.setImage(image);

        } catch (Exception e) {
            System.err.println("Не удалось создать аватар: " + e.getMessage());
        }
    }

    private void handleAttachImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        Stage stage = (Stage) attachImageButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
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

        Stage stage = (Stage) attachFileButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
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
            selectedFileLabel.setStyle("-fx-text-fill: linear-gradient(to right, #4CAF50, #2E7D32); " +
                    "-fx-font-weight: bold; -fx-font-size: 13; -fx-effect: dropshadow(gaussian, rgba(76,175,80,0.3), 2, 0, 0, 1);");
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

        if (messageListView != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), messageListView);
            ft.setFromValue(0);
            ft.setToValue(1.0);
            ft.play();
        }

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
                            String fileInfo = "🖼️ " + message.getShortFileInfo();

                            if (!message.getContent().isEmpty()) {
                                messageDisplay = String.format("[%s] %s%s: %s | %s%s",
                                        timestamp, statusIcon, senderNick, fileInfo,
                                        message.getContent(), deliveryStatus);
                            } else {
                                messageDisplay = String.format("[%s] %s%s: %s%s",
                                        timestamp, statusIcon, senderNick, fileInfo, deliveryStatus);
                            }
                        } else {
                            String fileInfo = String.format("📎 %s (%s)",
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
                        String editedMark = message.isEdited() ? " ✏️" : "";

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

            if (!messages.isEmpty()) {
                PauseTransition pt = new PauseTransition(Duration.millis(100));
                pt.setOnFinished(e -> {
                    messageListView.scrollTo(messages.size() - 1);
                    if (messagePulse != null) {
                        messagePulse.play();
                        PauseTransition stopPulse = new PauseTransition(Duration.seconds(2));
                        stopPulse.setOnFinished(ev -> messagePulse.stop());
                        stopPulse.play();
                    }
                });
                pt.play();
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
                    showAlert("Ошибка", "Вы можете редактировать только свои сообщения", Alert.AlertType.WARNING);
                }
            }
        }
    }

    private void showDeliveryDetails(Message message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Статус доставки сообщения");
        alert.setHeaderText("📊 Информация о доставке");

        alert.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9ff); " +
                "-fx-border-color: linear-gradient(to right, #667eea, #764ba2); -fx-border-width: 3;");

        StringBuilder content = new StringBuilder();
        content.append("💬 Сообщение: ").append(message.getContent()).append("\n\n");

        if (message.getDeliveryStatus() != null) {
            content.append(message.getDeliveryStatus().getDetailedStatus());
        }

        content.append("\n\n👁️ Прочитали (").append(message.getReadCount()).append("):\n");
        for (String username : message.getReadBy().keySet()) {
            if (message.isReadBy(username)) {
                content.append("• ").append(username).append("\n");
            }
        }

        alert.setContentText(content.toString());
        alert.getDialogPane().setPrefSize(450, 350);
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
                                showFileInfo(message);
                            }
                        } catch (Exception e) {
                            showFileInfo(message);
                        }
                    }
                } else {
                    showAlert("Файл не найден", "Файл " + message.getFileName() + " не найден.", Alert.AlertType.ERROR);
                }
            }
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось открыть файл: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showFileInfo(Message message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация о файле");
        alert.setHeaderText("📄 " + message.getFileName());
        alert.setContentText("📁 Путь: " + message.getFilePath() +
                "\n📊 Размер: " + message.getFormattedFileSize() +
                "\n📅 Тип: " + message.getFileType());
        alert.getDialogPane().setPrefSize(400, 200);
        alert.showAndWait();
    }

    private void showImagePreview(Message message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🖼️ Просмотр изображения: " + message.getFileName());
        alert.setHeaderText("Изображение от " + message.getSender().getNick());

        try {
            Image image = new Image("file:" + message.getFilePath());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(500);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            alert.getDialogPane().setContent(imageView);
            alert.getDialogPane().setPrefSize(520, 550);

            alert.getDialogPane().setStyle("-fx-background-color: #1a1a1a; " +
                    "-fx-border-color: linear-gradient(to right, #667eea, #764ba2); -fx-border-width: 3;");
        } catch (Exception e) {
            alert.setContentText("Не удалось загрузить изображение: " + e.getMessage());
        }

        alert.showAndWait();
    }

    private void openEditDialog(Message message, int index) {
        TextInputDialog dialog = new TextInputDialog(message.getContent());
        dialog.setTitle("✏️ Редактирование сообщения");
        dialog.setHeaderText("Редактируйте ваше сообщение");
        dialog.setContentText("Новый текст сообщения:");

        dialog.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9ff); " +
                "-fx-border-color: #667eea; -fx-border-width: 2;");

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
                sendButton.setText("⏳");

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

                sendButton.setText("✓");
                PauseTransition pt = new PauseTransition(Duration.millis(500));
                pt.setOnFinished(e -> sendButton.setText("Отправить"));
                pt.play();

            } catch (Exception e) {
                System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
                sendButton.setText("✗");
                PauseTransition pt = new PauseTransition(Duration.millis(1000));
                pt.setOnFinished(ev -> sendButton.setText("Отправить"));
                pt.play();
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
            statusStage.setTitle("🎭 Настройка статуса");
            statusStage.setScene(new Scene(root, 450, 500));
            statusStage.show();

        } catch (Exception e) {
            System.err.println("Ошибка открытия окна статуса: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        switch(type) {
            case INFORMATION:
                alert.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom, #e8f5e9, #c8e6c9); " +
                        "-fx-border-color: #4CAF50; -fx-border-width: 2;");
                break;
            case WARNING:
                alert.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom, #fff3e0, #ffe0b2); " +
                        "-fx-border-color: #FF9800; -fx-border-width: 2;");
                break;
            case ERROR:
                alert.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom, #ffebee, #ffcdd2); " +
                        "-fx-border-color: #F44336; -fx-border-width: 2;");
                break;
        }

        alert.showAndWait();
    }
}