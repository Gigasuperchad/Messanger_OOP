package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateChatController {
    @FXML
    private TextField chatNameField;

    @FXML
    private ListView<User> usersListView;

    @FXML
    private ListView<User> selectedUsersListView;

    @FXML
    private Button addUserButton;

    @FXML
    private Button removeUserButton;

    @FXML
    private Button createChatButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label statusLabel;

    private List<User> allUsers;
    private User currentUser;

    @FXML
    public void initialize() {
        allUsers = new ArrayList<>();

        usersListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getFullName() + " (" + user.getNick() + ")");
                }
            }
        });

        selectedUsersListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getFullName() + " (" + user.getNick() + ")");
                }
            }
        });

        addUserButton.setOnAction(event -> handleAddUser());
        removeUserButton.setOnAction(event -> handleRemoveUser());
        createChatButton.setOnAction(event -> handleCreateChat());
        cancelButton.setOnAction(event -> handleCancel());

        javafx.application.Platform.runLater(() -> {
            loadAllUsers();
            loadAvailableUsers();
        });
    }

    private void loadAllUsers() {
        allUsers.clear();

        currentUser = AppManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            System.err.println("Текущий пользователь не определен!");
            showError("Ошибка", "Текущий пользователь не определен!");
            return;
        }

        System.out.println("Текущий пользователь для создания чата: " + currentUser.getNick());

        // Загружаем пользователей из хранилища
        List<User> storedUsers = UserStorage.getAllUsers();

        if (storedUsers.isEmpty()) {
            System.out.println("📝 Нет сохраненных пользователей, создаем демо-пользователей");
            allUsers.add(new User("Анна", "password123", "Анна", "Иванова", "anna@example.com"));
            allUsers.add(new User("Борис", "password123", "Борис", "Петров", "boris@example.com"));
            allUsers.add(new User("Мария", "password123", "Мария", "Сидорова", "maria@example.com"));
            allUsers.add(new User("Иван", "password123", "Иван", "Смирнов", "ivan@example.com"));
            allUsers.add(new User("Ольга", "password123", "Ольга", "Кузнецова", "olga@example.com"));

            for (User user : allUsers) {
                UserStorage.saveUser(user);
            }
        } else {
            // Добавляем всех пользователей, кроме текущего
            for (User user : storedUsers) {
                if (!user.getNick().equals(currentUser.getNick())) {
                    allUsers.add(user);
                }
            }
        }

        System.out.println("✅ Загружено " + allUsers.size() + " пользователей для создания чата");

        for (User user : allUsers) {
            System.out.println("   👤 " + user.getNick() + " - " + user.getFullName());
        }
    }

    private void loadAvailableUsers() {
        usersListView.getItems().clear();
        usersListView.getItems().addAll(allUsers);
        selectedUsersListView.getItems().clear();
    }

    @FXML
    private void handleAddUser() {
        User selectedUser = usersListView.getSelectionModel().getSelectedItem();
        if (selectedUser != null && !selectedUsersListView.getItems().contains(selectedUser)) {
            selectedUsersListView.getItems().add(selectedUser);
            usersListView.getItems().remove(selectedUser);
            System.out.println("✅ Добавлен пользователь: " + selectedUser.getNick());
            updateStatus("Пользователь добавлен: " + selectedUser.getNick(), "green");
        }
    }

    @FXML
    private void handleRemoveUser() {
        User selectedUser = selectedUsersListView.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            selectedUsersListView.getItems().remove(selectedUser);
            if (!usersListView.getItems().contains(selectedUser)) {
                usersListView.getItems().add(selectedUser);
            }
            System.out.println("❌ Удален пользователь: " + selectedUser.getNick());
            updateStatus("Пользователь удален: " + selectedUser.getNick(), "orange");
        }
    }

    @FXML
    private void handleCreateChat() {
        String chatName = chatNameField.getText().trim();
        List<User> selectedUsers = new ArrayList<>(selectedUsersListView.getItems());

        if (chatName.isEmpty()) {
            showError("Ошибка", "Введите название чата");
            chatNameField.requestFocus();
            return;
        }

        if (selectedUsers.isEmpty()) {
            showError("Ошибка", "Выберите хотя бы одного участника");
            return;
        }

        if (currentUser == null) {
            showError("Ошибка", "Текущий пользователь не определен");
            return;
        }

        // Проверяем подключение к серверу
        Repository repository = AppManager.getInstance().getRepository();
        if (repository instanceof LocalRepository) {
            LocalRepository localRepo = (LocalRepository) repository;
            if (!localRepo.isConnectedToServer()) {
                int choice = showConfirmation("Предупреждение",
                        "Сервер не подключен. Чат будет создан только локально.\n" +
                                "Другие пользователи не увидят его до подключения к серверу.\n" +
                                "Продолжить?");

                if (choice != 0) {
                    return;
                }
            }
        }

        try {
            // Создаем список всех участников (текущий пользователь + выбранные)
            List<User> allChatUsers = new ArrayList<>(selectedUsers);
            allChatUsers.add(currentUser);

            System.out.println("\n🎯 СОЗДАНИЕ НОВОГО ЧАТА:");
            System.out.println("   Название: " + chatName);
            System.out.println("   Создатель: " + currentUser.getNick());
            System.out.println("   Всего участников: " + allChatUsers.size());
            System.out.println("   Участники:");
            for (User user : allChatUsers) {
                System.out.println("   👤 " + user.getNick() + " - " + user.getFullName());
            }

            // Создаем чат
            Chat newChat = new Chat(allChatUsers, chatName);

            // Добавляем приветственное сообщение
            Message welcomeMessage = new Message(currentUser,
                    "Чат \"" + chatName + "\" создан! Добро пожаловать!", new java.util.Date());
            newChat.send_message(welcomeMessage);

            System.out.println("✅ Чат создан: " + newChat.getChatName() + " (ID: " + newChat.getId() + ")");

            // Добавляем чат в репозиторий
            if (repository != null) {
                repository.add_chat(newChat);

                System.out.println("✅ Чат добавлен в репозиторий");
                updateStatus("Чат успешно создан!", "green");

                // Закрываем окно через 1.5 секунды
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        javafx.application.Platform.runLater(() -> {
                            closeWindow();
                            System.out.println("Переход к списку чатов...");
                            AppManager.getInstance().switchToChatList();
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

            } else {
                System.err.println("❌ Репозиторий не найден!");
                showError("Ошибка", "Репозиторий не инициализирован");
            }

        } catch (Exception e) {
            showError("Ошибка", "Не удалось создать чат: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        System.out.println("Отмена создания чата");
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private int showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType yesButton = new ButtonType("Да", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("Нет", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == yesButton) {
            return 0; // Да
        } else {
            return 1; // Нет
        }
    }

    private void updateStatus(String message, String color) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            if (color.equals("green")) {
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            } else if (color.equals("orange")) {
                statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
            } else {
                statusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
            }
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("Set current user in CreateChatController: " +
                (user != null ? user.getNick() : "null"));
        loadAllUsers();
        loadAvailableUsers();
    }
}