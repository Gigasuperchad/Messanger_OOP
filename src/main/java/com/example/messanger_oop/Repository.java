package com.example.messanger_oop;

import javafx.collections.ObservableList;
import java.util.List;

public interface Repository {
    void saveMessage(User sender, String message, Chat chat); // Исправлено
    ObservableList<Message> getMessages();
    List<Message> getMessagesByUser(String username);
    ObservableList<Chat> getChats();
    void add_chat(Chat chat);
    void send_msg(Chat chat, String message); // Добавлен новый метод
}