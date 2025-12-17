package com.example.messanger_oop.server;

import com.example.messanger_oop.shared.ProtocolConstants;

import java.io.*;
import java.net.*;
import java.util.Date;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ServerManager serverManager;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean authenticated = false;

    public ClientHandler(Socket socket, ServerManager serverManager) {
        this.clientSocket = socket;
        this.serverManager = serverManager;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);

            // УБЕРИТЕ ТАЙМАУТ или установите очень большой:
            // clientSocket.setSoTimeout(0); // Отключаем таймаут
            // Или установите большой таймаут:
            clientSocket.setSoTimeout(300000); // 5 минут

            out.println("Добро пожаловать в мессенджер!");
            out.println("Используйте команды: LOGIN|user|pass или REGISTER|user|pass");

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Получено от клиента: " + message);

                // Обработка PING сообщений (keep-alive)
                if ("PING".equals(message)) {
                    out.println("PONG");
                    System.out.println("Отправлен PONG клиенту");
                    continue;
                }

                String response = processMessage(message);
                out.println(response);

                if (message.startsWith(ProtocolConstants.CMD_QUIT)) {
                    break;
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Клиент отключен по таймауту: " + (username != null ? username : "неавторизованный"));
        } catch (IOException e) {
            System.err.println("Ошибка ввода-вывода: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private String processMessage(String message) {
        String[] parts = message.split("\\" + ProtocolConstants.DELIMITER);
        if (parts.length < 1) {
            return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Неверный формат команды";
        }

        String command = parts[0];

        switch (command) {
            case ProtocolConstants.CMD_LOGIN:
                if (parts.length >= 3) {
                    String username = parts[1];
                    String password = parts[2];
                    String result = serverManager.authenticate(username, password);

                    if (result.startsWith(ProtocolConstants.RESP_AUTH_SUCCESS)) {
                        this.username = username;
                        this.authenticated = true;
                        serverManager.registerClient(username, this);
                        // Отправляем список чатов после успешного входа
                        serverManager.getUserChats(username);
                    }
                    return result;
                }
                break;

            case ProtocolConstants.CMD_REGISTER:
                if (parts.length >= 3) {
                    String username = parts[1];
                    String password = parts[2];
                    return serverManager.register(username, password);
                }
                break;

            case ProtocolConstants.CMD_GET_CHATS:
                if (authenticated && username != null) {
                    return serverManager.getUserChats(username);
                }
                break;

            case ProtocolConstants.CMD_SEND_MESSAGE:
                if (authenticated && username != null && parts.length >= 3) {
                    try {
                        int chatId = Integer.parseInt(parts[1]);
                        String messageContent = parts[2];
                        return serverManager.sendMessage(username, chatId, messageContent);
                    } catch (NumberFormatException e) {
                        return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Неверный ID чата";
                    }
                }
                break;

            case ProtocolConstants.CMD_CREATE_CHAT:
                if (authenticated && username != null && parts.length >= 3) {
                    String chatName = parts[1];
                    String participants = parts.length >= 3 ? parts[2] : "";
                    return serverManager.createChat(username, chatName, participants);
                }
                break;

            case ProtocolConstants.CMD_UPDATE_STATUS:
                if (authenticated && username != null && parts.length >= 2) {
                    String status = parts[1];
                    return serverManager.updateStatus(username, status);
                }
                break;

            case ProtocolConstants.CMD_GET_ONLINE_USERS:
                if (authenticated && username != null) {
                    return serverManager.getOnlineUsers();
                }
                break;

            case ProtocolConstants.CMD_DELETE_CHAT:
                if (authenticated && username != null && parts.length >= 2) {
                    try {
                        int chatId = Integer.parseInt(parts[1]);
                        return serverManager.deleteChat(username, chatId);
                    } catch (NumberFormatException e) {
                        return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Неверный ID чата";
                    }
                }
                break;

            case ProtocolConstants.CMD_LOGOUT:
                if (authenticated && username != null) {
                    authenticated = false;
                    return ProtocolConstants.RESP_OK + ProtocolConstants.DELIMITER + "Выход выполнен";
                }
                break;

            default:
                return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Неизвестная команда";
        }

        return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Недостаточно параметров";
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void disconnect() {
        if (username != null && authenticated) {
            serverManager.unregisterClient(username);
        }

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException ignored) {}

        System.out.println("Соединение закрыто: " + (username != null ? username : "неавторизованный клиент"));
    }
}