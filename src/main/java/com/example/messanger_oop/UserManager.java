package com.example.messanger_oop;

import java.io.*;
import java.util.HashMap;

public class UserManager {
    private final String FILE = "server_data/users.txt";
    private HashMap<String, String> users = new HashMap<>();
    private HashMap<String, User> userObjects = new HashMap<>();

    public UserManager() {
        load();
        System.out.println("👥 UserManager initialized. Total users: " + users.size());
    }

    private void load() {
        try {
            new File("server_data").mkdirs();

            File f = new File(FILE);
            if (!f.exists()) {
                System.out.println("User file not found, will create new");
                return;
            }

            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(":");
                if (p.length == 2) {
                    String login = p[0];
                    String password = p[1];
                    users.put(login, password);

                    User user = new User(login, password);
                    userObjects.put(login, user);
                    count++;
                }
            }
            br.close();
            System.out.println("Loaded " + count + " users from file");
        } catch (IOException e) {
            System.err.println("User loading error: " + e.getMessage());
        }
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            int count = 0;
            for (String login : users.keySet()) {
                pw.println(login + ":" + users.get(login));
                count++;
            }
            System.out.println("Saved " + count + " users to file");
        } catch (IOException e) {
            System.err.println("User saving error: " + e.getMessage());
        }
    }

    public boolean register(String login, String pass) {
        if (users.containsKey(login)) {
            System.out.println("User already exists: " + login);
            return false;
        }

        users.put(login, pass);

        User user = new User(login, pass);
        userObjects.put(login, user);

        save();
        System.out.println("New user registered: " + login);
        return true;
    }

    public boolean authenticate(String login, String pass) {
        boolean authenticated = users.containsKey(login) && users.get(login).equals(pass);
        if (authenticated) {
            System.out.println("Authentication successful: " + login);
        } else {
            System.out.println("Authentication failed for: " + login);
        }
        return authenticated;
    }

    public User getUser(String login) {
        User user = userObjects.get(login);
        if (user == null && users.containsKey(login)) {
            user = new User(login, users.get(login));
            userObjects.put(login, user);
            System.out.println("Created user object for: " + login);
        }
        return user;
    }

    public boolean userExists(String login) {
        return users.containsKey(login);
    }

    public int getUserCount() {
        return users.size();
    }

    public void updateUser(User user) {
        if (user != null && user.getNick() != null) {
            userObjects.put(user.getNick(), user);
            System.out.println("Updated user object: " + user.getNick());
        }
    }

    public HashMap<String, User> getAllUsers() {
        return new HashMap<>(userObjects);
    }
}