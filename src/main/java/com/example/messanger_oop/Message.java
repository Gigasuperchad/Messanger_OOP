package com.example.messanger_oop;

import java.util.Date;

public class Message {
    private String sender;
    private String content;
    private Date timestamp;

    public Message(String sender, String content, Date timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public Date getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + content;
    }
}