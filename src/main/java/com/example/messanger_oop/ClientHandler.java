package com.example.messanger_oop;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private User user;

    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);

            sendMessage("Добро пожаловать в сервер мессенджера!");
            sendMessage("Выберите действие:");
            sendMessage("1. Регистрация нового пользователя");
            sendMessage("2. Вход существующего пользователя");
            sendMessage("Введите номер (1 или 2):");

            String action = readLineWithTimeout(30000);
            if (action == null) {
                sendMessage("⏱Таймаут ожидания выбора");
                close();
                return;
            }

            action = action.trim();
            if ("1".equals(action)) {
                if (!handleRegistration()) {
                    close();
                    return;
                }
            } else if ("2".equals(action)) {
                if (!handleLogin()) {
                    close();
                    return;
                }
            } else {
                sendMessage("Неверный выбор. Отключение.");
                close();
                return;
            }

            server.registerClient(username, this);
            server.sendUserChats(username, this);

            List<String> history = server.getChatManager().getMessages("global");
            for (String h : history) sendMessage(h);

            sendMessage("ВЫ ВОШЛИ В МЕССЕНДЖЕР. Для справки используйте /help");

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if ("/quit".equalsIgnoreCase(line)) {
                    sendMessage("До свидания!");
                    break;
                }
                server.processClientCommand(username, line, this);
            }

        } catch (IOException e) {
            System.err.println("IO error for client " + username + ": " + e.getMessage());
        } finally {
            server.unregisterClient(username);
            close();
        }
    }

    private boolean handleRegistration() throws IOException {
        sendMessage("=== РЕГИСТРАЦИЯ ===");
        sendMessage("Введите логин:");
        String login = in.readLine();
        if (login == null) return false;

        sendMessage("Введите пароль:");
        String pass = in.readLine();
        if (pass == null) return false;

        if (server.registerUser(login.trim(), pass.trim())) {
            this.username = login.trim();
            this.user = server.getUserManager().getUser(this.username);
            sendMessage("Регистрация успешна! Вы автоматически авторизованы.");
            return true;
        } else {
            sendMessage("Логин уже занят.");
            return false;
        }
    }

    private boolean handleLogin() throws IOException {
        sendMessage("=== ВХОД ===");
        sendMessage("Введите логин:");
        String login = in.readLine();
        if (login == null) return false;

        sendMessage("Введите пароль:");
        String pass = in.readLine();
        if (pass == null) return false;

        if (server.authenticate(login.trim(), pass.trim())) {
            this.username = login.trim();
            this.user = server.getUserManager().getUser(this.username);
            sendMessage("Вход выполнен! Добро пожаловать, " + (user != null ? user.getFirstName() : username) + "!");
            return true;
        } else {
            sendMessage("Неверный логин или пароль.");
            return false;
        }
    }

    private String readLineWithTimeout(long timeoutMillis) throws IOException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (in.ready()) {
                return in.readLine();
            }
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        return null;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void close() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException ignored) {}
    }
}