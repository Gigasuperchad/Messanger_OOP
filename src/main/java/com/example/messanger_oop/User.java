package com.example.messanger_oop;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nick;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String avatarBase64;
    private List<Chat> chatList;

    // Старый конструктор для совместимости
    public User(String nick, List<Chat> chats) {
        this.nick = nick;
        this.chatList = chats != null ? chats : new ArrayList<>();
        this.password = "";
        this.firstName = "";
        this.lastName = "";
        this.email = "";
        this.avatarBase64 = null;
    }

    // Конструктор для регистрации
    public User(String nick, String password, String firstName, String lastName, String email) {
        this.nick = nick;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.avatarBase64 = null;
        this.chatList = new ArrayList<>();
    }

    // Конструктор с аватаром
    public User(String nick, String password, String firstName, String lastName,
                String email, String avatarBase64) {
        this.nick = nick;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.avatarBase64 = avatarBase64;
        this.chatList = new ArrayList<>();
    }

    // Конструктор для существующего кода
    public User(String nick) {
        this.nick = nick;
        this.password = "";
        this.firstName = "";
        this.lastName = "";
        this.email = "";
        this.avatarBase64 = null;
        this.chatList = new ArrayList<>();
    }

    // Геттеры
    public String getNick() {
        return this.nick;
    }

    public String getPassword() {
        return this.password;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getEmail() {
        return this.email;
    }

    public String getAvatarBase64() {
        return this.avatarBase64;
    }

    public List<Chat> getChatList() {
        return this.chatList;
    }

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    // Сеттеры
    public void setPassword(String password) {
        this.password = password;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAvatarBase64(String avatarBase64) {
        this.avatarBase64 = avatarBase64;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setChatList(List<Chat> chatList) {
        this.chatList = chatList;
    }

    // Метод для добавления чата
    public void add_chat(Chat chat) {
        if (this.chatList == null) {
            this.chatList = new ArrayList<>();
        }
        this.chatList.add(chat);
    }

    // Метод для проверки пароля
    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    // Упрощенный метод toJson
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"nick\":\"").append(escapeJson(nick)).append("\",");
        json.append("\"password\":\"").append(escapeJson(password)).append("\",");
        json.append("\"firstName\":\"").append(escapeJson(firstName)).append("\",");
        json.append("\"lastName\":\"").append(escapeJson(lastName)).append("\",");
        json.append("\"email\":\"").append(escapeJson(email)).append("\"");
        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
            json.append(",\"avatarBase64\":\"").append(escapeJson(avatarBase64)).append("\"");
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Статический метод для создания User из JSON
    public static User fromJson(String jsonStr) {
        try {
            // Упрощенный парсинг JSON
            String nick = extractValue(jsonStr, "nick");
            String password = extractValue(jsonStr, "password");
            String firstName = extractValue(jsonStr, "firstName");
            String lastName = extractValue(jsonStr, "lastName");
            String email = extractValue(jsonStr, "email");
            String avatarBase64 = extractValue(jsonStr, "avatarBase64");

            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                return new User(nick, password, firstName, lastName, email, avatarBase64);
            } else {
                return new User(nick, password, firstName, lastName, email);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;

        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) return null;

        String value = json.substring(startIndex, endIndex);
        // Раскодирование escape-последовательностей
        return value.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    @Override
    public String toString() {
        return getFullName() + " (" + nick + ")";
    }
}