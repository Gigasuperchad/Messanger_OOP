package com.example.messanger_oop;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatusManager {
    private static final String STATUS_FILE = "server_data/user_status.dat";
    private static StatusManager instance;
    private Map<String, UserStatus> userStatuses;

    private StatusManager() {
        userStatuses = new ConcurrentHashMap<>();
        loadStatuses();
    }

    public static synchronized StatusManager getInstance() {
        if (instance == null) {
            instance = new StatusManager();
        }
        return instance;
    }

    private void loadStatuses() {
        try {
            File file = new File(STATUS_FILE);
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    userStatuses = (Map<String, UserStatus>) ois.readObject();
                    System.out.println("Loaded " + userStatuses.size() + " user statuses");
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading user statuses: " + e.getMessage());
            userStatuses = new ConcurrentHashMap<>();
        }
    }

    private void saveStatuses() {
        try {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STATUS_FILE))) {
                oos.writeObject(userStatuses);
            }
        } catch (IOException e) {
            System.err.println("Error saving user statuses: " + e.getMessage());
        }
    }

    public void setUserOnline(String username) {
        UserStatus status = userStatuses.computeIfAbsent(username, k -> new UserStatus(username));
        status.setStatus(UserStatus.Status.ONLINE);
        saveStatuses();
        System.out.println("User " + username + " is now ONLINE");
    }

    public void setUserOffline(String username) {
        UserStatus status = userStatuses.get(username);
        if (status != null) {
            status.setStatus(UserStatus.Status.OFFLINE);
            saveStatuses();
            System.out.println("User " + username + " is now OFFLINE");
        }
    }

    public void setUserStatus(String username, UserStatus.Status newStatus) {
        UserStatus status = userStatuses.computeIfAbsent(username, k -> new UserStatus(username));
        status.setStatus(newStatus);
        saveStatuses();
        System.out.println("User " + username + " status changed to " + newStatus);
    }

    public void setCustomStatusMessage(String username, String message) {
        UserStatus status = userStatuses.computeIfAbsent(username, k -> new UserStatus(username));
        status.setCustomMessage(message);
        saveStatuses();
        System.out.println("User " + username + " custom status message: " + message);
    }

    public UserStatus getUserStatus(String username) {
        return userStatuses.computeIfAbsent(username, k -> {
            UserStatus newStatus = new UserStatus(username);
            newStatus.setStatus(UserStatus.Status.OFFLINE);
            return newStatus;
        });
    }

    public boolean isUserOnline(String username) {
        UserStatus status = userStatuses.get(username);
        return status != null && status.isOnline();
    }

    public List<String> getOnlineUsers() {
        List<String> online = new ArrayList<>();
        for (Map.Entry<String, UserStatus> entry : userStatuses.entrySet()) {
            if (entry.getValue().isOnline()) {
                online.add(entry.getKey());
            }
        }
        return online;
    }

    public void updateLastSeen(String username) {
        UserStatus status = userStatuses.computeIfAbsent(username, k -> new UserStatus(username));
        status.setLastSeen(new Date());
        saveStatuses();
    }
}