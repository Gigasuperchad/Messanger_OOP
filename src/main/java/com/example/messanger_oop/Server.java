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
    private boolean isRunning = true;

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
        return userManager.authenticate(login, pass);
    }

    public boolean registerUser(String login, String pass) {
        return userManager.register(login, pass);
    }

    public void registerClient(String username, ClientHandler handler) {
        connectedClients.put(username, handler);
        System.out.println("Client registered: " + username);
        System.out.println("Total connected clients: " + connectedClients.size());
    }

    public void unregisterClient(String username) {
        connectedClients.remove(username);
        System.out.println("Client unregistered: " + username);
        System.out.println("Total connected clients: " + connectedClients.size());
    }

    public void broadcast(String message) {
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
                System.out.println("Creating chat with params: " + payload);

                int idx = payload.indexOf(' ');
                String usersPart = idx == -1 ? "" : payload.substring(0, idx);
                String chatName = idx == -1 ? "Чат" : payload.substring(idx + 1).trim();

                List<User> users = new ArrayList<>();
                User creator = userManager.getUser(username);
                if (creator != null) {
                    users.add(creator);
                    System.out.println("Creator: " + username);
                }

                if (!usersPart.isEmpty()) {
                    String[] parts = usersPart.split(",");
                    System.out.println("Participants: " + Arrays.toString(parts));

                    for (String u : parts) {
                        User uu = userManager.getUser(u.trim());
                        if (uu != null) {
                            users.add(uu);
                            System.out.println("Added participant: " + u);
                        } else {
                            System.out.println("Participant not found: " + u);
                        }
                    }
                }

                Chat created = chatManager.createChat(users, chatName);
                System.out.println("Chat created: " + created.getChatName() +
                        " (ID " + created.getId() + ", users: " + users.size() + ")");

                String confirmation = "[Сервер]: Чат \"" + created.getChatName() + "\" успешно создан!";
                handler.sendMessage(confirmation);

                for (User participant : users) {
                    ClientHandler participantHandler = connectedClients.get(participant.getNick());
                    if (participantHandler != null) {
                        sendUserChats(participant.getNick(), participantHandler);
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

                    String broadcastMsg = "[" + chat.getChatName() + "] " + username + ": " + msg;
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

            String globalMsg = username + ": " + trimmed;
            broadcast(globalMsg);
            chatManager.addMessage("global", username + ": " + trimmed);

        } catch (Exception e) {
            System.err.println("Error processing command: " + e.getMessage());
            e.printStackTrace();
            handler.sendMessage("[Сервер]: Ошибка обработки команды: " + e.getMessage());
        }
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
                messages.append(senderNick)
                        .append("|")
                        .append(message.getContent())
                        .append("|")
                        .append(timestamp);
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
        System.out.println("Sending chat list to " + username);
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
            System.out.println("Sending chat: " + chat.getChatName() + " (ID: " + chat.getId() + ")");
        }
        handler.sendMessage("CHAT_LIST_END");
        System.out.println("Chat list sent to " + username);
    }
}