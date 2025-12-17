package com.example.messanger_oop.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private ServerManager serverManager;
    private boolean isRunning = true;

    public Server() {
        this.serverManager = new ServerManager();
        System.out.println("Сервер мессенджера инициализирован");
        System.out.println("Порт: " + PORT);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("✅ Сервер запущен на порту " + PORT);
            System.out.println("Ожидание подключений...");

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Новое подключение: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, serverManager);
                threadPool.submit(handler);
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("❌ Ошибка сервера: " + e.getMessage());
            }
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Сервер остановлен");
            }
            threadPool.shutdownNow();
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}