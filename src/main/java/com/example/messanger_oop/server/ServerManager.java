package com.example.messanger_oop.server;

import com.example.messanger_oop.shared.*;

import java.util.*;
import java.util.concurrent.*;

public class ServerManager {
    private UserManager userManager;
    private ChatManager chatManager;
    private StatusManager statusManager;
    private ConcurrentHashMap<String, ClientHandler> connectedClients;

    public ServerManager() {
        this.userManager = new UserManager();
        this.chatManager = new ChatManager();
        this.statusManager = StatusManager.getInstance();
        this.connectedClients = new ConcurrentHashMap<>();

        System.out.println("ServerManager initialized:");
        System.out.println("  Users: " + userManager.getUserCount());
        System.out.println("  Chats: " + chatManager.getTotalChatCount());
    }

    // Аутентификация пользователя
    public String authenticate(String username, String password) {
        if (userManager.authenticate(username, password)) {
            statusManager.setUserOnline(username);
            return ProtocolConstants.RESP_AUTH_SUCCESS + ProtocolConstants.DELIMITER + username;
        } else {
            return ProtocolConstants.RESP_AUTH_FAILED + ProtocolConstants.DELIMITER + "Неверный логин или пароль";
        }
    }

    // Регистрация пользователя
    public String register(String username, String password) {
        // Если пользователь уже существует, возвращаем успех (а не ошибку)
        if (userManager.userExists(username)) {
            System.out.println("✅ Пользователь уже существует (авто-синхронизация): " + username);
            return ProtocolConstants.RESP_OK + ProtocolConstants.DELIMITER + "Пользователь уже зарегистрирован";
        }

        if (userManager.register(username, password)) {
            System.out.println("✅ Новый пользователь зарегистрирован: " + username);
            return ProtocolConstants.RESP_OK + ProtocolConstants.DELIMITER + "Регистрация успешна";
        } else {
            return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Ошибка регистрации";
        }
    }

    // Получение списка чатов пользователя
    public String getUserChats(String username) {
        List<Chat> chats = chatManager.getUserChats(username);
        StringBuilder response = new StringBuilder();
        response.append(ProtocolConstants.RESP_CHAT_LIST);

        for (Chat chat : chats) {
            response.append(ProtocolConstants.LIST_DELIMITER);
            response.append(chat.getId()).append(ProtocolConstants.FIELD_DELIMITER);
            response.append(chat.getChatName()).append(ProtocolConstants.FIELD_DELIMITER);
            response.append(chat.get_message_count()).append(ProtocolConstants.FIELD_DELIMITER);
            response.append(chat.getUsers().size());
        }

        return response.toString();
    }

    // Отправка сообщения в чат
    public String sendMessage(String username, int chatId, String messageContent) {
        try {
            Chat chat = chatManager.getChat(chatId);
            if (chat == null) {
                return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Чат не найден";
            }

            User sender = userManager.getUser(username);
            if (sender == null) {
                return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Пользователь не найден";
            }

            Message message = new Message(sender, messageContent, new Date());
            chat.send_message(message);
            chatManager.saveChat(chat);

            // Рассылка сообщения всем участникам чата
            String notification = ProtocolConstants.RESP_NEW_MESSAGE + ProtocolConstants.DELIMITER +
                    chatId + ProtocolConstants.FIELD_DELIMITER +
                    username + ProtocolConstants.FIELD_DELIMITER +
                    messageContent + ProtocolConstants.FIELD_DELIMITER +
                    message.getTimestamp().getTime();

            for (User participant : chat.getUsers()) {
                ClientHandler handler = connectedClients.get(participant.getNick());
                if (handler != null && !participant.getNick().equals(username)) {
                    handler.sendMessage(notification);
                }
            }

            return ProtocolConstants.RESP_OK + ProtocolConstants.DELIMITER + "Сообщение отправлено";
        } catch (Exception e) {
            return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + e.getMessage();
        }
    }

    // Создание нового чата
    public String createChat(String creatorUsername, String chatName, String participantsStr) {
        try {
            User creator = userManager.getUser(creatorUsername);
            if (creator == null) {
                return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Создатель не найден";
            }

            List<User> participants = new ArrayList<>();
            participants.add(creator);

            String[] participantNicks = participantsStr.split(",");
            for (String nick : participantNicks) {
                nick = nick.trim();
                if (!nick.isEmpty() && !nick.equals(creatorUsername)) {
                    User participant = userManager.getUser(nick);
                    if (participant != null) {
                        participants.add(participant);
                    }
                }
            }

            if (participants.size() < 2) {
                return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Недостаточно участников";
            }

            Chat newChat = chatManager.createChat(participants, chatName);

            // Добавляем приветственное сообщение
            Message welcomeMessage = new Message(creator,
                    "Чат \"" + chatName + "\" создан! Добро пожаловать!", new Date());
            newChat.send_message(welcomeMessage);
            chatManager.saveChat(newChat);

            // Уведомляем всех участников
            String notification = ProtocolConstants.RESP_CHAT_CREATED + ProtocolConstants.DELIMITER +
                    newChat.getId() + ProtocolConstants.FIELD_DELIMITER +
                    chatName;

            for (User participant : participants) {
                ClientHandler handler = connectedClients.get(participant.getNick());
                if (handler != null) {
                    handler.sendMessage(notification);
                    handler.sendMessage(getUserChats(participant.getNick()));
                }
            }

            return ProtocolConstants.RESP_OK + ProtocolConstants.DELIMITER +
                    "Чат создан: " + newChat.getId();
        } catch (Exception e) {
            return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + e.getMessage();
        }
    }

