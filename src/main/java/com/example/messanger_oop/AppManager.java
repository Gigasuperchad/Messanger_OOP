package com.example.messanger_oop;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

public class AppManager {
    private static AppManager instance;
    private Stage stage;
    private Repository repository;
    private User currentUser;
    private Chat currentChat;
    private boolean appActive = true;
    private boolean wasFullScreen = false;
    private double windowWidth = 450;
    private double windowHeight = 650;
    private double windowX = -1;
    private double windowY = -1;
    private boolean maximized = false;

    private AppManager() {
        repository = new LocalRepository();
        ensureDirectories();

        // Инициализируем менеджер статусов
        StatusManager.getInstance();
    }

    private void ensureDirectories() {
        String[] dirs = {"local_chats", "users_data", "chat_files", "server_data"};
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

        // Отслеживаем изменения состояния окна
        this.stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            appActive = newVal;
            System.out.println("Приложение " + (appActive ? "активно" : "не активно"));
        });

        this.stage.iconifiedProperty().addListener((obs, oldVal, newVal) -> {
            appActive = !newVal;
            System.out.println("Приложение " + (appActive ? "развернуто" : "свернуто"));
        });

        // Сохраняем состояние полного экрана
        this.stage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
            wasFullScreen = newVal;
            System.out.println("Полный экран: " + (newVal ? "включен" : "выключен"));
        });

        // Сохраняем состояние максимизации
        this.stage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            maximized = newVal;
            System.out.println("Окно " + (newVal ? "максимизировано" : "восстановлено"));
        });

        // Сохраняем размеры окна при изменении
        this.stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isFullScreen() && !stage.isMaximized()) {
                windowWidth = newVal.doubleValue();
            }
        });

        this.stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isFullScreen() && !stage.isMaximized()) {
                windowHeight = newVal.doubleValue();
            }
        });

        // Сохраняем позицию окна
        this.stage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isFullScreen() && !stage.isMaximized()) {
                windowX = newVal.doubleValue();
            }
        });

        this.stage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isFullScreen() && !stage.isMaximized()) {
                windowY = newVal.doubleValue();
            }
        });

        loadLoginScene();
    }

    public void loadLoginScene() {
        try {
            saveWindowState();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login_Scene.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, windowWidth, windowHeight);
            stage.setScene(scene);

            restoreWindowState();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            loadRegistrationScene();
        }
    }

    public void loadRegistrationScene() {
        try {
            saveWindowState();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Registration_Scene.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, windowWidth, windowHeight);
            stage.setScene(scene);

            restoreWindowState();
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

        // Устанавливаем статус онлайн
        StatusManager.getInstance().setUserOnline(user.getNick());

        if (repository instanceof LocalRepository) {
            LocalRepository localRepo = (LocalRepository) repository;
            localRepo.setCurrentUser(user);

            // Принудительно запрашиваем список чатов с сервера
            if (localRepo.isConnectedToServer()) {
                System.out.println("Запрашиваем список чатов с сервера...");
                // Запрос списка чатов будет выполнен в setCurrentUser
            } else {
                System.out.println("Работаем в автономном режиме");
            }

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

        this.currentChat = chat;

        try {
            saveWindowState();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Chat_Scene.fxml"));
            Parent root = loader.load();

            ChatController controller = loader.getController();
            if (controller != null) {
                controller.setRepository(repository);
                controller.setChat(chat);
                controller.setCurrentUser(currentUser);
            }

            Scene scene = new Scene(root, windowWidth, windowHeight);
            stage.setScene(scene);
            stage.setTitle("Чат: " + chat.getChatName());

            restoreWindowState();
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

        this.currentChat = null;

        try {
            saveWindowState();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Chat_List_Scene.fxml"));
            Parent root = loader.load();

            HelloController controller = loader.getController();
            if (controller != null) {
                controller.setRepository(repository);
            }

            Scene scene = new Scene(root, windowWidth, windowHeight);
            stage.setScene(scene);
            stage.setTitle("Мои чаты - " + (currentUser != null ? currentUser.getFullName() : "Неизвестный"));

            restoreWindowState();
            stage.show();

            // Обновляем список чатов
            if (repository instanceof LocalRepository) {
                ((LocalRepository) repository).printChatsInfo();
            }

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

    public void openStatusWindow() {
        System.out.println("\nОТКРЫТИЕ ОКНА СТАТУСА");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StatusWindow.fxml"));
            Parent root = loader.load();

            // Устанавливаем текущего пользователя в контроллер
            StatusController controller = loader.getController();
            if (controller != null) {
                // Контроллер сам загрузит пользователя в методе initialize()
            }

            Stage statusStage = new Stage();
            statusStage.setTitle("Настройка статуса");
            statusStage.setScene(new Scene(root, 500, 450)); // Изменили размер
            statusStage.initOwner(stage); // Делаем окно модальным к главному
            statusStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            statusStage.show();

        } catch (Exception e) {
            System.err.println("Ошибка открытия окна статуса: " + e.getMessage());
            e.printStackTrace();

            // Показать альтернативное уведомление
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText("Не удалось открыть окно статуса");
                alert.setContentText("Файл StatusWindow.fxml не найден или поврежден.");
                alert.showAndWait();
            });
        }
    }

    public void switchToProfileScene() {
        try {
            saveWindowState();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Profile_Scene.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, windowWidth, windowHeight);
            stage.setScene(scene);
            stage.setTitle("Мой профиль - " + currentUser.getFullName());

            restoreWindowState();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Ошибка загрузки профиля: " + e.getMessage());
        }
    }

    public void switchToServerMessenger() {
        try {
            saveWindowState();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Messenger.fxml"));
            Parent root = loader.load();

            MessengerController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
            }

            Scene scene = new Scene(root, windowWidth, windowHeight);
            stage.setScene(scene);
            stage.setTitle("Серверный мессенджер - " + currentUser.getFullName());

            restoreWindowState();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Ошибка загрузки серверного мессенджера: " + e.getMessage());
        }
    }

    public void logout() {
        System.out.println("\nВЫХОД ИЗ СИСТЕМЫ...");

        saveWindowState();

        // Устанавливаем статус оффлайн
        if (currentUser != null) {
            StatusManager.getInstance().setUserOffline(currentUser.getNick());
        }

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
        currentChat = null;

        repository = new LocalRepository();

        loadLoginScene();
    }

    public void exitApplication() {
        // Устанавливаем статус оффлайн перед выходом
        if (currentUser != null) {
            StatusManager.getInstance().setUserOffline(currentUser.getNick());
        }

        logout();
        if (stage != null) {
            stage.close();
        }
    }

    private void saveWindowState() {
        if (stage != null) {
            wasFullScreen = stage.isFullScreen();
            maximized = stage.isMaximized();

            if (!stage.isFullScreen() && !stage.isMaximized()) {
                windowWidth = stage.getWidth();
                windowHeight = stage.getHeight();
                windowX = stage.getX();
                windowY = stage.getY();
            }

            System.out.println("Сохранено состояние окна:");
            System.out.println("  Полный экран: " + wasFullScreen);
            System.out.println("  Максимизировано: " + maximized);
            System.out.println("  Размер: " + windowWidth + "x" + windowHeight);
            System.out.println("  Позиция: " + windowX + ", " + windowY);
        }
    }

    private void restoreWindowState() {
        if (stage != null) {
            if (windowX >= 0 && windowY >= 0) {
                stage.setX(windowX);
                stage.setY(windowY);
            }

            stage.setWidth(windowWidth);
            stage.setHeight(windowHeight);

            stage.setMaximized(maximized);

            if (wasFullScreen) {
                stage.setFullScreen(true);
            }

            System.out.println("Восстановлено состояние окна:");
            System.out.println("  Полный экран: " + wasFullScreen);
            System.out.println("  Максимизировано: " + maximized);
            System.out.println("  Размер: " + windowWidth + "x" + windowHeight);
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

    public Chat getCurrentChat() {
        return currentChat;
    }

    public boolean isAppActive() {
        return appActive;
    }

    public void showNotification(String title, String message) {
        if (!appActive) {
            System.out.println("\nСИСТЕМНОЕ УВЕДОМЛЕНИЕ:");
            System.out.println("   Заголовок: " + title);
            System.out.println("   Сообщение: " + message);

            javafx.application.Platform.runLater(() -> {
                try {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle(title);
                    alert.setHeaderText("Новое сообщение");
                    alert.setContentText(message);

                    alert.show();

                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    javafx.application.Platform.runLater(() -> alert.close());
                                }
                            },
                            3000
                    );
                } catch (Exception e) {
                    System.err.println("Ошибка показа уведомления: " + e.getMessage());
                }
            });
        }
    }
}