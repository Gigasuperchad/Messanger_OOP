package com.example.messanger_oop;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Chat implements Serializable {
    private static final long serialVersionUID = 1L;

    private static int nextId = 1;
    private int id;
    private List<User> users;
    private List<Message> messages;
    private String chatName;

    public Chat(List<User> users) {
        this.id = nextId++;
        this.users = users != null ? users : new ArrayList<>();
        this.messages = new ArrayList<>();
        this.chatName = generateChatName();
    }

    public Chat(List<User> users, String chatName) {
        this.id = nextId++;
        this.users = users != null ? users : new ArrayList<>();
        this.messages = new ArrayList<>();
        this.chatName = chatName;
    }

    private String generateChatName() {
        if (users.size() == 2) {
            // Для приватного чата
            return "Приватный чат";
        } else {
            // Для группового чата
            StringBuilder sb = new StringBuilder("Групповой чат: ");
            for (int i = 0; i < Math.min(users.size(), 3); i++) {
                sb.append(users.get(i).getNick());
                if (i < Math.min(users.size(), 3) - 1) {
                    sb.append(", ");
                }
            }
            if (users.size() > 3) {
                sb.append("...");
            }
            return sb.toString();
        }
    }

    // Геттеры
    public int getId() {
        return id;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public String getChatName() {
        return chatName;
    }

    // Сеттеры
    public void setId(int id) {
        this.id = id;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    // Методы для работы с чатом
    public void send_message(Message message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }

    public void add_user(User user) {
        if (users == null) {
            users = new ArrayList<>();
        }
        if (!users.contains(user)) {
            users.add(user);
        }
    }

    public void remove_user(User user) {
        if (users != null) {
            users.remove(user);
        }
    }

    public boolean has_user(User user) {
        return users != null && users.contains(user);
    }

    public int get_message_count() {
        return messages != null ? messages.size() : 0;
    }

    public Message get_last_message() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(messages.size() - 1);
        }
        return null;
    }

    @Override
    public String toString() {
        return chatName + " (" + get_message_count() + " сообщений)";
    }
}