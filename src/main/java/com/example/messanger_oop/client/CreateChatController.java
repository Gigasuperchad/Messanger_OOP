package com.example.messanger_oop.client;

import com.example.messanger_oop.shared.Chat;
import com.example.messanger_oop.shared.Message;
import com.example.messanger_oop.shared.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

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
        System.out.println("\n=== НАЧАЛО СОЗДАНИЯ ЧАТА ===");

        String chatName = chatNameField.getText().trim();
        List<User> selectedUsers = new ArrayList<>(selectedUsersListView.getItems());

        // Валидация ввода
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

        System.out.println("Проверка данных:");
        System.out.println("  Название чата: " + chatName);
        System.out.println("  Создатель: " + currentUser.getNick());
        System.out.println("  Выбрано участников: " + selectedUsers.size());

        // Получаем репозиторий
        Repository repository = AppManager.getInstance().getRepository();
        if (!(repository instanceof LocalRepository)) {
            showError("Ошибка", "Неподдерживаемый тип репозитория");
            return;
        }

        LocalRepository localRepo = (LocalRepository) repository;

        // Проверяем статус подключения
        System.out.println("\nПроверка статуса подключения:");

        // Проверяем TCP-соединение
        boolean hasNetworkConnection = localRepo.isConnectedToServer();
        System.out.println("  TCP подключение к серверу: " + (hasNetworkConnection ? "✅ ЕСТЬ" : "❌ НЕТ"));

        // Проверяем авторизацию пользователя
        boolean isUserAuthenticated = currentUser != null;
        System.out.println("  Пользователь определен: " + (isUserAuthenticated ? "✅ ДА" : "❌ НЕТ"));

        // Определяем статус подключения
        boolean canSendToServer = hasNetworkConnection && isUserAuthenticated;

        if (!hasNetworkConnection) {
            // Нет TCP-соединения
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Нет подключения к серверу");
            alert.setHeaderText("Сервер недоступен");
            alert.setContentText("Чат будет создан только локально.\n" +
                    "Другие пользователи не увидят его до подключения к серверу.\n\n" +
                    "Хотите продолжить создание локального чата?");

            ButtonType localButton = new ButtonType("Создать локально", ButtonBar.ButtonData.YES);
            ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(localButton, cancelButton);

            java.util.Optional<ButtonType> result = alert.showAndWait();
            if (!result.isPresent() || result.get() != localButton) {
                System.out.println("Пользователь отменил создание чата");
                return;
            }

            System.out.println("Пользователь выбрал создание локального чата");
            createLocalChat(chatName, selectedUsers);

        } else if (!isUserAuthenticated) {
            // Есть TCP-соединение, но пользователь не авторизован
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Требуется авторизация");
            alert.setHeaderText("Вы не авторизованы на сервере");
            alert.setContentText("У вас есть подключение к серверу, но требуется авторизация.\n\n" +
                    "Выберите вариант:\n" +
                    "1. Создать чат локально\n" +
                    "2. Создать чат на сервере (потребует авторизации)");

            ButtonType localButton = new ButtonType("Только локально");
            ButtonType serverButton = new ButtonType("На сервере");
            ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(serverButton, localButton, cancelButton);

            java.util.Optional<ButtonType> result = alert.showAndWait();
            if (!result.isPresent()) {
                return;
            }

            if (result.get() == serverButton) {
                System.out.println("Попытка авторизации и создания чата на сервере...");
                createServerChatWithAuth(chatName, selectedUsers, localRepo);
            } else if (result.get() == localButton) {
                System.out.println("Создание чата только локально...");
                createLocalChat(chatName, selectedUsers);
            }

        } else {
            // Полное подключение - можно создавать на сервере
            System.out.println("Полное подключение - создаем чат на сервере");
            createServerChat(chatName, selectedUsers, localRepo);
        }
    }

    private void createLocalChat(String chatName, List<User> selectedUsers) {
        try {
            // Создаем список всех участников (текущий пользователь + выбранные)
            List<User> allChatUsers = new ArrayList<>(selectedUsers);
            allChatUsers.add(currentUser);

            System.out.println("\n🎯 СОЗДАНИЕ ЛОКАЛЬНОГО ЧАТА:");
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
            System.out.println("💾 Чат сохранен локально");

            // Добавляем чат в репозиторий
            Repository repository = AppManager.getInstance().getRepository();
            if (repository != null) {
                repository.add_chat(newChat);

                updateStatus("✅ Чат успешно создан локально!", "green");

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

    private void createServerChat(String chatName, List<User> selectedUsers, LocalRepository localRepo) {
        try {
            // Создаем список всех участников (текущий пользователь + выбранные)
            List<User> allChatUsers = new ArrayList<>(selectedUsers);
            allChatUsers.add(currentUser);

            System.out.println("\n🎯 СОЗДАНИЕ ЧАТА НА СЕРВЕРЕ:");
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
            localRepo.add_chat(newChat);

            updateStatus("✅ Чат успешно создан на сервере!", "green");

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

        } catch (Exception e) {
            showError("Ошибка", "Не удалось создать чат на сервере: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createServerChatWithAuth(String chatName, List<User> selectedUsers, LocalRepository localRepo) {
        try {
            // Пытаемся авторизоваться
            System.out.println("🔄 Попытка авторизации на сервере...");

            // Здесь нужно вызвать метод авторизации в ClientConnection
            // localRepo.getClientConnection().authenticate(currentUser.getNick(), currentUser.getPassword());

            // Для демонстрации - покажем сообщение
            Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
            infoAlert.setTitle("Авторизация");
            infoAlert.setHeaderText("Функционал авторизации для создания чата");
            infoAlert.setContentText("В данной версии приложения авторизация для создания чата на сервере\n" +
                    "требует дополнительной реализации.\n\n" +
                    "Чат будет создан локально.");
            infoAlert.showAndWait();

            // Создаем локальный чат вместо серверного
            createLocalChat(chatName, selectedUsers);

        } catch (Exception e) {
            showError("Ошибка", "Не удалось выполнить авторизацию: " + e.getMessage());
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