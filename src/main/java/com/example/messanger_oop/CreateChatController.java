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
            return;
        }

        System.out.println("Текущий пользователь для создания чата: " + currentUser.getNick());

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
            allUsers.addAll(storedUsers);
        }

        allUsers.removeIf(user -> user.getNick().equals(currentUser.getNick()));

        System.out.println("Загружено " + allUsers.size() + " пользователей для создания чата");

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
            System.out.println("Добавлен пользователь: " + selectedUser.getNick());
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
            System.out.println("Удален пользователь: " + selectedUser.getNick());
        }
    }

    @FXML
    private void handleCreateChat() {
        String chatName = chatNameField.getText().trim();
        List<User> selectedUsers = new ArrayList<>(selectedUsersListView.getItems());

        if (chatName.isEmpty()) {
            showAlert("Ошибка", "Введите название чата");
            return;
        }

        if (selectedUsers.isEmpty()) {
            showAlert("Ошибка", "Выберите хотя бы одного участника");
            return;
        }

        if (currentUser == null) {
            showAlert("Ошибка", "Текущий пользователь не определен");
            return;
        }

        try {
            // Создаем список всех участников
            List<User> allChatUsers = new ArrayList<>(selectedUsers);
            allChatUsers.add(currentUser);

            System.out.println("\nСОЗДАНИЕ НОВОГО ЧАТА:");
            System.out.println("   Название: " + chatName);
            System.out.println("   Текущий пользователь: " + currentUser.getNick());
            System.out.println("   Всего участников: " + allChatUsers.size());
            for (User user : allChatUsers) {
                System.out.println("   👤 " + user.getNick());
            }

            Chat newChat = new Chat(allChatUsers, chatName);

            // Создаем приветственное сообщение
            Message welcomeMessage = new Message(currentUser,
                    "Чат \"" + chatName + "\" создан! Добро пожаловать!", new java.util.Date());
            newChat.send_message(welcomeMessage);

            System.out.println("Чат создан: " + newChat.getChatName() + " (ID: " + newChat.getId() + ")");

            Repository repository = AppManager.getInstance().getRepository();
            if (repository != null) {
                repository.add_chat(newChat);

                System.out.println("Чат добавлен в репозиторий");

                closeWindow();

                // Небольшая задержка перед обновлением списка чатов
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            System.out.println("Переход к списку чатов...");
                            AppManager.getInstance().switchToChatList();
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.err.println("Репозиторий не найден!");
                showAlert("Ошибка", "Репозиторий не инициализирован");
            }

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось создать чат: " + e.getMessage());
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("Set current user in CreateChatController: " +
                (user != null ? user.getNick() : "null"));
        loadAllUsers();
        loadAvailableUsers();
    }
}