package com.example.messanger_oop;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private User sender;
    private String content;
    private Date timestamp;
    private boolean edited;
    private String filePath;
    private String fileName;
    private String fileType;
    private long fileSize;
    private boolean hasAttachment;

    // Новые поля для статуса доставки
    private MessageDeliveryStatus deliveryStatus;
    private Map<String, Boolean> readBy; // Кто прочитал сообщение (username -> прочитано)

    public Message(User sender, String content, Date timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.edited = false;
        this.hasAttachment = false;
        this.filePath = null;
        this.fileName = null;
        this.fileType = null;
        this.fileSize = 0;
        this.deliveryStatus = new MessageDeliveryStatus();
        this.readBy = new HashMap<>();
    }

    // Конструктор для сообщения с файлом
    public Message(User sender, String content, Date timestamp,
                   String filePath, String fileName, String fileType, long fileSize) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.edited = false;
        this.hasAttachment = true;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.deliveryStatus = new MessageDeliveryStatus();
        this.readBy = new HashMap<>();
    }

    // Геттеры и сеттеры
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        this.hasAttachment = filePath != null;
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public boolean hasAttachment() { return hasAttachment; }
    public void setHasAttachment(boolean hasAttachment) { this.hasAttachment = hasAttachment; }

    // Остальные геттеры и сеттеры остаются без изменений
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    // Новые геттеры и сеттеры для статуса доставки
    public MessageDeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(MessageDeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Map<String, Boolean> getReadBy() { return readBy; }
    public void setReadBy(Map<String, Boolean> readBy) { this.readBy = readBy; }

    public void markAsRead(String username) {
        readBy.put(username, true);
        if (deliveryStatus != null) {
            deliveryStatus.setStatus(MessageDeliveryStatus.Status.READ);
        }
    }

    public boolean isReadBy(String username) {
        return readBy.getOrDefault(username, false);
    }

    public int getReadCount() {
        return (int) readBy.values().stream().filter(v -> v).count();
    }

    // Метод для форматирования размера файла
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " Б";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f КБ", fileSize / 1024.0);
        } else {
            return String.format("%.1f МБ", fileSize / (1024.0 * 1024.0));
        }
    }

    public String getShortFileInfo() {
        if (hasAttachment) {
            if (fileType.startsWith("image/")) {
                return "📷 Изображение: " + fileName + " (" + getFormattedFileSize() + ")";
            } else {
                return getFileIcon() + " " + fileName + " (" + getFormattedFileSize() + ")";
            }
        }
        return "";
    }

    // Метод для определения иконки файла по типу
    public String getFileIcon() {
        if (fileType == null) return "📄";

        if (fileType.startsWith("image/")) {
            return "🖼️";
        } else if (fileType.contains("pdf")) {
            return "📕";
        } else if (fileType.contains("word") || fileType.contains("document")) {
            return "📝";
        } else if (fileType.contains("excel") || fileType.contains("spreadsheet")) {
            return "📊";
        } else if (fileType.contains("zip") || fileType.contains("rar") || fileType.contains("archive")) {
            return "📦";
        } else if (fileType.contains("audio")) {
            return "🎵";
        } else if (fileType.contains("video")) {
            return "🎬";
        } else {
            return "📄";
        }
    }

    @Override
    public String toString() {
        String statusIcon = deliveryStatus != null ?
                deliveryStatus.getStatus().getIcon() + " " : "";

        if (hasAttachment) {
            return String.format("%s%s [Файл: %s (%s)] %s",
                    statusIcon, getFileIcon(), fileName, getFormattedFileSize(), content);
        }
        return statusIcon + content;
    }
}