package com.example.messanger_oop;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class AppManager {
    private static AppManager instance;
    private Stage stage;
    private Repository repository;
    private User currentUser;

    private AppManager() {
        repository = new LocalRepository();
        ensureDirectories();
    }

    private void ensureDirectories() {
        String[] dirs = {"local_chats", "users_data"};
        for (String dir : dirs) {
            java.io.File directory = new java.io.File(dir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Создана директория: " + dir);
                }
            }
        }
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

        loadLoginScene();
    }

    public void loadLoginScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login_Scene.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 450, 650);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            loadRegistrationScene();
        }
    }

    public void loadRegistrationScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Registration_Scene.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 500, 700);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Ошибка загрузки сцены регистрации: " + e.getMessage());
        }
    }

    public void loadMessengerScene(User user) {
        System.out.println("\nЗАГРУЗКА СЦЕНЫ МЕССЕНДЖЕРА ДЛЯ ПОЛЬЗОВАТЕЛЯ:");
        System.out.println("   👤 Пользователь: " + user.getNick());

        this.currentUser = user;
        UserStorage.saveUser(user);

        if (repository instanceof LocalRepository) {
            LocalRepository localRepo = (LocalRepository) repository;
            localRepo.setCurrentUser(user);

            localRepo.printChatsInfo();
        }

        try {
            switchToChatList();
        } catch (Exception e) {
            e.printStackTrace();
            loadLoginScene();
        }
    }

    public void switchToChatScene(Chat chat) {
        System.out.println("\nПЕРЕКЛЮЧЕНИЕ НА ЧАТ:");
        System.out.println("   Чат: " + chat.getChatName());
        System.out.println("   Сообщений: " + chat.get_message_count());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Chat_Scene.fxml"));
            Parent root = loader.load();

            ChatController controller = loader.getController();
            if (controller != null) {
                controller.setRepository(repository);
                controller.setChat(chat);
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

    public void switchToChatList() {
        System.out.println("\nПЕРЕКЛЮЧЕНИЕ НА СПИСОК ЧАТОВ");
        System.out.println("   Текущий пользователь: " +
                (currentUser != null ? currentUser.getNick() : "null"));

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Chat_List_Scene.fxml"));
            Parent root = loader.load();

            HelloController controller = loader.getController();
            if (controller != null) {
                controller.setRepository(repository);
            }

            stage.setScene(new Scene(root, 350, 500));
            stage.setTitle("Мои чаты - " + (currentUser != null ? currentUser.getFullName() : "Неизвестный"));
            stage.show();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки Chat_List_Scene.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void openCreateChatWindow() {
        System.out.println("\nОТКРЫТИЕ ОКНА СОЗДАНИЯ ЧАТА");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Create_Chat_Scene.fxml"));
            Parent root = loader.load();

            CreateChatController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
            }

            Stage createChatStage = new Stage();
            createChatStage.setTitle("Создание нового чата");
            createChatStage.setScene(new Scene(root, 700, 550));
            createChatStage.show();

        } catch (Exception e) {
            System.err.println("Ошибка открытия окна создания чата: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    public void logout() {
        System.out.println("\nВЫХОД ИЗ СИСТЕМЫ...");

        // Отправляем команду сохранения на сервер
        if (repository instanceof LocalRepository) {
            LocalRepository localRepo = (LocalRepository) repository;
            if (localRepo.isConnectedToServer()) {
                try {
                    localRepo.disconnect();
                    System.out.println("Данные отправлены на сервер для сохранения");
                } catch (Exception e) {
                    System.err.println("Ошибка отправки данных на сервер: " + e.getMessage());
                }
            }
        }

        UserStorage.clearCurrentUser();
        currentUser = null;

        repository = new LocalRepository();

        loadLoginScene();
    }

    public void exitApplication() {
        logout();
        if (stage != null) {
            stage.close();
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