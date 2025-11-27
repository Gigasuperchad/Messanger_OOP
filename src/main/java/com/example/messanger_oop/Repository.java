package com.example.messanger_oop;

import java.util.Date;
import java.util.List;

public interface Repository {
    void saveMessage(String sender, String message, Date timestamp);
    List<Message> getMessages();
    List<Message> getMessagesByUser(String username);
}

