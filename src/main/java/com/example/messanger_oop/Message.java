package com.example.messanger_oop;

import java.util.Date;

public class Message {
    private User sender;
    private String content;
    private Date timestamp;

    public Message(User sender, String content, Date date) {
        this.sender = sender;
        this.content = content;
//        this.timestamp = timestamp;
    }

    public User getSender() { return sender; }
    public String getContent() { return content; }
    public Date getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + content;
    }
}