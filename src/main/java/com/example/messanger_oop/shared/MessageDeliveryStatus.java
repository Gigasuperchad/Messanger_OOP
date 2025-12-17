package com.example.messanger_oop.shared;

import java.io.Serializable;
import java.util.Date;

public class MessageDeliveryStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        SENDING("🕒 Отправляется"),
        SENT("✓ Отправлено"),
        DELIVERED("✓✓ Доставлено"),
        READ("👁️ Прочитано"),
        FAILED("❌ Ошибка");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return displayName.split(" ")[0];
        }
    }

    private Status status;
    private Date sentTime;
    private Date deliveredTime;
    private Date readTime;
    private String errorMessage;

    public MessageDeliveryStatus() {
        this.status = Status.SENDING;
        this.sentTime = new Date();
    }

    // Геттеры и сеттеры
    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        this.status = status;
        switch (status) {
            case SENT:
                this.sentTime = new Date();
                break;
            case DELIVERED:
                this.deliveredTime = new Date();
                break;
            case READ:
                this.readTime = new Date();
                break;
        }
    }

    public Date getSentTime() { return sentTime; }
    public void setSentTime(Date sentTime) { this.sentTime = sentTime; }

    public Date getDeliveredTime() { return deliveredTime; }
    public void setDeliveredTime(Date deliveredTime) { this.deliveredTime = deliveredTime; }

    public Date getReadTime() { return readTime; }
    public void setReadTime(Date readTime) { this.readTime = readTime; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getDisplayStatus() {
        if (status == Status.FAILED && errorMessage != null) {
            return status.getDisplayName() + ": " + errorMessage;
        }
        return status.getDisplayName();
    }

    public String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Статус: ").append(status.getDisplayName());

        if (sentTime != null) {
            sb.append("\nОтправлено: ").append(formatTime(sentTime));
        }
        if (deliveredTime != null) {
            sb.append("\nДоставлено: ").append(formatTime(deliveredTime));
        }
        if (readTime != null) {
            sb.append("\nПрочитано: ").append(formatTime(readTime));
        }
        if (errorMessage != null) {
            sb.append("\nОшибка: ").append(errorMessage);
        }

        return sb.toString();
    }

    private String formatTime(Date date) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy");
        return sdf.format(date);
    }
}