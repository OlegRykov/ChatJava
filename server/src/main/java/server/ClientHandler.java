package server;

import commands.Command;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.List;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    socket.setSoTimeout(120000);
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s");
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg(Command.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    System.out.println("client " + nickname + " connected "
                                            + socket.getRemoteSocketAddress());
                                    socket.setSoTimeout(0);
                                    readUserHistory();
                                    break;
                                } else {
                                    sendMsg("Этот логин уже используется.");
                                }

                            } else {
                                sendMsg("Неверный логин или пароль.");
                            }
                        }

                        if (str.equals(Command.END)) {
                            sendMsg(Command.END);
                            throw new RuntimeException("client disconnected");
                        }

                        if (str.startsWith(Command.REGISTRATION)) {
                            String[] token = str.split("\\s");
                            if (token.length < 4) {
                                continue;
                            }
                            boolean registrationIsCompleted = server.getAuthService().registration(token[1],
                                    token[2], token[3]);
                            if (registrationIsCompleted) {
                                sendMsg(Command.REG_IS_COMPLETED);
                                addClientHistory(token[1]);
                            } else {
                                sendMsg(Command.REG_IS_NOT_COMPLETED);
                            }
                        }
                    }

                    while (true) {
                        String str = in.readUTF();


                        if (str.startsWith(Command.CHANGE_NICKNAME)) {
                            String[] token = str.split("\\s");
                            if (token.length < 4) {
                                continue;
                            }
                            boolean chNicknameIsCompleted = server.getAuthService().changeNickname(token[1],
                                    token[2], token[3]);
                            if (chNicknameIsCompleted) {
                                sendMsg(Command.CHANGE_NICKNAME_IS_COMPLETED);
                                sendMsg(Command.USER_NICKNAME + " " + token[3]);
                                nickname = token[3];
                                server.broadcastClientList();
                            } else {
                                sendMsg(Command.CHANGE_NICKNAME_IS_NOT_COMPLETED);
                            }
                        }

                        if (str.equals(Command.END)) {
                            sendMsg(Command.END);
                            System.out.println("client disconnected");
                            break;
                        }
                        server.broadcastMsg(this, str);
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (SocketTimeoutException e) {
                    try {
                        out.writeUTF(Command.END);
                        System.out.println("Client disconnected");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
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

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }

    public void addClientHistory(String login){
        File file = new File("userHistory/history_" + login +".txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void writeHistory(String msg){
        try {
            FileWriter fileWriter = new FileWriter("userHistory/history_" + login +".txt", true);
            fileWriter.write(msg + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readUserHistory(){
        File file = new File("userHistory/history_" + this.login +".txt");

        try {
            List<String> list = Files.readAllLines(file.toPath());
           if (list.size() > 100){
               for (int i = list.size() - 101; i < list.size(); i++) {
                   this.sendMsg(list.get(i));
               }
           }else {
               for (int i = 0; i < list.size(); i++) {
                   this.sendMsg(list.get(i));
               }
           }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
