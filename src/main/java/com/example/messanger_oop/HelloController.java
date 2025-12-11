package com.example.messanger_oop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class HelloController {
    @FXML
    public ListView<Chat> Chat_list;

    private Repository repo;

    @FXML
    public void initialize() {
        Chat_list.setCellFactory(lv -> new ListCell<Chat>() {
            @Override
            protected void updateItem(Chat chat, boolean empty) {
                super.updateItem(chat, empty);
                if (empty || chat == null) {
                    setText(null);
                } else {
                    setText(chat.getChatName() +
                            " (" + chat.get_message_count() + " сообщ.)");
                }
            }
        });

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

    @FXML
    private void handleProfileButton() {
        System.out.println("Переход к профилю...");
        AppManager.getInstance().switchToProfileScene();
    }

    @FXML
    private void handleLogout() {
        System.out.println("Выход из аккаунта...");
        AppManager.getInstance().logout();
    }

    @FXML
    private void handleCreateChat() {
        System.out.println("Создание нового чата...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Create_Chat_Scene.fxml"));
            Parent root = loader.load();

            CreateChatController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(AppManager.getInstance().getCurrentUser());
            }

            Stage createChatStage = new Stage();
            createChatStage.setTitle("Создание нового чата");
            createChatStage.setScene(new Scene(root, 700, 550));
            createChatStage.show();

        } catch (Exception e) {
            System.err.println("Ошибка открытия окна создания чата: " + e.getMessage());
            e.printStackTrace();
        }
    }
}