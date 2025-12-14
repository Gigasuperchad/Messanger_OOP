package com.example.messanger_oop;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;

    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool = Executors.newCachedThreadPool();
    private Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private UserManager userManager = new UserManager();
    private ChatManager chatManager = new ChatManager();
    private StatusManager statusManager;
    private boolean isRunning = true;

    public Server() {
        this.statusManager = StatusManager.getInstance();
    }

    public static void main(String[] args) {
        new Server().start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            System.out.println("Total users in database: " + userManager.getUserCount());
            System.out.println("Total chats in database: " + chatManager.getTotalChatCount());

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clientThreadPool.submit(handler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            clientThreadPool.shutdownNow();
        } catch (IOException ignored) {}
    }

    public boolean authenticate(String login, String pass) {
        boolean result = userManager.authenticate(login, pass);
        if (result) {
            statusManager.setUserOnline(login);
            broadcastUserStatusChange(login, "ONLINE");
        }
        return result;
    }

    public boolean registerUser(String login, String pass) {
        return userManager.register(login, pass);
    }

    public void registerClient(String username, ClientHandler handler) {
        connectedClients.put(username, handler);
        System.out.println("✅ Client registered: " + username);
        System.out.println("   Total connected clients: " + connectedClients.size());
    }

    public void unregisterClient(String username) {
        connectedClients.remove(username);
        statusManager.setUserOffline(username);
        broadcastUserStatusChange(username, "OFFLINE");
        System.out.println("❌ Client unregistered: " + username);
        System.out.println("   Total connected clients: " + connectedClients.size());
    }

    public void broadcast(String message) {
        for (ClientHandler ch : connectedClients.values()) {
            ch.sendMessage(message);
        }
    }

    private void broadcastUserStatusChange(String username, String status) {
        String message = String.format("[СТАТУС] %s: %s", username, status);
        for (ClientHandler ch : connectedClients.values()) {
            ch.sendMessage(message);
        }
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public void processClientCommand(String username, String commandLine, ClientHandler handler) {
        if (commandLine == null || commandLine.isEmpty()) return;

        String trimmed = commandLine.trim();
        System.out.println("Received command from " + username + ": " + trimmed);

        try {
            if (trimmed.equals("/chats")) {
                System.out.println("Requesting chat list for " + username);
                sendUserChats(username, handler);
                return;
            }

            if (trimmed.equals("/status")) {
                UserStatus status = statusManager.getUserStatus(username);
                handler.sendMessage("[Сервер]: Ваш статус: " + status.getStatusDisplay());
                return;
            }

            if (trimmed.startsWith("/status ")) {
                String statusCmd = trimmed.substring("/status ".length()).trim();
                handleStatusCommand(username, statusCmd, handler);
                return;
            }

            if (trimmed.equals("/who")) {
                sendOnlineUsersList(handler);
                return;
            }

            if (trimmed.startsWith("/chat_info ")) {
                String chatIdStr = trimmed.substring("/chat_info ".length()).trim();
                try {
                    int chatId = Integer.parseInt(chatIdStr);
                    Chat chat = chatManager.getChat(chatId);
                    if (chat != null) {
                        sendFullChatInfo(chat, handler);
                        System.out.println("Sent full info for chat " + chatId);
                    } else {
                        handler.sendMessage("[Сервер]: Чат не найден");
                    }
                } catch (NumberFormatException e) {
                    handler.sendMessage("[Сервер]: Неверный ID чата");
                }
                return;
            }

            if (trimmed.startsWith("/create_chat ")) {
                String payload = trimmed.substring("/create_chat ".length()).trim();
                System.out.println("\n🎯 СОЗДАНИЕ ЧАТА:");
                System.out.println("   От: " + username);
                System.out.println("   Параметры: " + payload);

                // Парсим участников и название чата
                int spaceIdx = payload.lastIndexOf(' ');
                String usersPart = spaceIdx == -1 ? "" : payload.substring(0, spaceIdx);
                String chatName = spaceIdx == -1 ? "Новый чат" : payload.substring(spaceIdx + 1).trim();

                System.out.println("   Участники: " + usersPart);
                System.out.println("   Название: " + chatName);

                List<User> users = new ArrayList<>();

                // Добавляем всех участников из списка
                String[] userNicks = usersPart.split(",");
                for (String userNick : userNicks) {
                    String nick = userNick.trim();
                    if (!nick.isEmpty()) {
                        User user = userManager.getUser(nick);
                        if (user != null) {
                            users.add(user);
                            System.out.println("   ✅ Добавлен участник: " + nick);
                        } else {
                            // Создаём пользователя если нет в системе
                            user = new User(nick);
                            users.add(user);
                            System.out.println("   ⚠️ Создан временный пользователь: " + nick);
                        }
                    }
                }

                // Проверяем, что есть хотя бы 2 участника
                if (users.size() < 2) {
                    handler.sendMessage("[Сервер]: Для создания чата нужно минимум 2 участника");
                    System.out.println("   ❌ Недостаточно участников: " + users.size());
                    return;
                }

                // Создаём чат
                Chat created = chatManager.createChat(users, chatName);
                System.out.println("   ✅ Чат создан: " + created.getChatName() +
                        " (ID: " + created.getId() + ", участников: " + users.size() + ")");

                // Добавляем приветственное сообщение
                User creator = userManager.getUser(username);
                if (creator != null) {
                    Message welcomeMessage = new Message(creator,
                            "Чат \"" + chatName + "\" создан! Добро пожаловать!", new Date());
                    created.send_message(welcomeMessage);
                    chatManager.saveChat(created);
                }

                // Отправляем уведомление и информацию о чате ВСЕМ участникам
                for (User participant : users) {
                    ClientHandler participantHandler = connectedClients.get(participant.getNick());
                    if (participantHandler != null) {
                        // Отправляем полную информацию о чате
                        sendFullChatInfo(created, participantHandler);

                        // Отправляем уведомление
                        String notification = String.format(
                                "[Сервер]: Вас добавили в чат \"%s\" с участниками: %s",
                                chatName, getParticipantsString(users, participant.getNick())
                        );
                        participantHandler.sendMessage(notification);

                        // Обновляем список чатов для участника
                        sendUserChats(participant.getNick(), participantHandler);

                        System.out.println("   📨 Уведомление отправлено: " + participant.getNick());
                    } else {
                        System.out.println("   ⚠️ Участник не в сети: " + participant.getNick());
                    }
                }

                handler.sendMessage("[Сервер]: Чат \"" + created.getChatName() + "\" успешно создан!");
                return;
            }

            if (trimmed.startsWith("/edit_message ")) {
                String[] parts = trimmed.split(" ", 4);
                if (parts.length == 4) {
                    try {
                        int chatId = Integer.parseInt(parts[1]);
                        int messageIndex = Integer.parseInt(parts[2]);
                        String newContent = parts[3];

                        System.out.println("\n=== РЕДАКТИРОВАНИЕ СООБЩЕНИЯ ===");
                        System.out.println("   Пользователь: " + username);
                        System.out.println("   Чат ID: " + chatId);
                        System.out.println("   Индекс сообщения: " + messageIndex);
                        System.out.println("   Новый текст: " + newContent);

                        Chat chat = chatManager.getChat(chatId);
                        if (chat != null && messageIndex >= 0 && messageIndex < chat.getMessages().size()) {
                            Message message = chat.getMessages().get(messageIndex);

                            if (message.getSender() != null &&
                                    username.equals(message.getSender().getNick())) {

                                message.setContent(newContent);
                                message.setEdited(true);
                                chatManager.saveChat(chat);

                                String notification = String.format(
                                        "[Сервер]: Пользователь %s отредактировал(а) сообщение в чате '%s'",
                                        username, chat.getChatName());

                                for (User participant : chat.getUsers()) {
                                    ClientHandler participantHandler = connectedClients.get(participant.getNick());
                                    if (participantHandler != null) {
                                        participantHandler.sendMessage(notification);
                                        sendFullChatInfo(chat, participantHandler);
                                    }
                                }

                                handler.sendMessage("[Сервер]: Сообщение успешно отредактировано");
                            } else {
                                handler.sendMessage("[Сервер]: Вы не можете редактировать это сообщение");
                            }
                        } else {
                            handler.sendMessage("[Сервер]: Сообщение не найдено");
                        }
                    } catch (NumberFormatException e) {
                        handler.sendMessage("[Сервер]: Неверный формат команды");
                    }
                }
                return;
            }

            if (trimmed.startsWith("/chat ")) {
                String rest = trimmed.substring("/chat ".length()).trim();
                int spaceIdx = rest.indexOf(' ');
                if (spaceIdx == -1) return;
                String chatIdStr = rest.substring(0, spaceIdx);
                String msg = rest.substring(spaceIdx + 1);
                int chatId = Integer.parseInt(chatIdStr);
                Chat chat = chatManager.getChat(chatId);
                if (chat != null) {
                    Message message = new Message(userManager.getUser(username), msg, new Date());
                    chatManager.addMessageToChat(chatId, message);

                    String broadcastMsg = String.format("[%s] %s: %s", chat.getChatName(), username, msg);
                    System.out.println("Отправка сообщения в чат: " + broadcastMsg);

                    for (User participant : chat.getUsers()) {
                        ClientHandler participantHandler = connectedClients.get(participant.getNick());
                        if (participantHandler != null) {
                            participantHandler.sendMessage(broadcastMsg);
                        }
                    }
                } else {
                    handler.sendMessage("[Сервер]: Чат не найден");
                }
                return;
            }

            if (trimmed.startsWith("/delete_chat ")) {
                String chatIdStr = trimmed.substring("/delete_chat ".length()).trim();
                try {
                    int chatId = Integer.parseInt(chatIdStr);
                    handleDeleteChat(username, chatId, handler);
                } catch (NumberFormatException e) {
                    handler.sendMessage("[Сервер]: Неверный ID чата");
                }
                return;
            }

            String broadcastMsg = String.format("[Общий чат] %s: %s", username, trimmed);
            System.out.println("Отправка сообщения в общий чат: " + broadcastMsg);
            broadcast(broadcastMsg);
            chatManager.addMessage("global", username + ": " + trimmed);

        } catch (Exception e) {
            System.err.println("Error processing command: " + e.getMessage());
            e.printStackTrace();
            handler.sendMessage("[Сервер]: Ошибка обработки команды: " + e.getMessage());
        }
    }

    private String getParticipantsString(List<User> users, String excludeUser) {
        StringBuilder sb = new StringBuilder();
        for (User user : users) {
            if (!user.getNick().equals(excludeUser)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(user.getNick());
            }
        }
        return sb.toString();
    }

    private void handleDeleteChat(String username, int chatId, ClientHandler handler) {
        try {
            System.out.println("\n=== ЗАПРОС НА УДАЛЕНИЕ ЧАТА ===");
            System.out.println("Пользователь: " + username);
            System.out.println("ID чата: " + chatId);

            Chat chat = chatManager.getChat(chatId);
            if (chat == null) {
                handler.sendMessage("[Сервер]: Чат не найден");
                return;
            }

            // Проверяем права пользователя (должен быть участником чата)
            boolean isParticipant = false;
            for (User user : chat.getUsers()) {
                if (username.equals(user.getNick())) {
                    isParticipant = true;
                    break;
                }
            }

            if (!isParticipant) {
                handler.sendMessage("[Сервер]: У вас нет прав для удаления этого чата");
                return;
            }

            // Удаляем чат из кэша
            chatManager.deleteChat(chatId);

            // Уведомляем всех участников
            String notification = String.format(
                    "[Сервер]: Чат '%s' был удален пользователем %s",
                    chat.getChatName(), username);

            for (User participant : chat.getUsers()) {
                ClientHandler participantHandler = connectedClients.get(participant.getNick());
                if (participantHandler != null) {
                    participantHandler.sendMessage(notification);
                    // Обновляем список чатов для участника
                    sendUserChats(participant.getNick(), participantHandler);
                }
            }

            handler.sendMessage("[Сервер]: Чат успешно удален");

        } catch (Exception e) {
            System.err.println("Ошибка удаления чата: " + e.getMessage());
            handler.sendMessage("[Сервер]: Ошибка удаления чата: " + e.getMessage());
        }
    }

    private void handleStatusCommand(String username, String statusCmd, ClientHandler handler) {
        try {
            if (statusCmd.equals("online")) {
                statusManager.setUserOnline(username);
            } else if (statusCmd.equals("offline")) {
                statusManager.setUserOffline(username);
            } else if (statusCmd.equals("away")) {
                statusManager.setUserStatus(username, UserStatus.Status.AWAY);
            } else if (statusCmd.equals("dnd") || statusCmd.equals("donotdisturb")) {
                statusManager.setUserStatus(username, UserStatus.Status.DO_NOT_DISTURB);
            } else if (statusCmd.equals("invisible")) {
                statusManager.setUserStatus(username, UserStatus.Status.INVISIBLE);
            } else if (statusCmd.startsWith("message ")) {
                String message = statusCmd.substring("message ".length()).trim();
                statusManager.setCustomStatusMessage(username, message);
            } else {
                handler.sendMessage("[Сервер]: Неизвестная команда статуса. Используйте: online, offline, away, dnd, invisible, message <текст>");
                return;
            }

            broadcastUserStatusChange(username, statusManager.getUserStatus(username).getStatus().name());
            handler.sendMessage("[Сервер]: Статус обновлен: " +
                    statusManager.getUserStatus(username).getStatusDisplay());
        } catch (Exception e) {
            handler.sendMessage("[Сервер]: Ошибка изменения статуса: " + e.getMessage());
        }
    }

    private void sendOnlineUsersList(ClientHandler handler) {
        List<String> onlineUsers = statusManager.getOnlineUsers();
        StringBuilder response = new StringBuilder("🟢 Пользователи онлайн (" + onlineUsers.size() + "):\n");

        for (String user : onlineUsers) {
            UserStatus status = statusManager.getUserStatus(user);
            response.append("• ").append(status.getStatusDisplay());

            if (!status.getCustomMessage().isEmpty()) {
                response.append(" - ").append(status.getCustomMessage());
            }
            response.append("\n");
        }

        handler.sendMessage(response.toString());
    }

    private void sendFullChatInfo(Chat chat, ClientHandler handler) {
        try {
            StringBuilder participants = new StringBuilder();
            for (User user : chat.getUsers()) {
                if (participants.length() > 0) participants.append(",");
                participants.append(user.getNick());
            }

            StringBuilder messages = new StringBuilder();
            for (Message message : chat.getMessages()) {
                if (messages.length() > 0) messages.append(";");
                String senderNick = message.getSender() != null ?
                        message.getSender().getNick() : "unknown";
                String timestamp = message.getTimestamp() != null ?
                        String.valueOf(message.getTimestamp().getTime()) : "0";
                String editedFlag = message.isEdited() ? "1" : "0";
                messages.append(senderNick)
                        .append("|")
                        .append(message.getContent())
                        .append("|")
                        .append(timestamp)
                        .append("|")
                        .append(editedFlag);
            }

            String fullInfo = "CHAT_FULL:" +
                    chat.getId() + ":" +
                    chat.getChatName() + ":" +
                    (participants.length() > 0 ? participants.toString() : "null") + ":" +
                    (messages.length() > 0 ? messages.toString() : "null");

            handler.sendMessage(fullInfo);
        } catch (Exception e) {
            System.err.println("Error forming full chat info: " + e.getMessage());
        }
    }

    public void sendUserChats(String username, ClientHandler handler) {
        System.out.println("📨 Sending chat list to " + username);
        List<Chat> chats = chatManager.getUserChats(username);
        System.out.println("   Found " + chats.size() + " chats for " + username);

        if (chats.isEmpty()) {
            System.out.println("   No chats found for user " + username);
            handler.sendMessage("CHAT_LIST_START");
            handler.sendMessage("CHAT_LIST_END");
            return;
        }

        handler.sendMessage("CHAT_LIST_START");
        for (Chat chat : chats) {
            handler.sendMessage("CHAT:" + chat.getId() + ":" + chat.getChatName());
            System.out.println("   Sending chat: " + chat.getChatName() + " (ID: " + chat.getId() + ")");
        }
        handler.sendMessage("CHAT_LIST_END");
        System.out.println("✅ Chat list sent to " + username);

        // После отправки списка чатов, отправляем полную информацию о каждом чате
        new Thread(() -> {
            try {
                Thread.sleep(500); // Небольшая задержка
                for (Chat chat : chats) {
                    sendFullChatInfo(chat, handler);
                    Thread.sleep(100); // Небольшая пауза между чатами
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}