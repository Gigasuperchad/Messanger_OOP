package com.example.messanger_oop;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserStorage {
    private static final String USERS_DIR = "users_data";
    private static final String CURRENT_USER_FILE = "current_user.dat";

    // Сохраняем пользователя в файл
    public static void saveUser(User user) {
        try {
            // Создаем директорию если ее нет
            Path dir = Paths.get(USERS_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // Сохраняем пользователя в отдельный файл
            String filename = USERS_DIR + "/" + user.getNick() + ".dat";
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(filename))) {
                oos.writeObject(user);
                System.out.println("Пользователь сохранен в файл: " + filename);
            }

            // Сохраняем как текущего пользователя
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(CURRENT_USER_FILE))) {
                oos.writeObject(user);
                System.out.println("Текущий пользователь сохранен");
            }

        } catch (IOException e) {
            System.err.println("Ошибка сохранения пользователя: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Загружаем пользователя по нику
    public static User loadUser(String nickname) {
        try {
            String filename = USERS_DIR + "/" + nickname + ".dat";
            if (Files.exists(Path.of(filename))) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(filename))) {
                    User user = (User) ois.readObject();
                    System.out.println("Пользователь загружен: " + user.getNick());
                    return user;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка загрузки пользователя: " + e.getMessage());
        }
        return null;
    }

    // Получаем текущего пользователя
    public static User getCurrentUser() {
        try {
            if (Files.exists(Path.of(CURRENT_USER_FILE))) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(CURRENT_USER_FILE))) {
                    User user = (User) ois.readObject();
                    System.out.println("Текущий пользователь загружен: " + user.getNick());
                    return user;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка загрузки текущего пользователя: " + e.getMessage());
        }
        return null;
    }

    // Проверяем, существует ли пользователь
    public static boolean userExists(String nickname) {
        String filename = USERS_DIR + "/" + nickname + ".dat";
        boolean exists = Files.exists(Path.of(filename));
        System.out.println("Проверка пользователя " + nickname + ": " + (exists ? "существует" : "не существует"));
        return exists;
    }

    // Получаем список всех пользователей
    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try {
            Path dir = Paths.get(USERS_DIR);
            if (Files.exists(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.dat")) {
                    for (Path file : stream) {
                        try (ObjectInputStream ois = new ObjectInputStream(
                                new FileInputStream(file.toFile()))) {
                            users.add((User) ois.readObject());
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка получения списка пользователей: " + e.getMessage());
        }
        return users;
    }

    // Удаляем текущего пользователя (для выхода)
    public static void clearCurrentUser() {
        try {
            Files.deleteIfExists(Path.of(CURRENT_USER_FILE));
            System.out.println("Текущий пользователь удален");
        } catch (IOException e) {
            System.err.println("Ошибка удаления текущего пользователя: " + e.getMessage());
        }
    }
}