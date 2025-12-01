package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.util.Date;

public class ChatController {
    @FXML
    private ListView<Message> messageListView;

    @FXML
    private TextArea messageTextArea;

    @FXML
    private Button sendButton;

    private Chat currentChat;
    private Repository repository;
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = AppManager.getInstance().getCurrentUser();

        // Настройка отображения сообщений
        messageListView.setCellFactory(lv -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                } else {
                    String senderName = message.getSender() != null ?
                            message.getSender().getNick() : "Неизвестно";
                    String time = message.getTimestamp() != null ?
                            message.getTimestamp().toString() : "Недавно";
                    setText(String.format("[%s] %s: %s",
                            time, senderName, message.getContent()));
                }
            }
        });
    }

    public void setChat(Chat chat) {
        this.currentChat = chat;
        if (chat != null && chat.getMessages() != null) {
            messageListView.setItems(chat.getMessages());

            // Прокрутка к последнему сообщению
            if (!chat.getMessages().isEmpty()) {
                messageListView.scrollTo(chat.getMessages().size() - 1);
            }
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @FXML
    private void handleSendMessage() {
        sendMessage();
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
            event.consume(); // Предотвращаем перенос строки
            sendMessage();
        }
    }

    private void sendMessage() {
        String text = messageTextArea.getText().trim();
        if (!text.isEmpty() && currentChat != null) {
            // Создаем новое сообщение
            Message newMessage = new Message(currentUser, text, new Date());

            // Отправляем сообщение в чат
            currentChat.send_message(newMessage);

            // Очищаем поле ввода
            messageTextArea.clear();

            // Прокрутка к последнему сообщению
            if (messageListView.getItems().size() > 0) {
                messageListView.scrollTo(messageListView.getItems().size() - 1);
            }
        }
    }

    @FXML
    private void handleBackToChatList() {
        // Возврат к списку чатов
        AppManager.getInstance().switchToChatList();
    }
}