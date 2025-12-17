package com.example.messanger_oop.client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.example.messanger_oop.shared.ProtocolConstants;
import javafx.application.Platform;

public class ClientConnection {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String serverAddress = "127.0.0.1";
    private int serverPort = 12345;
    private String currentUser;
    private MessageListener messageListener;
    private volatile boolean socketConnected = false;
    private Thread readerThread;
    private volatile boolean running = false;
    private ScheduledExecutorService keepAliveExecutor;

    // Состояния подключения
    public enum ConnectionState {
        DISCONNECTED,           // Нет соединения
        CONNECTING,             // В процессе подключения
        SOCKET_CONNECTED,       // TCP соединение установлено
        AUTHENTICATING,         // Идет авторизация
        AUTHENTICATED           // Полностью подключен и авторизован
    }

    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;

    public interface MessageListener {
        void onMessageReceived(String message);
        void onConnectionStatusChanged(ConnectionState state);
        void onError(String errorMessage);
        void onAuthResult(boolean success, String message);
    }

    public ClientConnection(MessageListener listener) {
        this.messageListener = listener;
    }

    // Подключение к серверу
    public synchronized boolean connect(String host, int port, int timeoutMillis) {
        if (connectionState == ConnectionState.CONNECTING) {
            System.out.println("⚠️ Уже идет процесс подключения");
            return false;
        }

        if (isFullyConnected()) {
            System.out.println("⚠️ Уже подключено");
            return true;
        }

        connectionState = ConnectionState.CONNECTING;
        notifyConnectionStatusChanged();

        try {
            System.out.println("🔄 Подключение к серверу " + host + ":" + port);

            // Закрываем предыдущее соединение если есть
            closeResourcesSilently();

            // Установка таймаута подключения
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);

            // Устанавливаем таймаут чтения (большое значение)
            socket.setSoTimeout(300000); // 5 минут

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            socketConnected = true;
            running = true;
            connectionState = ConnectionState.SOCKET_CONNECTED;

            System.out.println("✅ TCP-соединение установлено");
            notifyConnectionStatusChanged();

            // Запускаем поток для чтения
            startReaderThread();

            // Запускаем keep-alive сообщения
            startKeepAlive();

            System.out.println("➡️ Готово к авторизации");
            return true;

        } catch (IOException e) {
            System.err.println("❌ Ошибка подключения к " + host + ":" + port + " - " + e.getMessage());

            if (messageListener != null) {
                Platform.runLater(() -> messageListener.onError("Ошибка подключения: " + e.getMessage()));
            }

            connectionState = ConnectionState.DISCONNECTED;
            socketConnected = false;
            notifyConnectionStatusChanged();
            closeResourcesSilently();
            return false;
        }
    }

    private void startKeepAlive() {
        if (keepAliveExecutor != null) {
            keepAliveExecutor.shutdownNow();
        }

        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor();
        keepAliveExecutor.scheduleAtFixedRate(() -> {
            if (isSocketConnected() && out != null) {
                try {
                    // Отправляем ping каждые 25 секунд
                    out.println("PING");
                    System.out.println("📤 Отправлен keep-alive PING");
                } catch (Exception e) {
                    System.err.println("Ошибка отправки keep-alive: " + e.getMessage());
                }
            }
        }, 5, 25, TimeUnit.SECONDS);
    }

    private void stopKeepAlive() {
        if (keepAliveExecutor != null) {
            keepAliveExecutor.shutdownNow();
            try {
                if (!keepAliveExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    keepAliveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                keepAliveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            keepAliveExecutor = null;
        }
    }

    private void startReaderThread() {
        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
        }

        readerThread = new Thread(this::readFromServer);
        readerThread.setDaemon(true);
        readerThread.setName("Server-Reader-Thread");
        readerThread.start();
    }

    private void readFromServer() {
        try {
            String response;
            while (running && (response = in.readLine()) != null) {
                System.out.println("📨 Сервер: " + response);

                // Обрабатываем PONG ответы
                if ("PONG".equals(response)) {
                    System.out.println("✅ Получен PONG от сервера");
                    continue;
                }

                processServerResponse(response);
            }
        } catch (SocketTimeoutException e) {
            System.err.println("⏰ Таймаут чтения от сервера");
            handleConnectionError("Таймаут соединения");
        } catch (IOException e) {
            if (running) {
                System.err.println("📛 Ошибка чтения от сервера: " + e.getMessage());
                handleConnectionError("Соединение потеряно: " + e.getMessage());
            }
        } finally {
            emergencyDisconnect();
        }
    }

    private void handleConnectionError(String error) {
        if (messageListener != null) {
            Platform.runLater(() -> messageListener.onError(error));
        }
    }

    private void processServerResponse(String response) {
        Platform.runLater(() -> processServerMessage(response));
    }

    // Обработка сообщений от сервера
    private void processServerMessage(String message) {
        if (message == null || message.isEmpty()) return;

        System.out.println("🔍 Обработка: " + message);

        // Приветственное сообщение от сервера
        if (message.contains("Добро пожаловать") || message.contains("Используйте команды")) {
            if (messageListener != null) {
                messageListener.onMessageReceived(message);
            }
            return;
        }

        String[] parts = message.split("\\" + ProtocolConstants.DELIMITER, 2);
        if (parts.length < 1) return;

        String responseType = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (responseType) {
            case ProtocolConstants.RESP_AUTH_SUCCESS:
                handleAuthSuccess(data);
                break;

            case ProtocolConstants.RESP_AUTH_FAILED:
                handleAuthFailed(data);
                break;

            case ProtocolConstants.RESP_OK:
                if (messageListener != null) {
                    messageListener.onMessageReceived("✅ " + data);
                }
                break;

            case ProtocolConstants.RESP_ERROR:
                if (messageListener != null) {
                    messageListener.onError("❌ " + data);
                }
                break;

            case ProtocolConstants.RESP_CHAT_LIST:
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
                break;

            case ProtocolConstants.RESP_NEW_MESSAGE:
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
                break;

            case ProtocolConstants.RESP_STATUS_UPDATE:
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
                break;

            case ProtocolConstants.RESP_ONLINE_USERS:
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
                break;

            case ProtocolConstants.RESP_CHAT_CREATED:
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
                requestChats(); // Обновляем список чатов
                break;

            case ProtocolConstants.RESP_CHAT_DELETED:
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
                requestChats(); // Обновляем список чатов
                break;

            default:
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
        }
    }

    private void handleAuthSuccess(String data) {
        String[] authParts = data.split("\\" + ProtocolConstants.DELIMITER);
        if (authParts.length > 0) {
            currentUser = authParts[0];
            connectionState = ConnectionState.AUTHENTICATED;
            notifyConnectionStatusChanged();
            if (messageListener != null) {
                messageListener.onAuthResult(true, "Авторизация успешна");
            }
            requestChats();
            System.out.println("✅ Авторизован как: " + currentUser);
        }
    }

    private void handleAuthFailed(String data) {
        connectionState = ConnectionState.SOCKET_CONNECTED; // Возвращаемся к состоянию "только TCP"
        notifyConnectionStatusChanged();
        if (messageListener != null) {
            messageListener.onAuthResult(false, data);
        }
        currentUser = null;
        System.out.println("❌ Ошибка авторизации: " + data);
    }

    // Метод для тихого закрытия ресурсов (без QUIT)
    private synchronized void closeResourcesSilently() {
        running = false;
        socketConnected = false;

        stopKeepAlive();

        try {
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.interrupt();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Игнорируем ошибки при закрытии
        } finally {
            socket = null;
            out = null;
            in = null;
            currentUser = null;
        }
    }

    // Нормальное отключение
    public synchronized void gracefulDisconnect() {
        if (isSocketConnected() && out != null) {
            sendCommand(ProtocolConstants.CMD_QUIT);
            try {
                Thread.sleep(100); // Даем время отправить команду
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        disconnect();
    }

    // Аварийное отключение
    private synchronized void emergencyDisconnect() {
        disconnect();
    }

    // Основное отключение
    private synchronized void disconnect() {
        if (connectionState == ConnectionState.DISCONNECTED) {
            return;
        }

        System.out.println("🔌 Отключение от сервера...");
        running = false;
        connectionState = ConnectionState.DISCONNECTED;
        socketConnected = false;

        stopKeepAlive();
        closeResourcesSilently();
        notifyConnectionStatusChanged();

        System.out.println("✅ Отключено от сервера");
    }

    // Проверка TCP-соединения
    public boolean isSocketConnected() {
        return socketConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    // Проверка полного подключения (TCP + авторизация)
    public boolean isFullyConnected() {
        return connectionState == ConnectionState.AUTHENTICATED && isSocketConnected();
    }

    // Проверка сети (только TCP)
    public boolean isNetworkConnected() {
        return connectionState.ordinal() >= ConnectionState.SOCKET_CONNECTED.ordinal() && isSocketConnected();
    }

    // Для обратной совместимости
    public boolean isConnected() {
        return isNetworkConnected();
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public String getConnectionStatusText() {
        switch (connectionState) {
            case DISCONNECTED:
                return "❌ Отключено";
            case CONNECTING:
                return "🔄 Подключение...";
            case SOCKET_CONNECTED:
                return "✅ Сеть: Готов к авторизации";
            case AUTHENTICATING:
                return "🔐 Авторизация...";
            case AUTHENTICATED:
                return "✅ Онлайн как: " + (currentUser != null ? currentUser : "?");
            default:
                return "❓ Неизвестно";
        }
    }

    private void sendCommand(String command) {
        if (out != null && isSocketConnected()) {
            out.println(command);
            System.out.println("📤 Отправка команды: " + command);
        } else {
            System.err.println("⚠️ Не могу отправить команду: нет соединения");
        }
    }

    private void notifyConnectionStatusChanged() {
        if (messageListener != null) {
            Platform.runLater(() ->
                    messageListener.onConnectionStatusChanged(connectionState));
        }
    }

    // === ПУБЛИЧНЫЕ МЕТОДЫ ДЛЯ ВЗАИМОДЕЙСТВИЯ ===

    // Аутентификация
    public void authenticate(String username, String password) {
        if (!isNetworkConnected()) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onAuthResult(false, "Нет сетевого подключения");
                }
            });
            return;
        }

        if (connectionState == ConnectionState.AUTHENTICATING) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onAuthResult(false, "Уже идет авторизация");
                }
            });
            return;
        }

        try {
            connectionState = ConnectionState.AUTHENTICATING;
            notifyConnectionStatusChanged();

            String command = ProtocolConstants.CMD_LOGIN + ProtocolConstants.DELIMITER +
                    username + ProtocolConstants.DELIMITER + password;
            out.println(command);
            System.out.println("📤 Авторизация: " + username);
        } catch (Exception e) {
            connectionState = ConnectionState.SOCKET_CONNECTED;
            notifyConnectionStatusChanged();
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onAuthResult(false, "Ошибка отправки: " + e.getMessage());
                }
            });
        }
    }

    // Регистрация
    public void register(String username, String password) {
        if (!isNetworkConnected()) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onAuthResult(false, "Нет сетевого подключения");
                }
            });
            return;
        }

        try {
            String command = ProtocolConstants.CMD_REGISTER + ProtocolConstants.DELIMITER +
                    username + ProtocolConstants.DELIMITER + password;
            out.println(command);
            System.out.println("📤 Регистрация: " + username);
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onAuthResult(false, "Ошибка отправки: " + e.getMessage());
                }
            });
        }
    }

    // Отправка сообщения (только если полностью подключен)
    public void sendMessageToChat(int chatId, String message) {
        if (!isFullyConnected()) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onError("Требуется авторизация для отправки сообщений");
                }
            });
            return;
        }

        try {
            String command = ProtocolConstants.CMD_SEND_MESSAGE + ProtocolConstants.DELIMITER +
                    chatId + ProtocolConstants.DELIMITER + message;
            out.println(command);
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onError("Ошибка отправки сообщения: " + e.getMessage());
                }
            });
        }
    }

    // Запрос списка чатов (только если полностью подключен)
    public void requestChats() {
        if (!isFullyConnected()) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onError("Требуется авторизация для получения чатов");
                }
            });
            return;
        }

        try {
            out.println(ProtocolConstants.CMD_GET_CHATS);
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onError("Ошибка запроса чатов: " + e.getMessage());
                }
            });
        }
    }

    // Создание чата (только если полностью подключен)
    public void createChat(String chatName, String participants) {
        if (!isFullyConnected()) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onError("Требуется авторизация для создания чата");
                    messageListener.onError("Текущий статус: " + getConnectionStatusText());
                }
            });
            return;
        }

        try {
            String command = ProtocolConstants.CMD_CREATE_CHAT + ProtocolConstants.DELIMITER +
                    chatName + ProtocolConstants.DELIMITER + participants;
            out.println(command);
            System.out.println("📤 Создание чата: " + chatName);
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onError("Ошибка создания чата: " + e.getMessage());
                }
            });
        }
    }

    // Обновление статуса
    public void updateStatus(String status) {
        if (!isFullyConnected()) return;

        try {
            String command = ProtocolConstants.CMD_UPDATE_STATUS + ProtocolConstants.DELIMITER + status;
            out.println(command);
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onError("Ошибка обновления статуса: " + e.getMessage());
                }
            });
        }
    }

    // Получение онлайн пользователей
    public void requestOnlineUsers() {
        if (!isFullyConnected()) return;

        try {
            out.println(ProtocolConstants.CMD_GET_ONLINE_USERS);
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onError("Ошибка запроса пользователей: " + e.getMessage());
                }
            });
        }
    }

    // Удаление чата
    public void deleteChat(int chatId) {
        if (!isFullyConnected()) return;

        try {
            String command = ProtocolConstants.CMD_DELETE_CHAT + ProtocolConstants.DELIMITER + chatId;
            out.println(command);
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (messageListener != null) {
                    messageListener.onError("Ошибка удаления чата: " + e.getMessage());
                }
            });
        }
    }

    // Выход
    public void logout() {
        if (!isNetworkConnected()) return;

        try {
            if (isFullyConnected()) {
                out.println(ProtocolConstants.CMD_LOGOUT);
            }
            currentUser = null;
            connectionState = ConnectionState.SOCKET_CONNECTED;
            notifyConnectionStatusChanged();
            System.out.println("👋 Выход из системы");
        } catch (Exception e) {
            // Игнорируем ошибки при выходе
        }
    }

    // Быстрое подключение с настройками по умолчанию
    public boolean connectToServer() {
        return connect(serverAddress, serverPort, 5000);
    }

    public boolean connectToServer(int timeoutMillis) {
        return connect(serverAddress, serverPort, timeoutMillis);
    }

    // Геттеры
    public String getCurrentUser() {
        return currentUser;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }


}