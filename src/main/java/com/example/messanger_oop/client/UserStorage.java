package com.example.messanger_oop.client;

import com.example.messanger_oop.shared.User;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserStorage {
    private static final String USERS_DIR = "users_data";
    private static final String CURRENT_USER_FILE = "current_user.dat";

    public static void saveUser(User user) {
        System.out.println("\nСОХРАНЕНИЕ ПОЛЬЗОВАТЕЛЯ В UserStorage:");
        System.out.println("   Логин: " + user.getNick());
        System.out.println("   Имя: " + user.getFirstName());
        System.out.println("   Пароль: " + user.getPassword());

        try {
            Path dir = Paths.get(USERS_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                System.out.println("Создана директория: " + USERS_DIR);
            }

            String filename = USERS_DIR + "/" + user.getNick() + ".dat";
            System.out.println("Путь к файлу: " + filename);

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(filename))) {
                oos.writeObject(user);
                System.out.println("Пользователь сохранен в файл");
            }

            // Сохраняем как текущего пользователя
            saveCurrentUser(user);

            System.out.println("Сохранение завершено успешно!");

        } catch (IOException e) {
            System.err.println("Ошибка сохранения пользователя: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void saveCurrentUser(User user) {
        System.out.println("\nСОХРАНЕНИЕ ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ:");
        System.out.println("   Логин: " + user.getNick());

        try {
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(CURRENT_USER_FILE))) {
                oos.writeObject(user);
                System.out.println("Текущий пользователь сохранен в файл: " + CURRENT_USER_FILE);
            }
        } catch (IOException e) {
            System.err.println("Ошибка сохранения текущего пользователя: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static User loadUser(String nickname) {
        System.out.println("\nЗАГРУЗКА ПОЛЬЗОВАТЕЛЯ ИЗ UserStorage:");
        System.out.println("   Искомый ник: " + nickname);

        try {
            String filename = USERS_DIR + "/" + nickname + ".dat";
            System.out.println("Путь к файлу: " + filename);

            if (Files.exists(Path.of(filename))) {
                System.out.println("Файл существует");

                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(filename))) {
                    User user = (User) ois.readObject();
                    System.out.println("   👤 Пользователь загружен:");
                    System.out.println("      Логин: " + user.getNick());
                    System.out.println("      Имя: " + user.getFirstName());
                    System.out.println("      Пароль: " + user.getPassword());
                    return user;
                }
            } else {
                System.out.println("Файл не найден");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка загрузки пользователя: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static User getCurrentUser() {
        System.out.println("\nПОЛУЧЕНИЕ ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ:");

        try {
            if (Files.exists(Path.of(CURRENT_USER_FILE))) {
                System.out.println("Файл текущего пользователя существует");

                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(CURRENT_USER_FILE))) {
                    User user = (User) ois.readObject();
                    System.out.println("Текущий пользователь загружен: " + user.getNick());
                    return user;
                }
            } else {
                System.out.println("Файл текущего пользователя не найден");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка загрузки текущего пользователя: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static boolean userExists(String nickname) {
        String filename = USERS_DIR + "/" + nickname + ".dat";
        boolean exists = Files.exists(Path.of(filename));

        System.out.println("\nПРОВЕРКА СУЩЕСТВОВАНИЯ ПОЛЬЗОВАТЕЛЯ:");
        System.out.println("   Логин: " + nickname);
        System.out.println("   Файл: " + filename);
        System.out.println("   Существует: " + exists);

        return exists;
    }

    public static List<User> getAllUsers() {
        System.out.println("\nПОЛУЧЕНИЕ ВСЕХ ПОЛЬЗОВАТЕЛЕЙ:");

        List<User> users = new ArrayList<>();
        try {
            Path dir = Paths.get(USERS_DIR);
            if (Files.exists(dir)) {
                System.out.println("Директория существует: " + USERS_DIR);

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.dat")) {
                    int count = 0;
                    for (Path file : stream) {
                        try (ObjectInputStream ois = new ObjectInputStream(
                                new FileInputStream(file.toFile()))) {
                            User user = (User) ois.readObject();
                            users.add(user);
                            count++;
                            System.out.println("" + user.getNick() + " - " + user.getFullName());
                        }
                    }
                    System.out.println("Всего пользователей: " + count);
                }
            } else {
                System.out.println("Директория не существует: " + USERS_DIR);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка получения списка пользователей: " + e.getMessage());
            e.printStackTrace();
        }

        return users;
    }

    public static boolean emailExists(String email) {
        try {
            File storageDir = new File("users_data");
            if (!storageDir.exists()) {
                return false;
            }

            File[] userFiles = storageDir.listFiles((dir, name) -> name.endsWith(".dat"));
            if (userFiles == null) {
                return false;
            }

            for (File userFile : userFiles) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(userFile))) {
                    User user = (User) ois.readObject();
                    if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(email)) {
                        return true;
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка чтения пользователя: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка проверки email: " + e.getMessage());
        }
        return false;
    }

    public static void clearCurrentUser() {
        System.out.println("\nОЧИСТКА ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ");

        try {
            Files.deleteIfExists(Path.of(CURRENT_USER_FILE));
            System.out.println("Текущий пользователь удален");
        } catch (IOException e) {
            System.err.println("Ошибка удаления текущего пользователя: " + e.getMessage());
        }
    }
}