package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class ChatController {
    @FXML
    private ListView<String> messageListView;

    @FXML
    private TextArea messageTextArea;

    @FXML
    private Button sendButton;

    private Repository repository;
    private Chat chat;
    private User currentUser;
    private ObservableList<String> messages;

    @FXML
    public void initialize() {
        messages = FXCollections.observableArrayList();
        messageListView.setItems(messages);

        // Добавляем обработчик клавиши Enter
        messageTextArea.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleSendMessage();
                event.consume();
            }
        });
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
        updateMessageList();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("Текущий пользователь установлен: " +
                (user != null ? user.getNick() : "null"));
    }

    private void updateMessageList() {
        if (chat != null) {
            messages.clear();
            List<Message> chatMessages = chat.getMessages();
            if (chatMessages != null) {
                for (Message message : chatMessages) {
                    String timestamp = "Неизвестное время";
                    if (message.getTimestamp() != null) {
                        timestamp = message.getTimestamp().toString();
                    }

                    String senderNick = "Неизвестный";
                    if (message.getSender() != null) {
                        senderNick = message.getSender().getNick();
                    }

                    messages.add(timestamp + " - " + senderNick + ": " +
                            (message.getContent() != null ? message.getContent() : ""));
                }
            } else {
                System.err.println("Сообщения в чате null");
            }
        } else {
            System.err.println("Чат null");
        }
    }

    @FXML
    private void handleSendMessage() {
        String text = messageTextArea.getText().trim();

        // Отладочная информация
        System.out.println("=== Отправка сообщения ===");
        System.out.println("Текст: " + text);
        System.out.println("Чат: " + (chat != null ? chat.getChatName() : "null"));
        System.out.println("Текущий пользователь: " +
                (currentUser != null ? currentUser.getNick() : "null"));
        System.out.println("Репозиторий: " + (repository != null ? "установлен" : "null"));

        if (!text.isEmpty() && chat != null && currentUser != null) {
            try {
                Message message = new Message(currentUser, text, new java.util.Date());
                chat.send_message(message);

                if (repository != null) {
                    repository.send_msg(chat, text);
                }

                updateMessageList();
                messageTextArea.clear();
                System.out.println("Сообщение успешно отправлено!");
            } catch (Exception e) {
                System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Не могу отправить сообщение. Проверьте условия:");
            System.err.println("1. Текст не пустой: " + !text.isEmpty());
            System.err.println("2. Чат не null: " + (chat != null));
            System.err.println("3. Пользователь не null: " + (currentUser != null));
        }
    }

    @FXML
    private void handleBackToChatList() {
        System.out.println("Возврат к списку чатов...");
        AppManager.getInstance().switchToChatList();
    }
}