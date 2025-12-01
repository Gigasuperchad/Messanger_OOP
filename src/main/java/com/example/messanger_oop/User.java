package com.example.messanger_oop;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String nick;
    private List<Chat> chatList;
    public User(String nick, List<Chat> chats){
        this.nick = nick;
        this.chatList = chats;
    }

    public User(String nick) {
        this.nick = nick;
        this.chatList = new ArrayList<>();
    }

    public void add_chat(Chat chat){
        this.chatList.add(chat);
    }

    public String getNick() {
        return this.nick;
    }
}
