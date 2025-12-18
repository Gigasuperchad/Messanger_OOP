package com.example.messanger_oop.client;

import com.example.messanger_oop.server.StatusManager;
import com.example.messanger_oop.shared.User;
import com.example.messanger_oop.shared.UserStatus;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class StatusController {
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField statusMessageField;
    @FXML private Label currentStatusLabel;
    @FXML private Button setStatusButton;
    @FXML private Button cancelButton;

    private User currentUser;

    @FXML
    public void initialize() {
        // Заполняем ComboBox статусами через код
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
                "🟢 В сети",
                "🟡 Отошел",
                "🔴 Не беспокоить",
                "👻 Невидимый",
                "⚫ Не в сети"
        );
        statusComboBox.setItems(statusOptions);

        setStatusButton.setOnAction(event -> handleSetStatus());
        cancelButton.setOnAction(event -> closeWindow());

        // Загружаем текущий статус
        loadCurrentUser();
        updateCurrentStatusDisplay();
    }

    private void loadCurrentUser() {
        currentUser = AppManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserStatus status = StatusManager.getInstance().getUserStatus(currentUser.getNick());
            if (status != null) {
                // Находим соответствующий элемент в списке
                String statusDisplay = status.getStatusDisplay();
                for (String option : statusComboBox.getItems()) {
                    if (option.contains(statusDisplay.split(" ")[0])) {
                        statusComboBox.setValue(option);
                        break;
                    }
                }
                statusMessageField.setText(status.getCustomMessage());
            } else {
                statusComboBox.setValue("⚫ Не в сети");
            }
        }
    }

    private void updateCurrentStatusDisplay() {
        if (currentUser != null) {
            UserStatus status = StatusManager.getInstance().getUserStatus(currentUser.getNick());
            currentStatusLabel.setText("Текущий статус: " +
                    (status != null ? status.getStatusDisplay() : "Неизвестно"));
        }
    }

    @FXML
    private void handleSetStatus() {
        if (currentUser == null) return;

        String selectedStatus = statusComboBox.getValue();
        String customMessage = statusMessageField.getText().trim();

        if (selectedStatus == null) {
            showAlert("Ошибка", "Выберите статус!");
            return;
        }

        try {
            // Определяем тип статуса по значению
            UserStatus.Status statusType;
            if (selectedStatus.contains("🟢")) {
                statusType = UserStatus.Status.ONLINE;
            } else if (selectedStatus.contains("🟡")) {
                statusType = UserStatus.Status.AWAY;
            } else if (selectedStatus.contains("🔴")) {
                statusType = UserStatus.Status.DO_NOT_DISTURB;
            } else if (selectedStatus.contains("👻")) {
                statusType = UserStatus.Status.INVISIBLE;
            } else {
                statusType = UserStatus.Status.OFFLINE;
            }

            // Устанавливаем статус
            StatusManager.getInstance().setUserStatus(currentUser.getNick(), statusType);

            if (!customMessage.isEmpty()) {
                StatusManager.getInstance().setCustomStatusMessage(currentUser.getNick(), customMessage);
            }

            // Отправляем на сервер если подключены
            Repository repo = AppManager.getInstance().getRepository();
            if (repo instanceof LocalRepository) {
                LocalRepository localRepo = (LocalRepository) repo;
                if (localRepo.isConnectedToServer()) {
                    // Можно добавить отправку статуса на сервер здесь
                    System.out.println("Статус установлен локально. Для отправки на сервер нужно добавить функционал.");
                }
            }

            showAlert("Успех", "Статус успешно обновлен!");
            updateCurrentStatusDisplay();

            // Закрываем окно через 1 секунду
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(this::closeWindow);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось обновить статус: " + e.getMessage());
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}