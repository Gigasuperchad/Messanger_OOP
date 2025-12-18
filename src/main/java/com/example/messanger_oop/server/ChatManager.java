package com.example.messanger_oop.server;

import com.example.messanger_oop.shared.Chat;
import com.example.messanger_oop.shared.Message;
import com.example.messanger_oop.shared.User;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {
    private static final String CHATS_DIR = "server_data/chats";
    private static final String USER_CHATS_DIR = "server_data/user_chats";
    private Map<Integer, Chat> chatsCache = new ConcurrentHashMap<>();
    private Map<String, List<Integer>> userChatsMap = new ConcurrentHashMap<>();
    private static int nextId = 1;

    public ChatManager() {
        try {
            Files.createDirectories(Paths.get(CHATS_DIR));
            Files.createDirectories(Paths.get(USER_CHATS_DIR));
        } catch (IOException e) {
            System.err.println("Ошибка создания директорий: " + e.getMessage());
        }
        loadAllChats();
    }

    private synchronized int generateId() {
        return nextId++;
    }

    private void loadAllChats() {
        File dir = new File(CHATS_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".dat"));
        if (files == null) {
            System.out.println("No chat files found in " + CHATS_DIR);
            return;
        }

        System.out.println("Loading " + files.length + " chats from com.example.messanger_oop.server storage...");
        for (File f : files) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Chat chat = (Chat) ois.readObject();
                chatsCache.put(chat.getId(), chat);

                for (User u : chat.getUsers()) {
                    userChatsMap.computeIfAbsent(u.getNick(), k -> new ArrayList<>());
                    List<Integer> userChats = userChatsMap.get(u.getNick());
                    if (!userChats.contains(chat.getId())) {
                        userChats.add(chat.getId());
                        System.out.println("   Registered chat " + chat.getId() + " for user " + u.getNick());
                    }
                }

                if (chat.getId() >= nextId) {
                    nextId = chat.getId() + 1;
                }
                System.out.println("   Loaded chat: " + chat.getChatName() +
                        " (ID: " + chat.getId() +
                        ", users: " + chat.getUsers().size() + ")");
            } catch (Exception e) {
                System.err.println("   Error loading chat from " + f.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("✅ Total loaded chats: " + chatsCache.size());
        System.out.println("   Users with chats: " + userChatsMap.size());
    }

    public synchronized Chat createChat(List<User> users, String chatName) {
        System.out.println("\n🎯 CREATING CHAT:");
        System.out.println("   Name: " + chatName);
        System.out.println("   Participants: " + users.size());

        // Проверяем существующие приватные чаты
        if (users.size() == 2) {
            String user1 = users.get(0).getNick();
            String user2 = users.get(1).getNick();
            Chat existing = findPrivateChat(user1, user2);
            if (existing != null) {
                System.out.println("⚠️ Private chat already exists: " + existing.getChatName());
                return existing;
            }
        }

        Chat chat = new Chat(users, chatName);
        chat.setId(generateId());

        // Добавляем приветственное сообщение
        if (!users.isEmpty()) {
            User firstUser = users.get(0);
            Message welcomeMessage = new Message(firstUser,
                    "Чат \"" + chatName + "\" создан! Добро пожаловать!", new Date());
            chat.send_message(welcomeMessage);
        }

        saveChat(chat);

        // Регистрируем чат для ВСЕХ участников
        for (User u : users) {
            userChatsMap.computeIfAbsent(u.getNick(), k -> new ArrayList<>());
            List<Integer> lst = userChatsMap.get(u.getNick());
            if (!lst.contains(chat.getId())) {
                lst.add(chat.getId());
                System.out.println("   ✅ Registered chat " + chat.getId() + " for user " + u.getNick());
            }
            saveUserChats(u.getNick(), lst);
        }

        chatsCache.put(chat.getId(), chat);
        System.out.println("✅ Chat created: " + chat.getChatName() +
                " (ID " + chat.getId() +
                ", users: " + users.size() + ")");
        return chat;
    }

    public void saveChat(Chat chat) {
        String filename = CHATS_DIR + "/chat_" + chat.getId() + ".dat";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(chat);
            System.out.println("✅ Chat saved: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Chat saving error: " + e.getMessage());
        }
    }

    private void saveUserChats(String username, List<Integer> chatIds) {
        String filename = CHATS_DIR + "/user_" + username + "_chats.dat";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(chatIds);
            System.out.println("✅ User chat list saved for " + username + ": " + chatIds.size() + " chats");
        } catch (IOException e) {
            System.err.println("❌ User chat list saving error: " + e.getMessage());
        }
    }

    public Chat getChat(int chatId) {
        Chat chat = chatsCache.get(chatId);
        if (chat == null) {
            System.out.println("❌ Chat not found ID: " + chatId);
        }
        return chat;
    }

    private List<Integer> loadUserChats(String username) {
        String filename = CHATS_DIR + "/user_" + username + "_chats.dat";
        File file = new File(filename);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (List<Integer>) ois.readObject();
            } catch (Exception e) {
                System.err.println("❌ Error loading user chats for " + username + ": " + e.getMessage());
            }
        }
        return new ArrayList<>();
    }

    public List<Chat> getUserChats(String username) {
        System.out.println("\n🔍 Searching chats for user: " + username);

        // Проверяем кэш
        List<Integer> ids = userChatsMap.getOrDefault(username, new ArrayList<>());

        // Если нет в кэше, загружаем из файла
        if (ids.isEmpty()) {
            ids = loadUserChats(username);
            if (!ids.isEmpty()) {
                userChatsMap.put(username, ids);
            }
        }

        System.out.println("   Found chat IDs: " + ids.size() + " -> " + ids);

        if (ids.isEmpty()) {
            System.out.println("   No chats found for user " + username);
            return new ArrayList<>();
        }

        List<Chat> result = new ArrayList<>();
        for (int id : ids) {
            Chat c = getChat(id);
            if (c != null) {
                result.add(c);
                System.out.println("   ✅ Added chat: " + c.getChatName() + " (ID: " + id + ")");
            } else {
                System.out.println("   ❌ Chat not found ID: " + id);
            }
        }

        System.out.println("✅ Total chats for " + username + ": " + result.size());
        return result;
    }

    public void addMessageToChat(int chatId, Message message) {
        Chat chat = getChat(chatId);
        if (chat != null) {
            chat.send_message(message);
            saveChat(chat);
            System.out.println("Message added to chat " + chat.getChatName());
        } else {
            System.err.println("Chat not found: " + chatId);
        }
    }

    public void addMessage(String chatName, String fullMsg) {
        Chat global = null;
        for (Chat c : chatsCache.values()) {
            if (chatName.equals("global") && "global".equals(c.getChatName())) {
                global = c;
                break;
            }
        }
        if (global == null) {
            global = new Chat(new ArrayList<>(), "global");
            global.setId(generateId());
            saveChat(global);
            chatsCache.put(global.getId(), global);
        }
        global.send_message(new Message(null, fullMsg, new Date()));
        saveChat(global);
    }

    public List<String> getMessages(String chatName) {
        for (Chat c : chatsCache.values()) {
            if (c.getChatName().equals(chatName)) {
                List<String> out = new ArrayList<>();
                for (Message m : c.getMessages()) {
                    out.add(m.toString());
                }
                return out;
            }
        }
        return new ArrayList<>();
    }

    public Chat findPrivateChat(String user1, String user2) {
        List<Integer> ids = userChatsMap.getOrDefault(user1, new ArrayList<>());
        for (int id : ids) {
            Chat c = getChat(id);
            if (c != null && c.getUsers().size() == 2) {
                boolean has1 = false, has2 = false;
                for (User u : c.getUsers()) {
                    if (user1.equals(u.getNick())) has1 = true;
                    if (user2.equals(u.getNick())) has2 = true;
                }
                if (has1 && has2) {
                    System.out.println("Found private chat between " + user1 + " and " + user2);
                    return c;
                }
            }
        }
        return null;
    }

    public int getTotalChatCount() {
        return chatsCache.size();
    }

    public synchronized boolean deleteChat(int chatId) {
        System.out.println("Удаление чата ID: " + chatId);

        Chat chat = chatsCache.get(chatId);
        if (chat == null) {
            System.out.println("Чат не найден: " + chatId);
            return false;
        }

        // Удаляем чат из кэша
        chatsCache.remove(chatId);

        // Удаляем ссылку на чат у всех пользователей
        for (User user : chat.getUsers()) {
            List<Integer> userChats = userChatsMap.get(user.getNick());
            if (userChats != null) {
                userChats.remove(Integer.valueOf(chatId));
                saveUserChats(user.getNick(), userChats);
                System.out.println("Удален чат " + chatId + " у пользователя " + user.getNick());
            }
        }

        // Удаляем файл чата
        String filename = CHATS_DIR + "/chat_" + chatId + ".dat";
        java.io.File file = new java.io.File(filename);
        if (file.delete()) {
            System.out.println("Файл чата удален: " + filename);
        } else {
            System.out.println("Не удалось удалить файл чата: " + filename);
        }

        System.out.println("Чат " + chat.getChatName() + " (ID: " + chatId + ") успешно удален");
        return true;
    }
}