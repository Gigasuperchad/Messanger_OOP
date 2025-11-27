package com.example.messanger_oop;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private Map<String, ClientHandler> connectedClients;
    private Repository messageRepository;

    public Server() {
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.connectedClients = new ConcurrentHashMap<>();
        this.messageRepository = new LocalRepository();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Сервер запущен на порту " + PORT);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clientThreadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        }
    }

    public void addClient(String username, ClientHandler handler) {
        connectedClients.put(username, handler);
        broadcastMessage("SERVER", username + " присоединился к чату");
        System.out.println("Клиент подключен: " + username);
    }

    public void removeClient(String username) {
        connectedClients.remove(username);
        broadcastMessage("SERVER", username + " покинул чат");
        System.out.println("Клиент отключен: " + username);
    }

    public void broadcastMessage(String sender, String message) {
        messageRepository.saveMessage(sender, message, new Date());

        for (ClientHandler client : connectedClients.values()) {
            if (!client.getUsername().equals(sender)) {
                client.sendMessage(sender + ": " + message);
            }
        }
    }

    public void sendPrivateMessage(String sender, String recipient, String message) {
        ClientHandler recipientHandler = connectedClients.get(recipient);
        if (recipientHandler != null) {
            messageRepository.saveMessage(sender, "(private to " + recipient + ") " + message, new Date());
            recipientHandler.sendMessage("[Private from " + sender + "]: " + message);
        }
    }

    public boolean isUsernameTaken(String username) {
        return connectedClients.containsKey(username);
    }

    public Set<String> getConnectedUsers() {
        return connectedClients.keySet();
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            clientThreadPool.shutdown();
            System.out.println("Сервер остановлен");
        } catch (IOException e) {
            System.err.println("Ошибка при остановке сервера: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}