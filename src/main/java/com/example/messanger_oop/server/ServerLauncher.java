package com.example.messanger_oop.server;

public class ServerLauncher {
    public static void main(String[] args) {
        System.out.println("=== ЗАПУСК СЕРВЕРА МЕССЕНДЖЕРА ===");
        System.out.println("Текущая директория: " + System.getProperty("user.dir"));

        checkAndCreateDirectories();

        Server server = new Server();
        server.start();
    }

    private static void checkAndCreateDirectories() {
        String[] dirs = {
                "server_data",
                "server_data/users",
                "server_data/chats",
                "server_data/messages",
                "server_data/user_chats"
        };

        for (String dir : dirs) {
            java.io.File directory = new java.io.File(dir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Создана директория: " + dir);
                }
            }
        }
    }
}