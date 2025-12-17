package com.example.messanger_oop.shared;

import java.io.Serializable;
import java.util.Date;

public class MessageDeliveryStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        SENDING("🔄 Отправляется"),      // Сообщение отправляется
        SENT("✅ Отправлено"),           // Сообщение отправлено на сервер
        DELIVERED("✓ Доставлено"),       // Сообщение доставлено получателю
        READ("👁️ Прочитано"),           // Сообщение прочитано
        FAILED("❌ Ошибка"),              // Ошибка при отправке
        PENDING("⏳ Ожидает отправки");  // Сообщение в очереди (если нет сети)

        private final String display;

        Status(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }

        public String getIcon() {
            return display.split(" ")[0];
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
            return status.getDisplay() + ": " + errorMessage;
        }
        return status.getDisplay();
    }

    public String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Статус: ").append(status.getDisplay());

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