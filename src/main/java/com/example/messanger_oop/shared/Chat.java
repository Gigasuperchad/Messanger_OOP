package com.example.messanger_oop.shared;

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
            return "Приватный чат";
        } else {
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

    public String getChatInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Название: ").append(chatName).append("\n");
        info.append("ID: ").append(id).append("\n");
        info.append("Сообщений: ").append(get_message_count()).append("\n");
        info.append("Участников: ").append(users.size()).append("\n");

        if (users.size() == 2) {
            info.append("Тип: Приватный чат\n");
        } else {
            info.append("Тип: Групповой чат\n");
        }

        return info.toString();
    }

    public List<String> getParticipantNames() {
        List<String> names = new ArrayList<>();
        for (User user : users) {
            names.add(user.getFullName() + " (" + user.getNick() + ")");
        }
        return names;
    }
}