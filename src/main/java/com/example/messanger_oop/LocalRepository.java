package com.example.messanger_oop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;

public class LocalRepository implements Repository {
    private ObservableList<Message> Messages;
    private ObservableList<Chat> Chats;
    private User currentUser; // Текущий пользователь

    public LocalRepository() {
        Chats = FXCollections.observableArrayList();
        Messages = FXCollections.observableArrayList();
        currentUser = new User("CurrentUser"); // Заглушка
    }

    @Override
    public void saveMessage(User sender, String message, Chat chat) {
        chat.send_message(new Message(sender, message, new Date()));
    }

    @Override
    public void send_msg(Chat chat, String message) {
        saveMessage(currentUser, message, chat);
    }

    @Override
    public ObservableList<Message> getMessages() {
        return Messages;
    }

    @Override
    public List<Message> getMessagesByUser(String username) {
        List<Message> userMessages = new ArrayList<>();
        for (Message message : Messages) {
            if (message.getSender().getNick().equals(username)) {
                userMessages.add(message);
            }
        }
        return userMessages;
    }

    @Override
    public ObservableList<Chat> getChats() {
        return Chats;
    }

    @Override
    public void add_chat(Chat chat) {
        for (User user : chat.getUsers()) {
            user.add_chat(chat);
        }
        Chats.add(chat);
    }
}