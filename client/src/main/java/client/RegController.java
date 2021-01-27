package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegController {
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField nicknameField;
    @FXML
    public TextArea textArea;
    private Controller controller;

    private Stage regStage;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @FXML
    public void tryToReg(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknameField.getText().trim();

        controller.tryToReg(login, password, nickname);
    }

    public void regIsCompleted() {
        textArea.appendText("Регистрация прошла успешно.\n");
    }

    public void regIsNotCompleted() {
        textArea.appendText("Логин или никнейм уже заняты.\n");
    }

    @FXML
    public void cancel(ActionEvent actionEvent) {
        regStage = controller.getRegStage();
        regStage.close();
    }
}
