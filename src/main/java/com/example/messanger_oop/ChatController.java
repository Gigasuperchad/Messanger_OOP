package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.Date;

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
    private boolean isSending = false;

    @FXML
    public void initialize() {
        messages = FXCollections.observableArrayList();
        messageListView.setItems(messages);

        sendButton.setOnAction(event -> {
            handleSendMessage();
        });

        messageTextArea.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                handleSendMessage();
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
        System.out.println("Установлен текущий пользователь: " +
                (user != null ? user.getNick() : "null"));
    }

    private void updateMessageList() {
        if (chat != null) {
            messages.clear();
            List<Message> chatMessages = chat.getMessages();
            if (chatMessages != null) {
                for (Message message : chatMessages) {
                    String timestamp = formatTimestamp(message.getTimestamp());
                    String senderNick = message.getSender() != null ?
                            message.getSender().getNick() : "Неизвестный";
                    String content = message.getContent() != null ?
                            message.getContent() : "";

                    String messageDisplay = String.format("[%s] %s: %s",
                            timestamp, senderNick, content);
                    messages.add(messageDisplay);
                }
            } else {
                System.err.println("Сообщения в чате null");
            }
        } else {
            System.err.println("Чат null");
        }
    }

    private String formatTimestamp(Date timestamp) {
        if (timestamp == null) return "Неизвестное время";

        // Форматируем время в ЧЧ:ММ
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        return sdf.format(timestamp);
    }

    @FXML
    private void handleSendMessage() {
        if (isSending) {
            System.out.println("Отправка уже выполняется, пропускаем...");
            return;
        }

        String text = messageTextArea.getText().trim();

        System.out.println("=== Отправка сообщения ===");
        System.out.println("Текст: '" + text + "'");
        System.out.println("Чат: " + (chat != null ? chat.getChatName() : "null"));
        System.out.println("Текущий пользователь: " +
                (currentUser != null ? currentUser.getNick() : "null"));

        if (!text.isEmpty() && chat != null && currentUser != null) {
            isSending = true;

            try {
                Message message = new Message(currentUser, text, new Date());

                if (repository != null) {
                    repository.send_msg(chat, text);
                } else {
                    chat.send_message(message);
                }

                updateMessageList();

                messageTextArea.clear();

                System.out.println("Сообщение успешно отправлено!");

            } catch (Exception e) {
                System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isSending = false;
            }
        } else {
            System.err.println("Не могу отправить сообщение:");
            System.err.println("  Текст пустой: " + text.isEmpty());
            System.err.println("  Чат null: " + (chat == null));
            System.err.println("  Пользователь null: " + (currentUser == null));
            isSending = false;
        }
    }

    @FXML
    private void handleBackToChatList() {
        System.out.println("Возврат к списку чатов...");
        AppManager.getInstance().switchToChatList();
    }
}