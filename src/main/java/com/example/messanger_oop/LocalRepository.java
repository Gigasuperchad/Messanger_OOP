package com.example.messanger_oop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalRepository implements Repository {
    private ObservableList<Message> Messages = FXCollections.observableArrayList();
    private ObservableList<Chat> Chats = FXCollections.observableArrayList();
    private User currentUser;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connectedToServer = false;
    private Thread serverReader;
    private boolean chatsLoaded = false;
    private Map<Integer, Chat> serverChatsCache = new HashMap<>();

    public LocalRepository() {
        System.out.println("LocalRepository initialized");
    }

    public boolean connectToServer() {
        System.out.println("Trying to connect to server...");
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", 12345), 3000);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            connectedToServer = true;
            startServerReader();

            if (currentUser != null) {
                authenticateOnServer();
            }

            System.out.println("Connected to server");
            return true;
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            connectedToServer = false;
            createTestChatsIfNeeded();
            return false;
        }
    }

    private void authenticateOnServer() {
        if (out != null && currentUser != null) {
            try {
                out.println("2");
                out.println(currentUser.getNick());
                out.println(currentUser.getPassword());
                System.out.println("Authentication data sent to server");
            } catch (Exception e) {
                System.err.println("Server auth error: " + e.getMessage());
            }
        }
    }

    private void startServerReader() {
        if (serverReader != null && serverReader.isAlive()) {
            serverReader.interrupt();
        }

        serverReader = new Thread(() -> {
            try {
                String line;
                boolean inChatList = false;
                List<Chat> chatList = new ArrayList<>();

                while ((line = in.readLine()) != null) {
                    System.out.println("Server: " + line);

                    if ("CHAT_LIST_START".equals(line)) {
                        inChatList = true;
                        chatList.clear();
                        System.out.println("Start receiving chat list");
                        continue;
                    }

                    if ("CHAT_LIST_END".equals(line)) {
                        inChatList = false;
                        System.out.println("End of chat list. Total chats: " + chatList.size());

                        if (!chatList.isEmpty()) {
                            requestFullChatInfo(chatList);
                        } else {
                            javafx.application.Platform.runLater(() -> {
                                Chats.clear();
                                chatsLoaded = true;
                                System.out.println("No chats from server");
                            });
                        }
                        continue;
                    }

                    if (inChatList) {
                        if (line.startsWith("CHAT:")) {
                            String payload = line.substring("CHAT:".length());
                            int colon = payload.indexOf(':');
                            if (colon > 0) {
                                try {
                                    int id = Integer.parseInt(payload.substring(0, colon));
                                    String name = payload.substring(colon + 1);

                                    Chat tempChat = new Chat(new ArrayList<>(), name);
                                    tempChat.setId(id);
                                    chatList.add(tempChat);
                                    System.out.println("Chat added to list: " + name + " (ID: " + id + ")");
                                } catch (NumberFormatException e) {
                                    System.err.println("Chat ID parsing error: " + payload);
                                }
                            }
                        }
                        continue;
                    }

                    if (line.startsWith("CHAT_FULL:")) {
                        processFullChatInfo(line);
                        continue;
                    }

                    // Обработка сообщений из чатов для уведомлений
                    if (line.startsWith("[") && line.contains("]: ")) {
                        // Формат: "[НазваниеЧата] Отправитель: текст"
                        int bracketEnd = line.indexOf(']');
                        if (bracketEnd > 0) {
                            String chatName = line.substring(1, bracketEnd);
                            String rest = line.substring(bracketEnd + 2); // "]: " = 3 символа

                            int colonIndex = rest.indexOf(':');
                            if (colonIndex > 0) {
                                String sender = rest.substring(0, colonIndex).trim();
                                String content = rest.substring(colonIndex + 1).trim();

                                // Показываем уведомление для сообщения
                                showNotificationForMessage(chatName, sender, content);
                            }
                        }
                    }

                    // Добавляем сообщение в общий список
                    Messages.add(new Message(new User("System"), line, new Date()));
                }
            } catch (IOException e) {
                System.err.println("Server reader stopped: " + e.getMessage());
                connectedToServer = false;
                createTestChatsIfNeeded();
            }
        });
        serverReader.setDaemon(true);
        serverReader.start();
    }

    private void showNotificationForMessage(String chatName, String sender, String content) {
        System.out.println("\n🔔 НОВОЕ СООБЩЕНИЕ ДЛЯ УВЕДОМЛЕНИЯ:");
        System.out.println("   Чат: " + chatName);
        System.out.println("   Отправитель: " + sender);
        System.out.println("   Сообщение: " + content);

        // Проверяем, что сообщение не от текущего пользователя
        if (currentUser != null && !sender.equals(currentUser.getNick())) {
            // Получаем текущий чат из AppManager
            AppManager appManager = AppManager.getInstance();
            Chat currentChat = appManager.getCurrentChat();

            // Показываем уведомление, если:
            // 1. Приложение не активно
            // 2. ИЛИ текущий чат не совпадает с чатом уведомления
            if (!appManager.isAppActive() ||
                    (currentChat == null || !currentChat.getChatName().equals(chatName))) {

                String notificationTitle = "Новое сообщение в чате";
                String notificationMessage = String.format("%s: %s", sender, content);

                appManager.showNotification(notificationTitle,
                        String.format("Чат: %s\nОт: %s\n%s", chatName, sender, content));
            }
        }
    }

    public void saveChatFile(Chat chat, File sourceFile) throws IOException {
        String chatFilesDir = "chat_files/chat_" + chat.getId();
        File chatDir = new File(chatFilesDir);
        if (!chatDir.exists()) {
            chatDir.mkdirs();
        }

        String fileName = System.currentTimeMillis() + "_" + sourceFile.getName();
        String destPath = chatFilesDir + "/" + fileName;
        Files.copy(sourceFile.toPath(), Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING);
    }

    private void requestFullChatInfo(List<Chat> chatList) {
        if (out != null && currentUser != null) {
            System.out.println("Requesting full info for " + chatList.size() + " chats");
            for (Chat chat : chatList) {
                out.println("/chat_info " + chat.getId());
                System.out.println("Requested chat ID: " + chat.getId());
            }
        }
    }

    private List<User> parseParticipants(String participantsStr) {
        List<User> participants = new ArrayList<>();
        if (participantsStr != null && !participantsStr.isEmpty() && !participantsStr.equals("null")) {
            String[] nicknames = participantsStr.split(",");
            for (String nickname : nicknames) {
                String trimmed = nickname.trim();
                if (!trimmed.isEmpty()) {
                    User user = new User(trimmed);
                    participants.add(user);
                }
            }
        }
        return participants;
    }

    private void processFullChatInfo(String line) {
        try {
            String payload = line.substring("CHAT_FULL:".length());
            String[] parts = payload.split(":", 4);

            if (parts.length >= 4) {
                int id = Integer.parseInt(parts[0]);
                String name = parts[1];
                String participantsStr = parts[2];
                String messagesStr = parts[3];

                List<User> participants = parseParticipants(participantsStr);
                Chat fullChat = new Chat(participants, name);
                fullChat.setId(id);

                parseMessages(messagesStr, fullChat);

                serverChatsCache.put(id, fullChat);

                updateChatListFromCache();

                System.out.println("Full chat loaded: " + name +
                        " (participants: " + participants.size() +
                        ", messages: " + fullChat.get_message_count() + ")");
            }
        } catch (Exception e) {
            System.err.println("CHAT_FULL processing error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseMessages(String messagesStr, Chat chat) {
        if (messagesStr != null && !messagesStr.isEmpty() && !messagesStr.equals("null")) {
            String[] messageParts = messagesStr.split(";");
            for (String messagePart : messageParts) {
                if (!messagePart.trim().isEmpty()) {
                    String[] msgData = messagePart.split("\\|", 4);
                    if (msgData.length >= 3) {
                        String senderNick = msgData[0];
                        String content = msgData[1];
                        String timestampStr = msgData[2];
                        boolean edited = false;

                        if (msgData.length >= 4) {
                            edited = "1".equals(msgData[3]);
                        }

                        try {
                            User sender = new User(senderNick);
                            Date timestamp = new Date(Long.parseLong(timestampStr));
                            Message message = new Message(sender, content, timestamp);
                            message.setEdited(edited);
                            chat.send_message(message);
                        } catch (NumberFormatException e) {
                            System.err.println("Timestamp parsing error: " + timestampStr);
                        }
                    }
                }
            }
        }
    }

    private void updateChatListFromCache() {
        javafx.application.Platform.runLater(() -> {
            Chats.clear();
            Chats.addAll(serverChatsCache.values());
            chatsLoaded = true;

            saveChatsLocally();

            System.out.println("Chat list updated: " + Chats.size() + " chats");
            for (Chat chat : Chats) {
                System.out.println("" + chat.getChatName() +
                        " (ID: " + chat.getId() +
                        ", users: " + chat.getUsers().size() +
                        ", msgs: " + chat.get_message_count() + ")");
            }
        });
    }

    private void saveChatsLocally() {
        try {
            File chatsDir = new File("local_chats");
            if (!chatsDir.exists()) {
                chatsDir.mkdirs();
            }

            for (Chat chat : Chats) {
                saveChatLocally(chat);
            }

            if (currentUser != null) {
                List<Integer> chatIds = new ArrayList<>();
                for (Chat chat : Chats) {
                    chatIds.add(chat.getId());
                }

                String userChatsFile = "local_chats/" + currentUser.getNick() + "_chats.dat";
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        new FileOutputStream(userChatsFile))) {
                    oos.writeObject(chatIds);
                    System.out.println("Saved " + chatIds.size() + " chats for user " + currentUser.getNick());
                }
            }
        } catch (IOException e) {
            System.err.println("Chat saving error: " + e.getMessage());
        }
    }

    private void loadLocalChats() {
        if (chatsLoaded) {
            System.out.println("Chats already loaded, skipping local load");
            return;
        }

        System.out.println("Loading local chats...");
        Chats.clear();

        try {
            File chatsDir = new File("local_chats");
            if (!chatsDir.exists()) {
                System.out.println("local_chats directory doesn't exist");
                return;
            }

            if (currentUser != null) {
                String userChatsFile = "local_chats/" + currentUser.getNick() + "_chats.dat";
                File file = new File(userChatsFile);
                if (file.exists()) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                        List<Integer> chatIds = (List<Integer>) ois.readObject();
                        System.out.println("Found " + chatIds.size() + " chats for user " + currentUser.getNick());

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
                                        System.out.println("Loaded chat: " + chat.getChatName() + " (ID: " + chat.getId() + ")");
                                    } else {
                                        System.out.println("Chat " + chatId + " already loaded, skipping");
                                    }
                                }
                            } else {
                                System.out.println("Chat file not found: chat_" + chatId + ".dat");
                            }
                        }
                        System.out.println("Loaded " + loadedCount + " chats from local storage");
                        chatsLoaded = true;
                    }
                } else {
                    System.out.println("User chats file not found: " + userChatsFile);
                }
            }
        } catch (Exception e) {
            System.err.println("Local chat loading error: " + e.getMessage());
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
        if (!connectedToServer && !chatsLoaded && Chats.isEmpty() && currentUser != null) {
            System.out.println("Creating local test chats...");
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
        System.out.println("Created " + Chats.size() + " test chats");
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

    @Override
    public void updateMessage(Chat chat, int messageIndex, Message updatedMessage) {
        if (chat == null) {
            System.err.println("Chat is null in updateMessage");
            return;
        }

        List<Message> messages = chat.getMessages();
        if (messages == null || messageIndex < 0 || messageIndex >= messages.size()) {
            System.err.println("Invalid message index: " + messageIndex);
            return;
        }

        System.out.println("\n=== ОБНОВЛЕНИЕ СООБЩЕНИЯ ===");
        System.out.println("   Чат: " + chat.getChatName() + " (ID: " + chat.getId() + ")");
        System.out.println("   Индекс сообщения: " + messageIndex);
        System.out.println("   Отправитель: " + (updatedMessage.getSender() != null ? updatedMessage.getSender().getNick() : "null"));
        System.out.println("   Новый текст: " + updatedMessage.getContent());

        // Обновляем сообщение в чате
        messages.set(messageIndex, updatedMessage);

        // Сохраняем изменения локально
        saveChatLocally(chat);

        // Отправляем на сервер, если подключены
        if (connectedToServer && out != null) {
            try {
                String command = "/edit_message " + chat.getId() + " " +
                        messageIndex + " " + updatedMessage.getContent();
                out.println(command);
                System.out.println("📡 Отправлено на сервер: " + command);
            } catch (Exception e) {
                System.err.println("Ошибка отправки редактирования на сервер: " + e.getMessage());
            }
        }

        System.out.println("Сообщение успешно обновлено!");
    }

    @Override
    public void saveMessage(User sender, String message, Chat chat) {
        if (chat == null) {
            System.err.println("Chat is null");
            return;
        }

        Message msg = new Message(sender, message, new Date());
        chat.send_message(msg);
        saveChatLocally(chat);

        if (connectedToServer && out != null) {
            try {
                out.println("/chat " + chat.getId() + " " + message);
                System.out.println("Message sent to server");
            } catch (Exception e) {
                System.err.println("Send error: " + e.getMessage());
            }
        } else {
            System.out.println("Message saved locally");
        }
    }

    // Добавленный метод для сохранения сообщений с файлами
    public void saveMessage(Message message, Chat chat) {
        if (chat == null) {
            System.err.println("Chat is null in saveMessage");
            return;
        }

        chat.send_message(message);
        saveChatLocally(chat);

        if (connectedToServer && out != null) {
            try {
                // Для сообщений с файлами отправляем специальную команду
                if (message.hasAttachment()) {
                    String fileCommand = String.format("/file %d \"%s\" \"%s\" \"%s\" %d \"%s\"",
                            chat.getId(),
                            message.getFileName(),
                            message.getFileType(),
                            message.getContent(),
                            message.getFileSize(),
                            message.getFilePath());
                    out.println(fileCommand);
                    System.out.println("📡 Отправлено сообщение с файлом на сервер");
                } else {
                    out.println("/chat " + chat.getId() + " " + message.getContent());
                    System.out.println("📡 Отправлено текстовое сообщение на сервер");
                }
            } catch (Exception e) {
                System.err.println("Ошибка отправки на сервер: " + e.getMessage());
            }
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
            System.err.println("Can't add null chat");
            return;
        }

        if (chat.getId() <= 0) {
            chat.setId(generateNewChatId());
        }

        if (!containsChat(chat.getId())) {
            Chats.add(chat);
            System.out.println("Chat added to LocalRepository: " + chat.getChatName() +
                    " (ID: " + chat.getId() + ", users: " + chat.getUsers().size() + ")");

            saveChatLocally(chat);

            if (currentUser != null) {
                currentUser.add_chat(chat);
                saveUserChats();
            }
        } else {
            System.out.println("Chat with ID " + chat.getId() + " already exists, skipping");
        }

        if (connectedToServer && out != null && currentUser != null) {
            try {
                StringBuilder usersStr = new StringBuilder();
                for (User u : chat.getUsers()) {
                    if (!u.getNick().equals(currentUser.getNick())) {
                        if (usersStr.length() > 0) usersStr.append(",");
                        usersStr.append(u.getNick());
                    }
                }

                if (usersStr.length() > 0) {
                    String command = "/create_chat " + usersStr.toString() + " " + chat.getChatName();
                    out.println(command);
                    System.out.println("📡 Sent to server: " + command);

                    out.println("/chats");
                }
            } catch (Exception e) {
                System.err.println("Server send error: " + e.getMessage());
            }
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
                System.out.println("Chat saved locally: " + filename);
            }
        } catch (IOException e) {
            System.err.println("Chat saving error: " + e.getMessage());
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
                    System.out.println("User chat list saved: " + chatIds.size() + " chats");
                }
            } catch (IOException e) {
                System.err.println("User chat list saving error: " + e.getMessage());
            }
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("Current user set: " + (user != null ? user.getNick() : "null"));

        chatsLoaded = false;
        serverChatsCache.clear();

        if (!connectedToServer) {
            loadLocalChats();
        }

        if (!connectedToServer) {
            connectToServer();
        } else {
            if (out != null) {
                out.println("/chats");
            }
        }
    }

    public void disconnect() {
        try {
            if (out != null) out.println("/quit");
            if (socket != null && !socket.isClosed()) socket.close();
            if (serverReader != null) serverReader.interrupt();
        } catch (IOException ignored) {}
        connectedToServer = false;
    }

    public boolean isConnectedToServer() {
        return connectedToServer;
    }

    public void printChatsInfo() {
        System.out.println("\nCHAT INFORMATION:");
        System.out.println("   Total chats: " + Chats.size());
        System.out.println("   Chats loaded: " + chatsLoaded);
        System.out.println("   Connected to server: " + connectedToServer);
        System.out.println("   Current user: " + (currentUser != null ? currentUser.getNick() : "null"));

        for (int i = 0; i < Chats.size(); i++) {
            Chat chat = Chats.get(i);
            System.out.println("   " + (i + 1) + ". " + chat.getChatName() +
                    " (ID: " + chat.getId() + ", messages: " + chat.get_message_count() + ")");
        }
    }
}