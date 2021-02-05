package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ChNickController {
    @FXML
    public TextField currentNicknameField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField newNicknameField;
    @FXML
    public TextArea textArea;


    private Controller controller;

    private Stage chNickStage;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @FXML
    public void tryToChNickname(ActionEvent actionEvent) {
        String currentNickname = currentNicknameField.getText().trim();
        String password = passwordField.getText().trim();
        String newNickname = newNicknameField.getText().trim();

        controller.tryToChNick(currentNickname, password, newNickname);
    }

    public void chNickIsCompleted() {
        textArea.appendText("Смена никнейма прошла успешно.\n");
    }

    public void chNickIsNotCompleted() {
        textArea.appendText("Неверный никнейм или пароль или никнейм уже занят.\n");
    }

    @FXML
    public void cancel(ActionEvent actionEvent) {
        chNickStage = controller.getChNickStage();
        chNickStage.close();
    }
}
