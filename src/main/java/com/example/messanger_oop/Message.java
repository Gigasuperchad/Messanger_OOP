package com.example.messanger_oop;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private User sender;
    private String content;
    private Date timestamp;

    public Message(User sender, String content, Date timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content != null ? content : "";
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        String senderName = (sender != null && sender.getNick() != null) ?
                sender.getNick() : "Неизвестный";
        String time = (timestamp != null) ? timestamp.toString() : "Неизвестное время";
        return "[" + time + "] " + senderName + ": " + content;
    }
}