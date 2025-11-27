package com.example.messanger_oop;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalRepository implements Repository {
    private List<Message> messages;

    public LocalRepository() {
        this.messages = new CopyOnWriteArrayList<>();
    }

    @Override
    public void saveMessage(String sender, String message, Date timestamp) {
        messages.add(new Message(sender, message, timestamp));
        if (messages.size() > 1000) {
            messages.remove(0);
        }
    }

    @Override
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    @Override
    public List<Message> getMessagesByUser(String username) {
        List<Message> userMessages = new ArrayList<>();
        for (Message message : messages) {
            if (message.getSender().equals(username)) {
                userMessages.add(message);
            }
        }
        return userMessages;
    }
}