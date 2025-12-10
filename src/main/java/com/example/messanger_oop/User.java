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
    private transient List<Chat> chatList;

    public User(String nick) {
        this.nick = nick;
        this.password = "";
        this.firstName = "Имя";
        this.lastName = "Фамилия";
        this.email = nick + "@example.com";
        this.avatarBase64 = null;
        this.chatList = new ArrayList<>();
    }

    public User(String nick, String password) {
        this.nick = nick;
        this.password = password;
        this.firstName = nick; // По умолчанию ник как имя
        this.lastName = "Пользователь";
        this.email = nick + "@example.com";
        this.avatarBase64 = null;
        this.chatList = new ArrayList<>();
    }

    public User(String nick, String password, String firstName, String lastName, String email) {
        this.nick = nick;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.avatarBase64 = null;
        this.chatList = new ArrayList<>();
    }

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
        if (this.chatList == null) {
            this.chatList = new ArrayList<>();
        }
        return this.chatList;
    }

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

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

    public void add_chat(Chat chat) {
        if (this.chatList == null) {
            this.chatList = new ArrayList<>();
        }
        if (!this.chatList.contains(chat)) {
            this.chatList.add(chat);
            System.out.println("Чат добавлен пользователю " + this.nick + ": " + chat.getChatName());
        }
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    @Override
    public String toString() {
        return getFullName() + " (" + nick + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return nick.equals(user.nick);
    }

    @Override
    public int hashCode() {
        return nick.hashCode();
    }
}