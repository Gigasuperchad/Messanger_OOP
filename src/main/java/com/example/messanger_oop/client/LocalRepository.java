package com.example.messanger_oop.client;

import com.example.messanger_oop.server.StatusManager;
import com.example.messanger_oop.shared.UserStatus;
import com.example.messanger_oop.shared.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.util.*;

public class LocalRepository implements Repository, ClientConnection.MessageListener {
    private ObservableList<Message> Messages = FXCollections.observableArrayList();
    private ObservableList<Chat> Chats = FXCollections.observableArrayList();
    private User currentUser;
    private ClientConnection clientConnection;
    private Map<Integer, Chat> localChatsCache = new HashMap<>();
    private boolean chatsLoaded = false;

    public LocalRepository() {
        System.out.println("LocalRepository initialized");
        this.clientConnection = new ClientConnection(this);

        // Попытка подключения к серверу
        new Thread(() -> {
            boolean connected = clientConnection.connect(
                    clientConnection.getServerAddress(),
                    clientConnection.getServerPort(),
                    5000 // таймаут 5 секунд
            );
            System.out.println("Подключение к серверу: " + (connected ? "успешно" : "ошибка"));

            if (connected) {
                System.out.println("✅ Подключено к серверу");

                // ДОБАВЬТЕ ЭТО: синхронизируем пользователей после подключения
                new Thread(() -> {
                    try {
                        Thread.sleep(2000); // Ждем стабилизации соединения
                        syncUsersToServer();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                System.out.println("❌ Не удалось подключиться");
                createTestChatsIfNeeded();
            }
        }).start();
    }
    // Реализация методов интерфейса MessageListener
    @Override
    public void onMessageReceived(String message) {
        System.out.println("Получено от сервера: " + message);

        if (message == null || message.isEmpty()) return;

        // Обработка различных типов ответов
        if (message.startsWith(ProtocolConstants.RESP_CHAT_LIST)) {
            processChatList(message);
        } else if (message.startsWith(ProtocolConstants.RESP_NEW_MESSAGE)) {
            processNewMessage(message);
        } else if (message.startsWith(ProtocolConstants.RESP_CHAT_CREATED)) {
            processChatCreated(message);
        } else if (message.startsWith(ProtocolConstants.RESP_CHAT_DELETED)) {
            processChatDeleted(message);
        } else if (message.startsWith(ProtocolConstants.RESP_STATUS_UPDATE)) {
            processStatusUpdate(message);
        } else if (message.startsWith(ProtocolConstants.RESP_ONLINE_USERS)) {
            processOnlineUsers(message);
        }
    }

    @Override
    public void onConnectionStatusChanged(ClientConnection.ConnectionState state) {
        System.out.println("Статус соединения изменился: " + state.toString());

        switch (state) {
            case DISCONNECTED:
                System.out.println("❌ Соединение разорвано");
                break;
            case CONNECTING:
                System.out.println("🔄 Подключение...");
                break;
            case SOCKET_CONNECTED:
                System.out.println("✅ TCP-соединение установлено");
                System.out.println("➡️ Требуется авторизация");
                break;
            case AUTHENTICATING:
                System.out.println("🔐 Идет авторизация...");
                break;
            case AUTHENTICATED:
                System.out.println("✅ Авторизация успешна");
                if (currentUser != null) {
                    System.out.println("👤 Пользователь: " + currentUser.getNick());
                }
                break;
        }
    }

    @Override
    public void onError(String errorMessage) {
        System.err.println("❌ Ошибка соединения: " + errorMessage);
    }

    @Override
    public void onAuthResult(boolean success, String message) {
        System.out.println("Результат авторизации: " + (success ? "✅ успех" : "❌ ошибка") + " - " + message);

        if (success && currentUser != null) {
            // После успешной авторизации запрашиваем чаты
            clientConnection.requestChats();
        }
    }

    // Остальные методы остаются без изменений
    private void processChatList(String message) {
        try {
            // Формат: CHAT_LIST;id:name:messageCount:participantCount;...
            String[] parts = message.split("\\" + ProtocolConstants.DELIMITER, 2);
            if (parts.length < 2) return;

            String chatListStr = parts[1];
            if (chatListStr.isEmpty()) {
                System.out.println("Нет чатов от сервера");
                return;
            }

            String[] chatEntries = chatListStr.split(ProtocolConstants.LIST_DELIMITER);
            List<Chat> serverChats = new ArrayList<>();

            for (String entry : chatEntries) {
                if (entry.isEmpty()) continue;

                String[] fields = entry.split(ProtocolConstants.FIELD_DELIMITER);
                if (fields.length >= 4) {
                    try {
                        int id = Integer.parseInt(fields[0]);
                        String name = fields[1];
                        int messageCount = Integer.parseInt(fields[2]);
                        int participantCount = Integer.parseInt(fields[3]);

                        // Создаем временный объект чата
                        Chat chat = new Chat(new ArrayList<>(), name);
                        chat.setId(id);
                        serverChats.add(chat);

                        System.out.println("Добавлен чат от сервера: " + name + " (ID: " + id + ")");
                    } catch (NumberFormatException e) {
                        System.err.println("Ошибка парсинга чата: " + entry);
                    }
                }
            }

            // Обновляем список чатов в UI потоке
            javafx.application.Platform.runLater(() -> {
                Chats.clear();
                Chats.addAll(serverChats);
                chatsLoaded = true;
                System.out.println("Список чатов обновлен: " + Chats.size() + " чатов");
            });

            // Сохраняем локально
            saveChatsLocally(serverChats);

        } catch (Exception e) {
            System.err.println("Ошибка обработки списка чатов: " + e.getMessage());
        }
    }

    private void processNewMessage(String message) {
        try {
            // Формат: NEW_MESSAGE|chatId:sender:message:timestamp
            String[] parts = message.split("\\" + ProtocolConstants.DELIMITER, 2);
            if (parts.length < 2) return;

            String[] fields = parts[1].split(ProtocolConstants.FIELD_DELIMITER);
            if (fields.length >= 4) {
                int chatId = Integer.parseInt(fields[0]);
                String sender = fields[1];
                String content = fields[2];
                long timestamp = Long.parseLong(fields[3]);

                Chat chat = getChatById(chatId);
                if (chat != null) {
                    User senderUser = new User(sender);
                    Message newMessage = new Message(senderUser, content, new Date(timestamp));
                    chat.send_message(newMessage);

                    // Обновляем UI
                    javafx.application.Platform.runLater(() -> {
                        if (!Chats.contains(chat)) {
                            Chats.add(chat);
                        }
                        // Можно обновить конкретный чат здесь
                    });

                    System.out.println("Новое сообщение в чате " + chat.getChatName() + " от " + sender);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки нового сообщения: " + e.getMessage());
        }
    }

    private void processChatCreated(String message) {
        try {
            // Формат: CHAT_CREATED|chatId:chatName
            String[] parts = message.split("\\" + ProtocolConstants.DELIMITER, 2);
            if (parts.length < 2) return;

            String[] fields = parts[1].split(ProtocolConstants.FIELD_DELIMITER);
            if (fields.length >= 2) {
                int chatId = Integer.parseInt(fields[0]);
                String chatName = fields[1];

                System.out.println("Чат создан на сервере: " + chatName + " (ID: " + chatId + ")");

                // Обновляем список чатов
                clientConnection.requestChats();
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки создания чата: " + e.getMessage());
        }
    }

    private void processChatDeleted(String message) {
        try {
            // Формат: CHAT_DELETED|chatId:chatName
            String[] parts = message.split("\\" + ProtocolConstants.DELIMITER, 2);
            if (parts.length < 2) return;

            String[] fields = parts[1].split(ProtocolConstants.FIELD_DELIMITER);
            if (fields.length >= 2) {
                int chatId = Integer.parseInt(fields[0]);
                String chatName = fields[1];

                // Удаляем чат из локального кэша
                javafx.application.Platform.runLater(() -> {
                    Chats.removeIf(chat -> chat.getId() == chatId);
                    System.out.println("Чат удален: " + chatName);
                });
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки удаления чата: " + e.getMessage());
        }
    }

    private void processStatusUpdate(String message) {
        try {
            // Формат: STATUS_UPDATE|username:status
            String[] parts = message.split("\\" + ProtocolConstants.DELIMITER, 2);
            if (parts.length < 2) return;

            String[] fields = parts[1].split(ProtocolConstants.FIELD_DELIMITER);
            if (fields.length >= 2) {
                String username = fields[0];
                String status = fields[1];

                // Обновляем статус в StatusManager
                UserStatus.Status statusEnum;
                switch (status) {
                    case ProtocolConstants.USER_STATUS_ONLINE:
                        statusEnum = UserStatus.Status.ONLINE;
                        break;
                    case ProtocolConstants.USER_STATUS_OFFLINE:
                        statusEnum = UserStatus.Status.OFFLINE;
                        break;
                    case ProtocolConstants.USER_STATUS_AWAY:
                        statusEnum = UserStatus.Status.AWAY;
                        break;
                    case ProtocolConstants.USER_STATUS_DND:
                        statusEnum = UserStatus.Status.DO_NOT_DISTURB;
                        break;
                    case ProtocolConstants.USER_STATUS_INVISIBLE:
                        statusEnum = UserStatus.Status.INVISIBLE;
                        break;
                    default:
                        statusEnum = UserStatus.Status.OFFLINE;
                }

                StatusManager.getInstance().setUserStatus(username, statusEnum);
                System.out.println("Статус обновлен: " + username + " -> " + status);
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки обновления статуса: " + e.getMessage());
        }
    }

    private void processOnlineUsers(String message) {
        System.out.println("Список онлайн пользователей получен");
    }

    private Chat getChatById(int chatId) {
        for (Chat chat : Chats) {
            if (chat.getId() == chatId) {
                return chat;
            }
        }
        return null;
    }

    private void saveChatsLocally(List<Chat> chats) {
        try {
            File chatsDir = new File("local_chats");
            if (!chatsDir.exists()) {
                chatsDir.mkdirs();
            }

            for (Chat chat : chats) {
                saveChatLocally(chat);
            }

            if (currentUser != null) {
                List<Integer> chatIds = new ArrayList<>();
                for (Chat chat : chats) {
                    chatIds.add(chat.getId());
                }

                String userChatsFile = "local_chats/" + currentUser.getNick() + "_chats.dat";
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        new FileOutputStream(userChatsFile))) {
                    oos.writeObject(chatIds);
                    System.out.println("Сохранено " + chatIds.size() + " чатов для пользователя " + currentUser.getNick());
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка сохранения чатов: " + e.getMessage());
        }
    }

    private void saveChatLocally(Chat chat) {
        try {
            File chatsDir = new File("local_chats");
            if (!chatsDir.exists()) {
                chatsDir.mkdirs();
            }

            String filename = "local_chats/chat_" + chat.getId() + ".dat";
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(chat);
                System.out.println("Чат сохранен локально: " + filename);
            }
        } catch (IOException e) {
            System.err.println("Ошибка сохранения чата: " + e.getMessage());
        }
    }

    private void loadLocalChats() {
        if (chatsLoaded) {
            System.out.println("Чаты уже загружены, пропускаем локальную загрузку");
            return;
        }

        System.out.println("Загрузка локальных чатов...");
        Chats.clear();

        try {
            File chatsDir = new File("local_chats");
            if (!chatsDir.exists()) {
                System.out.println("Директория local_chats не существует");
                return;
            }

            if (currentUser != null) {
                String userChatsFile = "local_chats/" + currentUser.getNick() + "_chats.dat";
                File file = new File(userChatsFile);
                if (file.exists()) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                        List<Integer> chatIds = (List<Integer>) ois.readObject();
                        System.out.println("Найдено " + chatIds.size() + " чатов для пользователя " + currentUser.getNick());

                        int loadedCount = 0;
                        for (Integer chatId : chatIds) {
                            File chatFile = new File("local_chats/chat_" + chatId + ".dat");
                            if (chatFile.exists()) {
                                try (ObjectInputStream chatOis = new ObjectInputStream(
                                        new FileInputStream(chatFile))) {
                                    Chat chat = (Chat) chatOis.readObject();
                                    if (!containsChat(chat.getId())) {
                                        Chats.add(chat);
                                        loadedCount++;
                                        System.out.println("Загружен чат: " + chat.getChatName() + " (ID: " + chat.getId() + ")");
                                    }
                                }
                            }
                        }
                        System.out.println("Загружено " + loadedCount + " чатов из локального хранилища");
                        chatsLoaded = true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки локальных чатов: " + e.getMessage());
        }
    }

    private boolean containsChat(int chatId) {
        for (Chat chat : Chats) {
            if (chat.getId() == chatId) {
                return true;
            }
        }
        return false;
    }

    private void createTestChatsIfNeeded() {
        if (!clientConnection.isConnected() && !chatsLoaded && Chats.isEmpty() && currentUser != null) {
            System.out.println("Создание локальных тестовых чатов...");
            createTestChats();
        }
    }

    private void createTestChats() {
        Chats.clear();

        List<User> users1 = new ArrayList<>();
        users1.add(currentUser);
        users1.add(new User("Anna", "pass", "Анна", "Иванова", "anna@test.com"));
        Chat chat1 = new Chat(users1, "Локальный чат с Анной");
        chat1.setId(generateNewChatId());
        chat1.send_message(new Message(currentUser, "Добро пожаловать в автономный режим!", new Date()));
        Chats.add(chat1);
        saveChatLocally(chat1);

        List<User> users2 = new ArrayList<>();
        users2.add(currentUser);
        users2.add(new User("Борис", "pass", "Борис", "Петров", "boris@test.com"));
        users2.add(new User("Мария", "pass", "Мария", "Сидорова", "maria@test.com"));
        Chat chat2 = new Chat(users2, "Групповой чат");
        chat2.setId(generateNewChatId());
        chat2.send_message(new Message(currentUser, "Чат создан в автономном режиме", new Date()));
        Chats.add(chat2);
        saveChatLocally(chat2);

        chatsLoaded = true;
        System.out.println("Создано " + Chats.size() + " тестовых чатов");
    }

    private int generateNewChatId() {
        int maxId = 0;
        for (Chat chat : Chats) {
            if (chat.getId() > maxId) {
                maxId = chat.getId();
            }
        }

        File chatsDir = new File("local_chats");
        if (chatsDir.exists()) {
            File[] files = chatsDir.listFiles((dir, name) -> name.startsWith("chat_") && name.endsWith(".dat"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String name = file.getName();
                        String idStr = name.substring(5, name.length() - 4);
                        int fileId = Integer.parseInt(idStr);
                        if (fileId > maxId) {
                            maxId = fileId;
                        }
                    } catch (NumberFormatException e) {
                        // Игнорируем ошибки парсинга
                    }
                }
            }
        }

        return maxId + 1;
    }

    // Реализация методов интерфейса Repository
    @Override
    public void saveMessage(User sender, String message, Chat chat) {
        if (chat == null) {
            System.err.println("Чат равен null");
            return;
        }

        Message msg = new Message(sender, message, new Date());
        chat.send_message(msg);
        saveChatLocally(chat);

        if (clientConnection.isConnected()) {
            clientConnection.sendMessageToChat(chat.getId(), message);
        } else {
            System.out.println("Сообщение сохранено локально");
        }
    }

    @Override
    public void saveMessage(Message message, Chat chat) {
        if (chat == null) {
            System.err.println("Чат равен null в saveMessage");
            return;
        }

        chat.send_message(message);
        saveChatLocally(chat);

        if (clientConnection.isConnected()) {
            clientConnection.sendMessageToChat(chat.getId(), message.getContent());
        } else {
            System.out.println("Сообщение сохранено локально");
        }
    }

    @Override
    public void send_msg(Chat chat, String message) {
        if (currentUser != null) saveMessage(currentUser, message, chat);
    }

    @Override
    public ObservableList<Message> getMessages() {
        return Messages;
    }

    @Override
    public List<Message> getMessagesByUser(String username) {
        List<Message> res = new ArrayList<>();
        for (Message m : Messages) {
            if (m.getSender() != null && username.equals(m.getSender().getNick())) res.add(m);
        }
        return res;
    }

    @Override
    public ObservableList<Chat> getChats() {
        return Chats;
    }

    @Override
    public void add_chat(Chat chat) {
        if (chat == null) {
            System.err.println("Нельзя добавить null чат");
            return;
        }

        if (chat.getId() <= 0) {
            chat.setId(generateNewChatId());
        }

        if (!containsChat(chat.getId())) {
            Chats.add(chat);
            System.out.println("✅ Чат добавлен в LocalRepository: " + chat.getChatName() +
                    " (ID: " + chat.getId() + ", пользователей: " + chat.getUsers().size() + ")");

            saveChatLocally(chat);

            if (currentUser != null) {
                currentUser.add_chat(chat);
                saveUserChats();
            }
        }

        // ИСПРАВЛЕНА ПРОВЕРКА ПОДКЛЮЧЕНИЯ
        System.out.println("\n=== ПРОВЕРКА ПОДКЛЮЧЕНИЯ ПЕРЕД ОТПРАВКОЙ НА СЕРВЕР ===");
        boolean networkConnected = clientConnection.isNetworkConnected();
        boolean fullyConnected = clientConnection.isFullyConnected();
        boolean hasCurrentUser = currentUser != null;

        System.out.println("TCP подключение: " + (networkConnected ? "✅" : "❌"));
        System.out.println("Полное подключение: " + (fullyConnected ? "✅" : "❌"));
        System.out.println("Текущий пользователь: " + (hasCurrentUser ? "✅ " + currentUser.getNick() : "❌ null"));

        // Отправляем на сервер если есть TCP соединение И есть пользователь
        if (networkConnected && hasCurrentUser) {
            try {
                // Собираем список ВСЕХ участников чата
                StringBuilder usersStr = new StringBuilder();
                for (User u : chat.getUsers()) {
                    if (!u.getNick().equals(currentUser.getNick())) {
                        if (usersStr.length() > 0) usersStr.append(",");
                        usersStr.append(u.getNick());
                    }
                }

                System.out.println("📤 Отправка чата на сервер: " + chat.getChatName());
                clientConnection.createChat(chat.getChatName(), usersStr.toString());

                // Добавляем приветственное сообщение
                Message welcomeMessage = new Message(currentUser,
                        "Чат \"" + chat.getChatName() + "\" создан! Добро пожаловать!", new Date());
                chat.send_message(welcomeMessage);
                saveChatLocally(chat);

                System.out.println("✅ Чат отправлен на сервер");

            } catch (Exception e) {
                System.err.println("❌ Ошибка отправки на сервер: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (!networkConnected) {
                System.out.println("⚠️ Нет TCP-соединения с сервером");
            }
            if (!hasCurrentUser) {
                System.out.println("⚠️ Текущий пользователь не определен");
            }
            System.out.println("💾 Чат сохранен только локально");
        }
    }


    private void saveUserChats() {
        if (currentUser != null) {
            try {
                File chatsDir = new File("local_chats");
                if (!chatsDir.exists()) {
                    chatsDir.mkdirs();
                }

                String filename = "local_chats/" + currentUser.getNick() + "_chats.dat";
                List<Integer> chatIds = new ArrayList<>();
                for (Chat chat : Chats) {
                    chatIds.add(chat.getId());
                }

                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                    oos.writeObject(chatIds);
                    System.out.println("Список чатов пользователя сохранен: " + chatIds.size() + " чатов");
                }
            } catch (IOException e) {
                System.err.println("Ошибка сохранения списка чатов пользователя: " + e.getMessage());
            }
        }
    }

    public void syncUsersToServer() {
        System.out.println("\n🔄 СИНХРОНИЗАЦИЯ ПОЛЬЗОВАТЕЛЕЙ С СЕРВЕРОМ");

        if (!clientConnection.isNetworkConnected()) {
            System.out.println("❌ Нет подключения к серверу, синхронизация невозможна");
            return;
        }

        // Загружаем всех локальных пользователей
        List<User> localUsers = UserStorage.getAllUsers();
        System.out.println("Найдено локальных пользователей: " + localUsers.size());

        for (User user : localUsers) {
            syncUserToServer(user);
        }
    }

    private void syncUserToServer(User user) {
        if (user == null) return;

        System.out.println("🔄 Синхронизация пользователя: " + user.getNick());

        // Пробуем сначала авторизоваться (пользователь уже может существовать на сервере)
        // Если авторизация не удастся, зарегистрируем пользователя

        new Thread(() -> {
            try {
                // Ждем немного перед синхронизацией каждого пользователя
                Thread.sleep(500);

                // Проверяем статус подключения
                if (!clientConnection.isNetworkConnected()) {
                    System.out.println("❌ Нет подключения для синхронизации " + user.getNick());
                    return;
                }

                // Пробуем зарегистрировать пользователя
                System.out.println("📤 Регистрация на сервере: " + user.getNick());
                clientConnection.register(user.getNick(), user.getPassword());

                // Ждем ответа от сервера
                Thread.sleep(1000);

                // Если текущий пользователь - авторизуем его
                if (currentUser != null && currentUser.getNick().equals(user.getNick())) {
                    System.out.println("🔐 Авторизация текущего пользователя: " + user.getNick());
                    clientConnection.authenticate(user.getNick(), user.getPassword());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Ошибка синхронизации пользователя " + user.getNick() + ": " + e.getMessage());
            }
        }).start();
    }

    // Добавьте метод для синхронизации текущего пользователя
    public void syncCurrentUserToServer() {
        if (currentUser != null && clientConnection.isNetworkConnected()) {
            syncUserToServer(currentUser);
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("Текущий пользователь установлен: " + (user != null ? user.getNick() : "null"));

        chatsLoaded = false;
        localChatsCache.clear();

        // Если подключены к серверу, синхронизируем пользователя
        if (clientConnection.isNetworkConnected() && user != null) {
            // Синхронизируем всех пользователей при первом подключении
            syncUsersToServer();

            // Затем авторизуем текущего пользователя с задержкой
            new Thread(() -> {
                try {
                    // Ждем завершения синхронизации
                    Thread.sleep(1500);

                    // Авторизуем текущего пользователя
                    if (clientConnection.isNetworkConnected()) {
                        System.out.println("🔐 Авторизация на сервере: " + user.getNick());
                        clientConnection.authenticate(user.getNick(), user.getPassword());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            loadLocalChats();
        }
    }

    public void disconnect() {
        clientConnection.logout();
    }

    public boolean isConnectedToServer() {
        return clientConnection.isNetworkConnected();
    }

    public boolean isFullyConnected() {
        return clientConnection.isFullyConnected();
    }

    public String getConnectionStatus() {
        return clientConnection.getConnectionStatusText();
    }

    public void printChatsInfo() {
        System.out.println("\nИНФОРМАЦИЯ О ЧАТАХ:");
        System.out.println("   Всего чатов: " + Chats.size());
        System.out.println("   Чаты загружены: " + chatsLoaded);
        System.out.println("   Подключены к серверу (TCP): " + clientConnection.isNetworkConnected());
        System.out.println("   Полное подключение: " + clientConnection.isFullyConnected());
        System.out.println("   Текущий пользователь: " + (currentUser != null ? currentUser.getNick() : "null"));

        for (int i = 0; i < Chats.size(); i++) {
            Chat chat = Chats.get(i);
            System.out.println("   " + (i + 1) + ". " + chat.getChatName() +
                    " (ID: " + chat.getId() + ", сообщений: " + chat.get_message_count() + ")");
        }
    }

    @Override
    public void updateMessage(Chat chat, int messageIndex, Message updatedMessage) {
        if (chat == null) {
            System.err.println("Чат равен null в updateMessage");
            return;
        }

        List<Message> messages = chat.getMessages();
        if (messages == null || messageIndex < 0 || messageIndex >= messages.size()) {
            System.err.println("Неверный индекс сообщения: " + messageIndex);
            return;
        }

        System.out.println("\n=== ОБНОВЛЕНИЕ СООБЩЕНИЯ ===");
        System.out.println("   Чат: " + chat.getChatName() + " (ID: " + chat.getId() + ")");
        System.out.println("   Индекс сообщения: " + messageIndex);
        System.out.println("   Отправитель: " + (updatedMessage.getSender() != null ? updatedMessage.getSender().getNick() : "null"));
        System.out.println("   Новый текст: " + updatedMessage.getContent());

        messages.set(messageIndex, updatedMessage);
        saveChatLocally(chat);

        System.out.println("Сообщение успешно обновлено!");
    }

    @Override
    public void deleteChat(Chat chat) {
        if (chat == null) {
            System.err.println("Нельзя удалить null чат");
            return;
        }

        System.out.println("\n=== УДАЛЕНИЕ ЧАТА В РЕПОЗИТОРИИ ===");
        System.out.println("Чат: " + chat.getChatName() + " (ID: " + chat.getId() + ")");

        // Удаляем из списка чатов
        Chats.remove(chat);

        // Удаляем локальные файлы
        deleteChatFiles(chat);

        // Обновляем список чатов пользователя
        updateUserChatsFile(chat);

        // Если подключены к серверу, отправляем команду удаления
        if (clientConnection.isNetworkConnected() && currentUser != null) {
            clientConnection.deleteChat(chat.getId());
        }

        System.out.println("Чат успешно удален из репозитория");
    }

    private void deleteChatFiles(Chat chat) {
        try {
            // Удаляем файл чата
            String chatFile = "local_chats/chat_" + chat.getId() + ".dat";
            java.io.File file = new java.io.File(chatFile);
            if (file.exists() && file.delete()) {
                System.out.println("Файл чата удален: " + chatFile);
            }

            // Удаляем папку с файлами чата
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
            if (currentUser == null) return;

            String userChatsFile = "local_chats/" + currentUser.getNick() + "_chats.dat";
            java.io.File file = new java.io.File(userChatsFile);

            if (file.exists()) {
                // Читаем текущий список чатов
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

                System.out.println("Список чатов пользователя обновлен. Осталось: " + chatIds.size());
            }

        } catch (Exception e) {
            System.err.println("Ошибка обновления файла чатов пользователя: " + e.getMessage());
        }
    }
}