package com.example.messanger_oop;

import java.io.Serializable;
import java.util.Date;

public class UserStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        ONLINE("🟢 В сети"),
        OFFLINE("⚫ Не в сети"),
        AWAY("🟡 Отошел"),
        DO_NOT_DISTURB("🔴 Не беспокоить"),
        INVISIBLE("👻 Невидимый");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            // Извлекаем иконку (первый символ до пробела)
            String[] parts = displayName.split(" ", 2);
            return parts.length > 0 ? parts[0] : displayName;
        }
    }

    private String username;
    private Status status;
    private Date lastSeen;
    private String customMessage;

    public UserStatus(String username) {
        this.username = username;
        this.status = Status.OFFLINE;
        this.lastSeen = new Date();
        this.customMessage = "";
    }

    public UserStatus(String username, Status status) {
        this.username = username;
        this.status = status;
        this.lastSeen = new Date();
        this.customMessage = "";
    }

    // Геттеры и сеттеры
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        this.status = status;
        this.lastSeen = new Date();
    }

    public Date getLastSeen() { return lastSeen; }
    public void setLastSeen(Date lastSeen) { this.lastSeen = lastSeen; }

    public String getCustomMessage() { return customMessage; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }

    // Метод для получения иконки статуса
    public String getIcon() {
        return status.getIcon();
    }

    public boolean isOnline() {
        return status == Status.ONLINE;
    }

    public String getStatusDisplay() {
        if (status == Status.OFFLINE) {
            long minutesAgo = (new Date().getTime() - lastSeen.getTime()) / (1000 * 60);
            if (minutesAgo < 1) return "⚫ Только что";
            if (minutesAgo < 60) return "⚫ Был(а) " + minutesAgo + " мин. назад";
            long hoursAgo = minutesAgo / 60;
            if (hoursAgo < 24) return "⚫ Был(а) " + hoursAgo + " ч. назад";
            return "⚫ Был(а) давно";
        }
        return status.getDisplayName();
    }

    @Override
    public String toString() {
        return getStatusDisplay();
    }
}