    // Обновление статуса пользователя
    public String updateStatus(String username, String status) {
        try {
            switch (status) {
                case ProtocolConstants.USER_STATUS_ONLINE:
                    statusManager.setUserOnline(username);
                    break;
                case ProtocolConstants.USER_STATUS_OFFLINE:
                    statusManager.setUserOffline(username);
                    break;
                case ProtocolConstants.USER_STATUS_AWAY:
                    statusManager.setUserStatus(username, UserStatus.Status.AWAY);
                    break;
                case ProtocolConstants.USER_STATUS_DND:
                    statusManager.setUserStatus(username, UserStatus.Status.DO_NOT_DISTURB);
                    break;
                case ProtocolConstants.USER_STATUS_INVISIBLE:
                    statusManager.setUserStatus(username, UserStatus.Status.INVISIBLE);
                    break;
                default:
                    return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Неизвестный статус";
            }

            // Рассылка обновления статуса
            String statusUpdate = ProtocolConstants.RESP_STATUS_UPDATE + ProtocolConstants.DELIMITER +
                    username + ProtocolConstants.FIELD_DELIMITER + status;

            broadcast(statusUpdate, username);

            return ProtocolConstants.RESP_OK + ProtocolConstants.DELIMITER + "Статус обновлен";
        } catch (Exception e) {
            return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + e.getMessage();
        }
    }

    // Получение онлайн пользователей
    public String getOnlineUsers() {
        List<String> onlineUsers = statusManager.getOnlineUsers();
        StringBuilder response = new StringBuilder();
        response.append(ProtocolConstants.RESP_ONLINE_USERS);

        for (String user : onlineUsers) {
            UserStatus status = statusManager.getUserStatus(user);
            response.append(ProtocolConstants.LIST_DELIMITER);
            response.append(user).append(ProtocolConstants.FIELD_DELIMITER);
            response.append(status.getStatus().name()).append(ProtocolConstants.FIELD_DELIMITER);
            response.append(status.getCustomMessage());
        }

        return response.toString();
    }

    // Удаление чата
    public String deleteChat(String username, int chatId) {
        try {
            Chat chat = chatManager.getChat(chatId);
            if (chat == null) {
                return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Чат не найден";
            }

            // Проверка прав
            boolean hasAccess = false;
            for (User user : chat.getUsers()) {
                if (user.getNick().equals(username)) {
                    hasAccess = true;
                    break;
                }
            }

            if (!hasAccess) {
                return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Нет доступа к чату";
            }

            boolean deleted = chatManager.deleteChat(chatId);
            if (deleted) {
                // Уведомляем всех участников
                String notification = ProtocolConstants.RESP_CHAT_DELETED + ProtocolConstants.DELIMITER +
                        chatId + ProtocolConstants.FIELD_DELIMITER +
                        chat.getChatName();

                for (User participant : chat.getUsers()) {
                    ClientHandler handler = connectedClients.get(participant.getNick());
                    if (handler != null) {
                        handler.sendMessage(notification);
                    }
                }

                return ProtocolConstants.RESP_OK + ProtocolConstants.DELIMITER + "Чат удален";
            } else {
                return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + "Ошибка удаления";
            }
        } catch (Exception e) {
            return ProtocolConstants.RESP_ERROR + ProtocolConstants.DELIMITER + e.getMessage();
        }
    }

    // Регистрация клиента
    public void registerClient(String username, ClientHandler handler) {
        connectedClients.put(username, handler);
        System.out.println("✅ Клиент зарегистрирован: " + username);
        System.out.println("   Всего подключено: " + connectedClients.size());
    }

    // Удаление клиента
    public void unregisterClient(String username) {
        connectedClients.remove(username);
        statusManager.setUserOffline(username);
        System.out.println("❌ Клиент отключен: " + username);
        System.out.println("   Осталось подключено: " + connectedClients.size());
    }

    // Рассылка сообщения всем, кроме указанного пользователя
    public void broadcast(String message, String excludeUser) {
        for (Map.Entry<String, ClientHandler> entry : connectedClients.entrySet()) {
            if (!entry.getKey().equals(excludeUser)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    // Геттеры
    public UserManager getUserManager() { return userManager; }
    public ChatManager getChatManager() { return chatManager; }
    public StatusManager getStatusManager() { return statusManager; }
    public boolean isUserConnected(String username) {
        return connectedClients.containsKey(username);
    }
}