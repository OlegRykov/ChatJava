package server;

import commands.Command;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Server {
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8189;
    private List<ClientHandler> clients;
    private AuthService authService;
    private final Logger logger = Logger.getLogger(Server.class.getName());
    LogManager manager = LogManager.getLogManager();

    public Server() {
        clients = new CopyOnWriteArrayList<>();
//        authService = new SimpleAuthService();
        authService = new DataBaseAuthService();
        try {
            manager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            server = new ServerSocket(PORT);
            logger.info("Server started");

            while (true) {
                socket = server.accept();
                logger.info("Client connected");
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE,"Произошла ошибка: ", e);
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE,"Произошла ошибка: ", e);
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler clientHandler, String msg) {
        String message = String.format("[ %s ]: %s", clientHandler.getNickname(), msg);
        for (ClientHandler c : clients) {
            if (msg.startsWith(Command.CHANGE_NICKNAME)) {
                logger.info(msg);
            } else if (!msg.startsWith("/")) {
                logger.log(Level.FINE, msg);
                c.sendMsg(message);
                c.writeHistory(message);
            } else if (msg.startsWith(Command.PRIVATE_MESSAGE + " " + c.getNickname()) ||
                    c.getNickname().equals(clientHandler.getNickname())) {
                String[] prMsg = msg.split("\\s", 3);
                String privateMassage;
                if (prMsg.length > 2) {
                    privateMassage = String.format("[ %s ]: %s", clientHandler.getNickname(), prMsg[2]);
                    c.sendMsg(privateMassage);
                    c.writeHistory(privateMassage);
                }
            }
        }
    }

    void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder(Command.CLIENT_LIST);

        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }

        String msg = sb.toString();

        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }

    public Logger getLogger() {
        return logger;
    }
}
