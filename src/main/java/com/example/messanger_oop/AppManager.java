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
    private FXMLLoader loader;
    private User currentUser;

    private AppManager() {
        repository = new LocalRepository();
        currentUser = new User("ТекущийПользователь");
    }

    public static AppManager getInstance() {
        if (instance == null) {
            instance = new AppManager();
        }
        return instance;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setResizable(false);
        this.stage.setTitle("Мессенджер");

        // Создаем тестовые данные после установки stage
        initializeTestData();

        // Запускаем со списка чатов
        switchToChatList();
    }

    private void initializeTestData() {
        try {
            // Тестовые пользователи
            User user1 = new User("Анна");
            User user2 = new User("Борис");
            User user3 = new User("Мария");

            // Создаем чат 1
            List<User> chat1Users = new ArrayList<>();
            chat1Users.add(currentUser);
            chat1Users.add(user1);
            Chat privateChat = new Chat(chat1Users);

            // Создаем чат 2
            List<User> chat2Users = new ArrayList<>();
            chat2Users.add(currentUser);
            chat2Users.add(user2);
            chat2Users.add(user3);
            Chat groupChat = new Chat(chat2Users);

            // Добавляем чаты в репозиторий
            repository.add_chat(privateChat);
            repository.add_chat(groupChat);

            // Тестовые сообщения для чата 1
            privateChat.send_message(new Message(currentUser, "Привет, Анна!", new java.util.Date()));
            privateChat.send_message(new Message(user1, "Привет! Как дела?", new java.util.Date()));

            // Тестовые сообщения для чата 2
            groupChat.send_message(new Message(currentUser, "Всем привет!", new java.util.Date()));
            groupChat.send_message(new Message(user2, "Привет!", new java.util.Date()));

        } catch (Exception e) {
            System.err.println("Ошибка инициализации тестовых данных: " + e.getMessage());
        }
    }

    // Метод для перехода в окно чата
    public void switchToChatScene(Chat chat) {
        try {
            loader = new FXMLLoader(getClass().getResource("Chat_Scene.fxml"));
            Parent root = loader.load();

            ChatController controller = loader.getController();
            if (controller != null) {
                controller.setRepository(repository);
                controller.setChat(chat);
            }

            stage.setScene(new Scene(root, 400, 500));
            stage.show();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки Chat_Scene.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод для перехода к списку чатов
    public void switchToChatList() {
        try {
            loader = new FXMLLoader(getClass().getResource("Chat_List_Scene.fxml"));
            Parent root = loader.load();

            HelloController controller = loader.getController();
            if (controller != null) {
                controller.setRepository(repository);
            }

            stage.setScene(new Scene(root, 300, 500));
            stage.show();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки Chat_List_Scene.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}