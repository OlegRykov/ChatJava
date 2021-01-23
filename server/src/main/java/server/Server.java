package server;

import commands.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8189;
    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() {
        clients = new CopyOnWriteArrayList<>();
        authService = new SimpleAuthService();

        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started");

            while (true) {
                socket = server.accept();
                System.out.println("Client connected");
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler clientHandler, String msg) {
        String message = String.format("[ %s ]: %s", clientHandler.getNickname(), msg);
        for (ClientHandler c : clients) {
            if (!msg.startsWith("/")) {
                c.sendMsg(message);
            } else if (msg.startsWith(Command.PRIVATE_MESSAGE + " " + c.getNickname()) ||
                    c.getNickname().equals(clientHandler.getNickname())) {
                String[] prMsg = msg.split("\\s", 3);
                String privateMassage;
                if (prMsg.length > 2) {
                    privateMassage = String.format("[ %s ]: %s", clientHandler.getNickname(), prMsg[2]);
                    c.sendMsg(privateMassage);
                }
            } else {
                c.sendMsg(message);
            }
        }
    }

    void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public AuthService getAuthService() {
        return authService;
    }
}
