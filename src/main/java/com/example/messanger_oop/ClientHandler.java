package com.example.messanger_oop;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Ошибка инициализации клиента: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            out.println("Введите ваше имя:");
            username = in.readLine();

            if (username == null || username.trim().isEmpty()) {
                out.println("Имя не может быть пустым. Соединение закрыто.");
                return;
            }

            if (server.isUsernameTaken(username)) {
                out.println("Имя уже занято. Соединение закрыто.");
                return;
            }

            server.addClient(username, this);
            out.println("Добро пожаловать в чат, " + username + "!");
            out.println("Доступные команды:");
            out.println("/users - список подключенных пользователей");
            out.println("/private <username> <message> - приватное сообщение");
            out.println("/quit - выход из чата");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("/quit")) {
                    break;
                } else if (message.equalsIgnoreCase("/users")) {
                    out.println("Подключенные пользователи: " + server.getConnectedUsers());
                } else if (message.startsWith("/private ")) {
                    handlePrivateMessage(message);
                } else {
                    server.broadcastMessage(username, message);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка клиента " + username + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handlePrivateMessage(String message) {
        String[] parts = message.split(" ", 3);
        if (parts.length >= 3) {
            String recipient = parts[1];
            String privateMessage = parts[2];
            server.sendPrivateMessage(username, recipient, privateMessage);
        } else {
            out.println("Неверный формат приватного сообщения. Используйте: /private username message");
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;
    }

    private void disconnect() {
        try {
            if (username != null) {
                server.removeClient(username);
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Ошибка при отключении клиента: " + e.getMessage());
        }
    }
}