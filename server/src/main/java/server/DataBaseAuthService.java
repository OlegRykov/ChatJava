package server;

import java.sql.*;

public class DataBaseAuthService implements AuthService {

    private Connection connection;
    private Statement stmt;
    private PreparedStatement psInsert;

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            connect();
            ResultSet rs = stmt.executeQuery("SELECT login, password, nickname FROM users;");

            while (rs.next()) {
                if (rs.getString("login").equals(login) &&
                        rs.getString("password").equals(password)) {
                    return rs.getString("nickname");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        try {
            connect();
            ResultSet rs = stmt.executeQuery("SELECT login, nickname FROM users;");

            while (rs.next()) {
                if (rs.getString("login").equals(login) ||
                        rs.getString("nickname").equals(nickname)) {
                    return false;
                }
            }
            insertUser();
            fillUser(login, password, nickname);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return true;
    }

    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        stmt = connection.createStatement();
    }

    private void disconnect() {
        try {
            stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void insertUser() throws SQLException {
        psInsert = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?);");
    }

    public void fillUser(String login, String password, String nickname) throws SQLException {
        psInsert.setString(1, login);
        psInsert.setString(2, password);
        psInsert.setString(3, nickname);
        psInsert.executeUpdate();
    }

    @Override
    public boolean changeNickname(String currentNickname, String password, String newNickname) {
        try {
            connect();
            ResultSet rs = stmt.executeQuery("SELECT password, nickname FROM users;");

            while (rs.next()) {
                if (rs.getString("password").equals(password) &&
                        rs.getString("nickname").equals(currentNickname)) {
                    ResultSet resNick = stmt.executeQuery("SELECT nickname FROM users;");
                    while (resNick.next()) {
                        if (resNick.getString("nickname").equals(newNickname)) {
                            return false;
                        }
                    }
                    insertChNickname();
                    fillChNickname(newNickname, currentNickname);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return false;
    }
    public void insertChNickname() throws SQLException {
        psInsert = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?;");
    }

    public void fillChNickname(String newNickname, String currentNickname) throws SQLException {
        psInsert.setString(1, newNickname);
        psInsert.setString(2, currentNickname);
        psInsert.executeUpdate();
    }
}
