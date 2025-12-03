package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;

public class HelloController {
    @FXML
    public ListView<Chat> Chat_list;

    private Repository repo;

    @FXML
    public void initialize() {
        // Настройка отображения чатов
        Chat_list.setCellFactory(lv -> new ListCell<Chat>() {
            @Override
            protected void updateItem(Chat chat, boolean empty) {
                super.updateItem(chat, empty);
                if (empty || chat == null) {
                    setText(null);
                } else {
                    // Отображаем название чата или список участников
                    setText(chat.getChatName());
                }
            }
        });

        // Обработчик клика на чат
        Chat_list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Chat selected = Chat_list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    System.out.println("Выбран чат: " + selected);
                    AppManager.getInstance().switchToChatScene(selected);
                }
            }
        });
    }

    public void setRepository(Repository repository) {
        this.repo = repository;
        if (Chat_list != null && repo != null) {
            Chat_list.setItems(repo.getChats());
        }
    }

    // ДОБАВЛЯЕМ ЭТОТ МЕТОД
    @FXML
    private void handleProfileButton() {
        System.out.println("Переход к профилю...");
        AppManager.getInstance().switchToProfileScene();
    }
}