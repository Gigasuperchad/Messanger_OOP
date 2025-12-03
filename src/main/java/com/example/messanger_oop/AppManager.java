package com.example.messanger_oop;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppManager {
    private static AppManager instance;
    private Stage stage;
    private Repository repository;
    private User currentUser;

    private AppManager() {
        repository = new LocalRepository();
    }

    public static AppManager getInstance() {
        if (instance == null) {
            instance = new AppManager();
        }
        return instance;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setTitle("Мессенджер");

        // Пробуем загрузить последнего зарегистрированного пользователя
        loadCurrentUserFromStorage();

        if (currentUser != null) {
            // Если есть сохраненный пользователь, идем сразу в мессенджер
            loadMessengerScene(currentUser);
        } else {
            // Если нет, показываем регистрацию
            loadRegistrationScene();
        }
    }

    private void loadCurrentUserFromStorage() {
        currentUser = UserStorage.getCurrentUser();
        if (currentUser != null) {
            System.out.println("Загружен пользователь: " + currentUser.getFullName());
        }
    }

    public void loadRegistrationScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login_Scene.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 450, 650);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadMessengerScene(User user) {
        this.currentUser = user;

        // Сохраняем пользователя
        UserStorage.saveUser(user);

        // Инициализируем тестовые данные для чатов
        initializeTestData();

        try {
            // Переходим к списку чатов
            switchToChatList();
        } catch (Exception e) {
            e.printStackTrace();
            // В случае ошибки возвращаемся к регистрации
            loadRegistrationScene();
        }
    }

    private void initializeTestData() {
        try {
            // Проверяем, есть ли уже чаты в репозитории
            if (repository.getChats().isEmpty()) {
                System.out.println("Создаем тестовые чаты...");

                // Создаем тестовых пользователей с полными данными
                User user1 = new User("Анна", "password123", "Анна", "Иванова", "anna@example.com");
                User user2 = new User("Борис", "password123", "Борис", "Петров", "boris@example.com");
                User user3 = new User("Мария", "password123", "Мария", "Сидорова", "maria@example.com");

                // Создаем чат 1 (приватный)
                List<User> chat1Users = new ArrayList<>();
                chat1Users.add(currentUser);
                chat1Users.add(user1);
                Chat privateChat = new Chat(chat1Users, "Приватный чат с Анной");

                // Создаем чат 2 (групповой)
                List<User> chat2Users = new ArrayList<>();
                chat2Users.add(currentUser);
                chat2Users.add(user2);
                chat2Users.add(user3);
                Chat groupChat = new Chat(chat2Users, "Рабочая группа");

                // Добавляем чаты в репозиторий
                repository.add_chat(privateChat);
                repository.add_chat(groupChat);

                // Тестовые сообщения для чата 1
                if (currentUser != null) {
                    privateChat.send_message(new Message(currentUser, "Привет, Анна! Как дела?", new java.util.Date()));
                }
                if (user1 != null) {
                    privateChat.send_message(new Message(user1, "Привет! Все отлично, спасибо! А у тебя?", new java.util.Date()));
                }

                // Тестовые сообщения для чата 2
                if (currentUser != null) {
                    groupChat.send_message(new Message(currentUser, "Всем добрый день! Начинаем собрание.", new java.util.Date()));
                }
                if (user2 != null) {
                    groupChat.send_message(new Message(user2, "Приветствую! Я готов.", new java.util.Date()));
                }

                System.out.println("Создано " + repository.getChats().size() + " тестовых чата");
            } else {
                System.out.println("Чаты уже загружены: " + repository.getChats().size() + " чатов");
            }
        } catch (Exception e) {
            System.err.println("Ошибка инициализации тестовых данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод для перехода в окно чата
    public void switchToChatScene(Chat chat) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Chat_Scene.fxml"));
            Parent root = loader.load();

            ChatController controller = loader.getController();
            if (controller != null) {
                controller.setRepository(repository);
                controller.setChat(chat);
                // ВАЖНО: передаем текущего пользователя в контроллер чата
                controller.setCurrentUser(currentUser);
            }

            stage.setScene(new Scene(root, 500, 600));
            stage.setTitle("Чат: " + chat.getChatName());
            stage.show();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки Chat_Scene.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод для перехода к списку чатов
    public void switchToChatList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Chat_List_Scene.fxml"));
            Parent root = loader.load();

            HelloController controller = loader.getController();
            if (controller != null) {
                controller.setRepository(repository);
            }

            stage.setScene(new Scene(root, 350, 500));
            stage.setTitle("Мои чаты - " + currentUser.getFullName());
            stage.show();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки Chat_List_Scene.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод для перехода к профилю
    public void switchToProfileScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Profile_Scene.fxml"));
            Parent root = loader.load();

            stage.setScene(new Scene(root, 400, 450));
            stage.setTitle("Мой профиль - " + currentUser.getFullName());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Ошибка загрузки профиля: " + e.getMessage());
        }
    }

    // Метод для перехода к серверному мессенджеру (если нужен)
    public void switchToServerMessenger() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Messenger.fxml"));
            Parent root = loader.load();

            MessengerController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
            }

            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Серверный мессенджер - " + currentUser.getFullName());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Ошибка загрузки серверного мессенджера: " + e.getMessage());
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Repository getRepository() {
        return repository;
    }

    public Stage getStage() {
        return stage;
    }
}