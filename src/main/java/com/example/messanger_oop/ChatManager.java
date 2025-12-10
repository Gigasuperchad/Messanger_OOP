package com.example.messanger_oop;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {
    private static final String CHATS_DIR = "server_data/chats";
    private Map<Integer, Chat> chatsCache = new ConcurrentHashMap<>();
    private Map<String, List<Integer>> userChatsMap = new ConcurrentHashMap<>();
    private static int nextId = 1;

    public ChatManager() {
        try {
            Files.createDirectories(Paths.get(CHATS_DIR));
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

        System.out.println("Loading " + files.length + " chats from server storage...");
        for (File f : files) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Chat chat = (Chat) ois.readObject();
                chatsCache.put(chat.getId(), chat);

                for (User u : chat.getUsers()) {
                    userChatsMap.computeIfAbsent(u.getNick(), k -> new ArrayList<>());
                    List<Integer> userChats = userChatsMap.get(u.getNick());
                    if (!userChats.contains(chat.getId())) {
                        userChats.add(chat.getId());
                        System.out.println(" Registered chat " + chat.getId() + " for user " + u.getNick());
                    }
                }

                if (chat.getId() >= nextId) {
                    nextId = chat.getId() + 1;
                }
                System.out.println(" Loaded chat: " + chat.getChatName() +
                        " (ID: " + chat.getId() +
                        ", users: " + chat.getUsers().size() + ")");
            } catch (Exception e) {
                System.err.println(" Error loading chat from " + f.getName() + ": " + e.getMessage());
            }
        }
        System.out.println(" Total loaded chats: " + chatsCache.size());
        System.out.println(" Users with chats: " + userChatsMap.size());
    }

    public synchronized Chat createChat(List<User> users, String chatName) {
        System.out.println("🎯 Creating chat: " + chatName);

        if (users.size() == 2) {
            Chat existing = findPrivateChat(users.get(0).getNick(), users.get(1).getNick());
            if (existing != null) {
                System.out.println("⚠️ Private chat already exists: " + existing.getChatName());
                return existing;
            }
        }

        Chat chat = new Chat(users, chatName);
        chat.setId(generateId());
        saveChat(chat);

        for (User u : users) {
            userChatsMap.computeIfAbsent(u.getNick(), k -> new ArrayList<>());
            List<Integer> lst = userChatsMap.get(u.getNick());
            if (!lst.contains(chat.getId())) {
                lst.add(chat.getId());
            }
            saveUserChats(u.getNick(), lst);
            System.out.println(" Added chat " + chat.getId() + " to user " + u.getNick());
        }

        chatsCache.put(chat.getId(), chat);
        System.out.println(" Chat created: " + chat.getChatName() +
                " (ID " + chat.getId() +
                ", users: " + users.size() + ")");
        return chat;
    }

    public void saveChat(Chat chat) {
        String filename = CHATS_DIR + "/chat_" + chat.getId() + ".dat";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(chat);
            System.out.println("Chat saved: " + filename);
        } catch (IOException e) {
            System.err.println("Chat saving error: " + e.getMessage());
        }
    }

    private void saveUserChats(String username, List<Integer> chatIds) {
        String filename = CHATS_DIR + "/user_" + username + "_chats.dat";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(chatIds);
        } catch (IOException e) {
            System.err.println("User chat list saving error: " + e.getMessage());
        }
    }

    public Chat getChat(int chatId) {
        Chat chat = chatsCache.get(chatId);
        if (chat == null) {
            System.out.println("Chat not found ID: " + chatId);
        }
        return chat;
    }

    public List<Chat> getUserChats(String username) {
        System.out.println("Searching chats for user: " + username);
        List<Chat> result = new ArrayList<>();
        List<Integer> ids = userChatsMap.getOrDefault(username, new ArrayList<>());

        System.out.println("Found chat IDs: " + ids.size() + " -> " + ids);

        if (ids.isEmpty()) {
            System.out.println(" No chats found for user " + username);
            return result;
        }

        for (int id : ids) {
            Chat c = getChat(id);
            if (c != null) {
                result.add(c);
                System.out.println("Added chat: " + c.getChatName() + " (ID: " + id + ")");
            } else {
                System.out.println("Chat not found ID: " + id);
            }
        }

        System.out.println("Total chats for " + username + ": " + result.size());
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
}