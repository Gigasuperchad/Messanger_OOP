package com.example.messanger_oop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class Chat {
    private List<User> users;
    private ObservableList<Message> messages;
    private static int idCounter = 0;
    private int id;

    public Chat(List<User> users) {
        this.users = users;
        this.messages = FXCollections.observableArrayList();
        this.id = ++idCounter;
    }

    public void send_message(Message message) {
        messages.add(message);
    }

    public List<User> getUsers() {
        return users;
    }

    public ObservableList<Message> getMessages() {
        return messages;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        if (users.size() > 1) {
            return "Чат с " + users.get(0).getNick() + " и другими";
        } else if (users.size() == 1) {
            return "Чат с " + users.get(0).getNick();
        }
        return "Пустой чат";
    }
}