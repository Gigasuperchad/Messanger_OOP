package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.ObservableList;

public class HelloController {
    @FXML
    public ListView<Chat> Chat_list;

    @FXML
    private Button statusButton;

    @FXML
    private Button deleteChatButton;

    private Repository repo;

    @FXML
    public void initialize() {
        Chat_list.setCellFactory(lv -> new ListCell<Chat>() {
            @Override
            protected void updateItem(Chat chat, boolean empty) {
                super.updateItem(chat, empty);
                if (empty || chat == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(chat.getChatName() +
                            " (" + chat.get_message_count() + " сообщ.)");

                    // Добавляем иконку статуса чата
                    Label statusIcon = new Label("💬");
                    if (chat.getUsers().size() > 2) {
                        statusIcon.setText("👥");
                    }
                    setGraphic(statusIcon);
                }
            }
        });

        Chat_list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                updateDeleteButtonState();
            }
            if (event.getClickCount() == 2) {
                Chat selected = Chat_list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    System.out.println("Выбран чат: " + selected);
                    AppManager.getInstance().switchToChatScene(selected);
                }
            }
        });

        // Контекстное меню для чатов
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openItem = new MenuItem("📖 Открыть чат");
        MenuItem deleteItem = new MenuItem("🗑️ Удалить чат");
        MenuItem infoItem = new MenuItem("ℹ️ Информация о чате");

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

        contextMenu.getItems().addAll(openItem, deleteItem, new SeparatorMenuItem(), infoItem);
        Chat_list.setContextMenu(contextMenu);

        // Добавляем обработчик для кнопки статуса
        if (statusButton != null) {
            statusButton.setOnAction(event -> handleStatusButton());
        }

        // Добавляем обработчик для кнопки удаления
        if (deleteChatButton != null) {
            deleteChatButton.setOnAction(event -> handleDeleteChat());
        }
    }

    public void setRepository(Repository repository) {
        this.repo = repository;
        if (Chat_list != null && repo != null) {
            Chat_list.setItems(repo.getChats());
            updateDeleteButtonState();
        }
    }

    private void updateDeleteButtonState() {
        if (deleteChatButton != null) {
            Chat selected = Chat_list.getSelectionModel().getSelectedItem();
            deleteChatButton.setDisable(selected == null);
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
        AppManager.getInstance().logout();
    }

    @FXML
    private void handleCreateChat() {
        System.out.println("Создание нового чата...");
        AppManager.getInstance().openCreateChatWindow();
    }

    @FXML
    private void handleStatusButton() {
        System.out.println("Открытие окна статуса...");
        AppManager.getInstance().openStatusWindow();
    }

    @FXML
    private void handleDeleteChat() {
        Chat selected = Chat_list.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите чат для удаления!");
            return;
        }

        // Подтверждение удаления
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение удаления");
        confirmAlert.setHeaderText("Удаление чата: " + selected.getChatName());
        confirmAlert.setContentText("Вы уверены, что хотите удалить этот чат?\n" +
                "Это действие нельзя отменить.\n" +
                "Сообщений в чате: " + selected.get_message_count() + "\n" +
                "Участников: " + selected.getUsers().size());

        ButtonType deleteButton = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(deleteButton, cancelButton);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == deleteButton) {
                deleteSelectedChat(selected);
            }
        });
    }

    private void deleteSelectedChat(Chat chat) {
        try {
            System.out.println("\n=== УДАЛЕНИЕ ЧАТА ===");
            System.out.println("Чат: " + chat.getChatName());
            System.out.println("ID: " + chat.getId());
            System.out.println("Сообщений: " + chat.get_message_count());

            // Используем метод репозитория
            if (repo != null) {
                repo.deleteChat(chat);
                showAlert("Успех", "Чат '" + chat.getChatName() + "' успешно удален!");
            } else {
                // Резервный вариант
                ObservableList<Chat> chats = Chat_list.getItems();
                chats.remove(chat);
                deleteChatFiles(chat);
                updateUserChatsFile(chat);
                showAlert("Успех", "Чат удален!");
            }

            updateDeleteButtonState();

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось удалить чат: " + e.getMessage());
            e.printStackTrace();
        }
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
                chatIds.remove(Integer.valueOf(deletedChat.getId()));

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

        infoAlert.setContentText(info.toString());
        infoAlert.getDialogPane().setPrefSize(400, 300);
        infoAlert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}