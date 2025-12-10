package com.example.messanger_oop;

import java.net.Socket;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("🔍 Тестирование подключения к серверу...");

        try {
            Socket socket = new Socket("localhost", 12345);
            System.out.println("✅ Успешное подключение к серверу!");
            System.out.println("   Адрес: " + socket.getInetAddress());
            System.out.println("   Порт: " + socket.getPort());
            socket.close();
        } catch (Exception e) {
            System.err.println("❌ Не удалось подключиться: " + e.getMessage());
            System.out.println("\n🛠️ Возможные причины:");
            System.out.println("1. Сервер не запущен");
            System.out.println("2. Неправильный порт (должен быть 12345)");
            System.out.println("3. Фаервол блокирует подключение");
            System.out.println("4. Ошибка в адресе (используйте 'localhost' или '127.0.0.1')");
        }
    }
}