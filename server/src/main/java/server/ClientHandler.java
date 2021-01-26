package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {

                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s");
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            if (newNick != null) {
                                nickname = newNick;
                                sendMsg(Command.AUTH_OK + " " + nickname);
                                server.subscribe(this);
                                System.out.println("client " + nickname + " connected "
                                        + socket.getRemoteSocketAddress());
                                break;
                            } else {
                                sendMsg("Неверный логин или пароль.");
                            }
                        }

                        if (str.equals(Command.END)) {
                            sendMsg(Command.END);
                            System.out.println("client disconnected");
                            break;
                        }
                    }

                    while (true) {
                        String str = in.readUTF();

                        if (str.equals(Command.END)) {
                            sendMsg(Command.END);
                            System.out.println("client disconnected");
                            break;
                        }

                        server.broadcastMsg(this, str);
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
}
