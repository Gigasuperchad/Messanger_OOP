package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;

public class HelloController {
    @FXML
    public ListView<Chat> Chat_list;

    @FXML
    private Button deleteChatButton;

    @FXML
    private Button infoButton;

    @FXML
    private VBox emptyChatsHint;

    @FXML
    private Label chatCountLabel;

    @FXML
    private VBox chatListContainer; // Добавлено: контейнер списка чатов

    private Repository repo;

    @FXML
    public void initialize() {
        System.out.println("Инициализация HelloController");

        // Настройка стилей кнопок
        setupButtonStyles();

        // Настраиваем ListView для растягивания на всю высоту
        configureListView();

        // Устанавливаем фабрику ячеек для красивого отображения чатов
        Chat_list.setCellFactory(lv -> new ListCell<Chat>() {
            private final HBox root = new HBox(10);
            private final Label iconLabel = new Label();
            private final VBox textContainer = new VBox(2);
            private final Label nameLabel = new Label();
            private final Label infoLabel = new Label();

            {
                // Инициализируем компоненты один раз
                root.setAlignment(Pos.CENTER_LEFT);
                root.setStyle("-fx-padding: 8px;");
                root.setMaxWidth(Double.MAX_VALUE); // Растягиваем на всю ширину
                HBox.setHgrow(root, Priority.ALWAYS); // Разрешаем растягивание

                // Настройка иконки
                iconLabel.setStyle("-fx-font-size: 20px;");
                iconLabel.setPrefWidth(30);

                // Настройка текста
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: normal; -fx-text-fill: #333;");
                infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

                // Разрешаем тексту растягиваться
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                infoLabel.setMaxWidth(Double.MAX_VALUE);

                textContainer.getChildren().addAll(nameLabel, infoLabel);
                textContainer.setMaxWidth(Double.MAX_VALUE);
                VBox.setVgrow(textContainer, Priority.ALWAYS);

                root.getChildren().addAll(iconLabel, textContainer);

                // Устанавливаем графику один раз
                setGraphic(root);

                // Настраиваем ячейку для растягивания
                setMaxWidth(Double.MAX_VALUE);
                setPrefWidth(0);
            }

            @Override
            protected void updateItem(Chat chat, boolean empty) {
                super.updateItem(chat, empty);

                if (empty || chat == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // Устанавливаем иконку в зависимости от типа чата
                    if (chat.getUsers().size() > 2) {
                        iconLabel.setText("👥");
                        infoLabel.setText("Групповой чат • " + chat.get_message_count() + " сообщений");
                    } else {
                        iconLabel.setText("💬");
                        infoLabel.setText("Личный чат • " + chat.get_message_count() + " сообщений");
                    }

                    nameLabel.setText(chat.getChatName());

                    // Сбрасываем все стили для лейблов перед применением новых
                    nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: normal; -fx-text-fill: #333;");
                    infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                    iconLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");

                    // Проверяем активность чата
                    if (!chat.getMessages().isEmpty()) {
                        Message lastMessage = chat.getMessages().get(chat.getMessages().size() - 1);
                        long timeDiff = System.currentTimeMillis() - lastMessage.getTimestamp().getTime();
                        long minutesDiff = timeDiff / (1000 * 60);

                        if (minutesDiff < 5) {
                            // Активный чат - выделяем жирным синим
                            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");
                            iconLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #2196F3;");
                        } else if (minutesDiff < 60) {
                            // Недавний чат
                            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: normal; -fx-text-fill: #333;");
                        } else {
                            // Старый чат
                            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: normal; -fx-text-fill: #666;");
                            infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
                            iconLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #666;");
                        }
                    }

                    // Устанавливаем графику
                    setGraphic(root);
                    setText(null);

                    // Базовый стиль ячейки
                    setStyle("-fx-background-color: white; " +
                            "-fx-padding: 0; " +
                            "-fx-border-color: #f0f0f0; " +
                            "-fx-border-width: 0 0 1 0;");

                    // Обработчики событий мыши
                    setOnMouseEntered(e -> {
                        if (!isEmpty()) {
                            setStyle("-fx-background-color: #f5f9ff; " +
                                    "-fx-padding: 0; " +
                                    "-fx-border-color: #d0e3ff; " +
                                    "-fx-border-width: 0 0 1 0; " +
                                    "-fx-cursor: hand;");
                        }
                    });

                    setOnMouseExited(e -> {
                        if (!isEmpty()) {
                            setStyle("-fx-background-color: white; " +
                                    "-fx-padding: 0; " +
                                    "-fx-border-color: #f0f0f0; " +
                                    "-fx-border-width: 0 0 1 0;");
                        }
                    });

                    setOnMousePressed(e -> {
                        if (!isEmpty()) {
                            setStyle("-fx-background-color: #e3f2fd; " +
                                    "-fx-padding: 0; " +
                                    "-fx-border-color: #2196F3; " +
                                    "-fx-border-width: 0 0 1 0; " +
                                    "-fx-cursor: hand;");
                        }
                    });

                    setOnMouseReleased(e -> {
                        if (!isEmpty()) {
                            setStyle("-fx-background-color: #f5f9ff; " +
                                    "-fx-padding: 0; " +
                                    "-fx-border-color: #d0e3ff; " +
                                    "-fx-border-width: 0 0 1 0; " +
                                    "-fx-cursor: hand;");
                        }
                    });
                }
            }
        });

        // Обработчик одинарного клика (выбор чата)
        Chat_list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                updateDeleteButtonState();
                // Подсвечиваем выбранный чат
                Chat selected = Chat_list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    System.out.println("Выбран чат: " + selected.getChatName());
                }
            }
            if (event.getClickCount() == 2) {
                Chat selected = Chat_list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    System.out.println("Открытие чата: " + selected.getChatName());
                    AppManager.getInstance().switchToChatScene(selected);
                }
            }
        });

        // Контекстное меню для чатов
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openItem = new MenuItem("📖 Открыть чат");
        MenuItem deleteItem = new MenuItem("🗑️ Удалить чат");
        MenuItem infoItem = new MenuItem("ℹ️ Информация о чате");
        MenuItem markAsReadItem = new MenuItem("✅ Отметить как прочитанное");

        openItem.setOnAction(event -> {
            Chat selected = Chat_list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                AppManager.getInstance().switchToChatScene(selected);
            }
        });

        deleteItem.setOnAction(event -> handleDeleteChat());

        infoItem.setOnAction(event -> {
            Chat selected = Chat_list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showChatInfo(selected);
            }
        });

        markAsReadItem.setOnAction(event -> {
            Chat selected = Chat_list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAlert("Чат обновлен", "Чат '" + selected.getChatName() + "' отмечен как прочитанный");
            }
        });

        contextMenu.getItems().addAll(openItem, markAsReadItem, new SeparatorMenuItem(), deleteItem, infoItem);
        Chat_list.setContextMenu(contextMenu);

        // Инициализируем подсказку при отсутствии чатов
        if (emptyChatsHint != null) {
            emptyChatsHint.setVisible(false);
        }

        // Инициализируем счетчик чатов
        updateChatCount();

        // Отладочная информация
        System.out.println("HelloController инициализирован");
    }

    private void configureListView() {
        // Настраиваем ListView для растягивания на всю доступную высоту
        Chat_list.setStyle("-fx-background-color: transparent; " +
                "-fx-background-insets: 0; " +
                "-fx-padding: 0; " +
                "-fx-border-width: 0;");

        // Устанавливаем фиксированную высоту ячейки для лучшего контроля
        Chat_list.setFixedCellSize(60);

        // Разрешаем ListView растягиваться
        Chat_list.setMaxWidth(Double.MAX_VALUE);
        Chat_list.setMaxHeight(Double.MAX_VALUE);
    }

    private void setupButtonStyles() {
        // Стиль для кнопки Удалить (красная с белым шрифтом, закругленная)
        if (deleteChatButton != null) {
            deleteChatButton.setStyle("-fx-background-color: #ff4444; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 14px; " +
                    "-fx-background-radius: 20; " +
                    "-fx-border-radius: 20; " +
                    "-fx-border-color: #ff7777; " +
                    "-fx-border-width: 2; " +
                    "-fx-cursor: hand;");
        }

        // Стиль для кнопки Инфо (желтая с черным шрифтом, закругленная)
        if (infoButton != null) {
            infoButton.setStyle("-fx-background-color: #FFC107; " +
                    "-fx-text-fill: #333333; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 14px; " +
                    "-fx-background-radius: 20; " +
                    "-fx-border-radius: 20; " +
                    "-fx-border-color: #FFD54F; " +
                    "-fx-border-width: 2; " +
                    "-fx-cursor: hand; " +
                    "-fx-opacity: 1.0;");
        }
    }

    public void setRepository(Repository repository) {
        System.out.println("Установка репозитория в HelloController");
        this.repo = repository;

        if (Chat_list != null && repo != null) {
            System.out.println("Количество чатов в репозитории: " + repo.getChats().size());

            // Очищаем текущий список и добавляем новые элементы
            Chat_list.getItems().clear();
            Chat_list.getItems().addAll(repo.getChats());

            // Обновляем кнопку удаления
            updateDeleteButtonState();

            // Обновляем счетчик чатов
            updateChatCount();

            // Скрываем/показываем подсказку при отсутствии чатов
            if (emptyChatsHint != null) {
                boolean isEmpty = Chat_list.getItems().isEmpty();
                emptyChatsHint.setVisible(isEmpty);
                System.out.println("Подсказка при отсутствии чатов: " + (isEmpty ? "видна" : "скрыта"));
            }

            // Добавляем слушатель изменений в списке чатов
            repo.getChats().addListener((javafx.collections.ListChangeListener.Change<? extends Chat> change) -> {
                System.out.println("Обнаружено изменение в списке чатов");

                // Обновляем список в UI потоке
                javafx.application.Platform.runLater(() -> {
                    Chat_list.getItems().clear();
                    Chat_list.getItems().addAll(repo.getChats());

                    updateChatCount();
                    updateDeleteButtonState();

                    if (emptyChatsHint != null) {
                        emptyChatsHint.setVisible(repo.getChats().isEmpty());
                    }

                    // Принудительное обновление отображения
                    Chat_list.refresh();
                    System.out.println("Список чатов обновлен, всего: " + Chat_list.getItems().size());
                });
            });

            // Принудительно обновляем отображение
            Chat_list.refresh();
        } else {
            System.err.println("Ошибка: Chat_list или repo равно null");
            if (Chat_list == null) System.err.println("Chat_list is null");
            if (repo == null) System.err.println("repo is null");
        }
    }

    private void updateDeleteButtonState() {
        if (deleteChatButton != null) {
            Chat selected = Chat_list.getSelectionModel().getSelectedItem();
            boolean isDisabled = (selected == null);
            deleteChatButton.setDisable(isDisabled);

            // Обновляем стиль кнопки удаления в зависимости от состояния
            if (isDisabled) {
                deleteChatButton.setStyle("-fx-background-color: #ff9999; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-background-radius: 20; " +
                        "-fx-border-radius: 20; " +
                        "-fx-border-color: #ffbbbb; " +
                        "-fx-border-width: 2; " +
                        "-fx-opacity: 0.7;");
            } else {
                deleteChatButton.setStyle("-fx-background-color: #ff4444; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-background-radius: 20; " +
                        "-fx-border-radius: 20; " +
                        "-fx-border-color: #ff7777; " +
                        "-fx-border-width: 2; " +
                        "-fx-cursor: hand; " +
                        "-fx-opacity: 1.0;");
            }

            System.out.println("Кнопка удаления: " + (isDisabled ? "отключена" : "включена"));
        }
    }

    @FXML
    private void handleProfileButton() {
        System.out.println("Переход к профилю...");
        AppManager.getInstance().switchToProfileScene();
    }

    @FXML
    private void handleLogout() {
        System.out.println("Выход из аккаунта...");

        // Подтверждение выхода
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение выхода");
        confirmAlert.setHeaderText("Вы уверены, что хотите выйти?");
        confirmAlert.setContentText("Вы выйдете из своего аккаунта и вернетесь на экран входа.");

        ButtonType logoutButton = new ButtonType("Выйти", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(logoutButton, cancelButton);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == logoutButton) {
                AppManager.getInstance().logout();
            }
        });
    }

    @FXML
    private void handleCreateChat() {
        System.out.println("Создание нового чата...");
        AppManager.getInstance().openCreateChatWindow();
    }

    @FXML
    private void handleOpenChatInfo() {
        Chat selected = Chat_list.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showChatInfo(selected);
        } else {
            showAlert("Информация", "Выберите чат для просмотра информации");
        }
    }

    @FXML
    private void handleDeleteChat() {
        Chat selected = Chat_list.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите чат для удаления!");
            return;
        }

        // Сохраняем информацию о чате
        int chatId = selected.getId();
        String chatName = selected.getChatName();

        // Подтверждение удаления
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение удаления");
        confirmAlert.setHeaderText("Удаление чата: " + chatName);
        confirmAlert.setContentText("Вы уверены, что хотите удалить этот чат?\n" +
                "Это действие нельзя отменить.\n" +
                "Сообщений в чате: " + selected.get_message_count() + "\n" +
                "Участников: " + selected.getUsers().size());

        ButtonType deleteButton = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(deleteButton, cancelButton);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == deleteButton) {
                try {
                    System.out.println("\n=== УДАЛЕНИЕ ЧАТА ===");
                    System.out.println("Чат: " + chatName);
                    System.out.println("ID: " + chatId);

                    // Удаляем из репозитория
                    if (repo != null) {
                        repo.deleteChat(selected);
                        showAlert("Успех", "Чат '" + chatName + "' успешно удален!");
                    } else {
                        // Резервный вариант
                        Chat_list.getItems().remove(selected);
                        deleteChatFiles(selected);
                        updateUserChatsFile(selected);
                        showAlert("Успех", "Чат удален!");
                    }

                    // Обновляем UI
                    updateDeleteButtonState();
                    updateChatCount();

                    // Обновляем видимость подсказки
                    if (emptyChatsHint != null) {
                        emptyChatsHint.setVisible(Chat_list.getItems().isEmpty());
                    }

                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить чат: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void deleteChatFiles(Chat chat) {
        try {
            // Удаляем файл чата
            String chatFile = "local_chats/chat_" + chat.getId() + ".dat";
            java.io.File file = new java.io.File(chatFile);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("Файл чата удален: " + chatFile);
                } else {
                    System.out.println("Не удалось удалить файл чата: " + chatFile);
                }
            }

            // Удаляем папку с файлами чата (если есть)
            String chatFilesDir = "chat_files/chat_" + chat.getId();
            java.io.File dir = new java.io.File(chatFilesDir);
            if (dir.exists() && dir.isDirectory()) {
                deleteDirectory(dir);
                System.out.println("Папка файлов чата удалена: " + chatFilesDir);
            }

        } catch (Exception e) {
            System.err.println("Ошибка удаления файлов чата: " + e.getMessage());
        }
    }

    private void deleteDirectory(java.io.File dir) {
        if (dir.isDirectory()) {
            java.io.File[] children = dir.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }

    private void updateUserChatsFile(Chat deletedChat) {
        try {
            User currentUser = AppManager.getInstance().getCurrentUser();
            if (currentUser == null) return;

            String userChatsFile = "local_chats/" + currentUser.getNick() + "_chats.dat";
            java.io.File file = new java.io.File(userChatsFile);

            if (file.exists()) {
                java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                        new java.io.FileInputStream(file));
                java.util.List<Integer> chatIds = (java.util.List<Integer>) ois.readObject();
                ois.close();

                // Удаляем ID удаленного чата
                Integer chatIdToRemove = deletedChat.getId();
                boolean removed = chatIds.remove(chatIdToRemove);
                System.out.println("ID чата " + chatIdToRemove +
                        (removed ? " удален из списка" : " не найден в списке"));

                // Сохраняем обновленный список
                java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                        new java.io.FileOutputStream(file));
                oos.writeObject(chatIds);
                oos.close();

                System.out.println("Обновлен файл чатов пользователя. Осталось чатов: " + chatIds.size());
            }

        } catch (Exception e) {
            System.err.println("Ошибка обновления файла чатов пользователя: " + e.getMessage());
        }
    }

    private void showChatInfo(Chat chat) {
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("Информация о чате");
        infoAlert.setHeaderText(chat.getChatName());

        StringBuilder info = new StringBuilder();
        info.append("ID чата: ").append(chat.getId()).append("\n");
        info.append("Сообщений: ").append(chat.get_message_count()).append("\n");
        info.append("Участников: ").append(chat.getUsers().size()).append("\n\n");

        info.append("Участники:\n");
        for (User user : chat.getUsers()) {
            info.append("• ").append(user.getFullName()).append(" (@").append(user.getNick()).append(")\n");
        }

        info.append("\nСоздан: ");
        if (chat.getMessages() != null && !chat.getMessages().isEmpty()) {
            java.util.Date firstMessageDate = chat.getMessages().get(0).getTimestamp();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
            info.append(sdf.format(firstMessageDate));
        } else {
            info.append("Неизвестно");
        }

        // Добавляем статистику активности
        if (!chat.getMessages().isEmpty()) {
            java.util.Date lastMessageDate = chat.getMessages().get(chat.getMessages().size() - 1).getTimestamp();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
            info.append("\nПоследнее сообщение: ").append(sdf.format(lastMessageDate));
        }

        infoAlert.setContentText(info.toString());
        infoAlert.getDialogPane().setPrefSize(400, 300);
        infoAlert.showAndWait();
    }

    private void updateChatCount() {
        if (chatCountLabel != null) {
            int count = Chat_list.getItems().size();
            String word = getChatWord(count);
            chatCountLabel.setText(count + " " + word);
            System.out.println("Обновлен счетчик чатов: " + count + " " + word);
        }
    }

    private String getChatWord(int count) {
        if (count % 10 == 1 && count % 100 != 11) return "чат";
        if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) return "чата";
        return "чатов";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Публичные методы для управления списком чатов

    public void refreshChatList() {
        if (Chat_list != null) {
            Chat_list.refresh();
            updateChatCount();
            System.out.println("Список чатов обновлен вручную");
        }
    }

    public void selectChat(Chat chat) {
        if (Chat_list != null && chat != null) {
            Chat_list.getSelectionModel().select(chat);
            updateDeleteButtonState();
        }
    }

    public Chat getSelectedChat() {
        return Chat_list != null ? Chat_list.getSelectionModel().getSelectedItem() : null;
    }

    // Метод для принудительного обновления при возвращении на эту сцену
    public void onSceneActivated() {
        System.out.println("Активация сцены списка чатов");

        // Обновляем список чатов из репозитория
        if (repo != null && Chat_list != null) {
            Chat_list.getItems().clear();
            Chat_list.getItems().addAll(repo.getChats());
        }

        refreshChatList();
        updateDeleteButtonState();
        updateChatCount();

        // Обновляем видимость подсказки
        if (emptyChatsHint != null) {
            emptyChatsHint.setVisible(Chat_list.getItems().isEmpty());
        }
    }
}