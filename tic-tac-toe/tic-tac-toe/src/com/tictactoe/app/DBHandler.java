package com.tictactoe.app;

import java.sql.*;
import java.util.*;
import java.io.*;

public class DBHandler {

    private static String URL;
    private static String USER;
    private static String PASS;

    // ────────────────────────────────────────────────────────────
    //  Load DB credentials from config.properties (once)
    // ────────────────────────────────────────────────────────────
    static {
        try (InputStream input = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            URL  = prop.getProperty("db.url");
            USER = prop.getProperty("db.user");
            PASS = prop.getProperty("db.password");
        } catch (IOException ex) {
            System.out.println("❌ Failed to load DB config: " + ex.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Basic connection helper
    // ────────────────────────────────────────────────────────────
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ────────────────────────────────────────────────────────────
    //  Insert player (no dup issue thanks to ON DUPLICATE KEY)
    // ────────────────────────────────────────────────────────────
    public static void insertPlayer(String name) {
        String query = "INSERT INTO players (player_name) VALUES (?) " +
                       "ON DUPLICATE KEY UPDATE player_name = player_name";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, name);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Insert error: " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Update stats for a player
    // ────────────────────────────────────────────────────────────
    public static void updateStats(String name, boolean won) {
        String sql = "UPDATE players SET games_played = games_played + 1" +
                     (won ? ", wins = wins + 1" : "") +
                     " WHERE player_name = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Log a finished game
    // ────────────────────────────────────────────────────────────
    public static void logGame(String player, String opponent, String result, String mode) {
        String sql = "INSERT INTO game_history (player_name, opponent_name, result, mode) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, player);
            ps.setString(2, opponent);
            ps.setString(3, result);
            ps.setString(4, mode);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Log error: " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Fetch last 10 games for a player (most‑recent first)
    // ────────────────────────────────────────────────────────────
    public static List<String> fetchLast10Games(String playerName) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT opponent_name, result, mode, date_played " +
                     "FROM game_history WHERE player_name = ? " +
                     "ORDER BY date_played DESC LIMIT 10";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                history.add(String.format(
                    "vs %s | %s | %s | %s",
                    rs.getString("opponent_name"),
                    rs.getString("result"),
                    rs.getString("mode"),
                    rs.getTimestamp("date_played").toLocalDateTime()
                ));
            }

        } catch (SQLException e) {
            System.out.println("Fetch error: " + e.getMessage());
        }
        return history;
    }
}
