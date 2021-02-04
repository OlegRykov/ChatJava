package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import commands.Command;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;

    private boolean authenticated;
    private String nickname;

    private Stage stage;
    private Stage regStage;
    private RegController regController;
    private Stage chNickStage;
    private ChNickController chNickController;


    @FXML
    public HBox chNickname;
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox authPanel;
    @FXML
    private HBox msgPanel;
    @FXML
    private TextField textField;
    @FXML
    private TextArea textArea;

    @FXML
    public void btnSend() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        chNickname.setVisible(authenticated);
        chNickname.setManaged(authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        if (!authenticated) {
            nickname = "";
        }
        setTitle(nickname);
        textArea.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textArea.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (socket != null && !socket.isClosed()) {
                    try {
                        out.writeUTF(Command.END);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });

        setAuthenticated(false);
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.startsWith(Command.AUTH_OK)) {
                                nickname = str.split("\\s")[1];
                                setAuthenticated(true);
                                break;
                            }

                            if (str.equals(Command.REG_IS_COMPLETED)) {
                                regController.regIsCompleted();
                            }

                            if (str.equals(Command.REG_IS_NOT_COMPLETED)) {
                                regController.regIsNotCompleted();
                            }

                            if (str.equals(Command.END)) {
                                System.out.println("client disconnected");
                                throw new RuntimeException("server disconnected us");
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }

                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                System.out.println("client disconnected");
                                break;
                            }

                            if (str.startsWith(Command.USER_NICKNAME)){
                                String[] token = str.split("\\s");
                                nickname = token[1];
                                setTitle(nickname);
                            }

                            if (str.equals(Command.CHANGE_NICKNAME_IS_COMPLETED)) {
                                chNickController.chNickIsCompleted();
                            }
                            if (str.equals(Command.CHANGE_NICKNAME_IS_NOT_COMPLETED)) {
                                chNickController.chNickIsNotCompleted();
                            }

                            if (str.startsWith(Command.CLIENT_LIST)) {
                                String[] token = str.split("\\s");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    setAuthenticated(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        String msg = String.format("%s %s %s", Command.AUTH, loginField.getText().trim(),
                passwordField.getText().trim());

        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nickname) {
        if (nickname.equals("")) {
            Platform.runLater(() -> {
                stage.setTitle("Chat");
            });
        } else {
            Platform.runLater(() -> {
                stage.setTitle(String.format("Chat [ %s ]", nickname));
            });
        }
    }

    @FXML
    public void clientListClicked(MouseEvent mouseEvent) {
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textField.setText(String.format("%s %s ", Command.PRIVATE_MESSAGE, receiver));
    }

    @FXML
    public void registration(ActionEvent actionEvent) {
        if (regStage == null) {
            createRegWindow();

        }
        regStage.show();
    }

    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("Chat registration");
            regStage.setScene(new Scene(root, 400, 350));
            regController = fxmlLoader.getController();
            regController.setController(this);
            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.initStyle(StageStyle.UTILITY);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToReg(String login, String password, String nickname) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        String msg = String.format("%s %s %s %s", Command.REGISTRATION, login, password, nickname);
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Stage getRegStage() {
        return regStage;
    }

    @FXML
    public void changeNickname(ActionEvent actionEvent) {
        if (chNickStage == null) {
            createChNickWindow();

        }
        chNickStage.show();
    }

    private void createChNickWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/changeNickname.fxml"));
            Parent root = fxmlLoader.load();
            chNickStage = new Stage();
            chNickStage.setTitle("Chat change nickname");
            chNickStage.setScene(new Scene(root, 400, 350));
            chNickController = fxmlLoader.getController();
            chNickController.setController(this);
            chNickStage.initModality(Modality.APPLICATION_MODAL);
            chNickStage.initStyle(StageStyle.UTILITY);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToChNick(String currentNickname, String password, String newNickname) {
        String msg = String.format("%s %s %s %s", Command.CHANGE_NICKNAME, currentNickname, password, newNickname);
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Stage getChNickStage() {
        return chNickStage;
    }
}
