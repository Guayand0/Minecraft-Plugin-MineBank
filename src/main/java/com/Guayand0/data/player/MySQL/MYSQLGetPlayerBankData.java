package com.Guayand0.data.player.MySQL;

import java.sql.*;
import java.util.*;

public class MYSQLGetPlayerBankData {

    // Datos de la base de datos
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    public MYSQLGetPlayerBankData(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    /**
     * Obtiene una conexión a la base de datos MySQL.
     *
     * @return La conexión a la base de datos.
     * @throws SQLException Si ocurre un error al obtener la conexión.
     */
    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Obtiene los datos del banco de un jugador desde MySQL
     *
     * @param playerName Nombre del jugador
     * @return Un mapa con los datos del banco
     * @throws SQLException Si ocurre un error en la consulta SQL
     */
    public Map<String, Object> getBankDataOfPlayerName(String playerName) throws SQLException {
        String query = "SELECT name, level, max_balance, upgrade_cost, final_level FROM player_data WHERE player_name = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Map<String, Object> bankData = new HashMap<>();
                bankData.put("name", resultSet.getInt("name"));
                bankData.put("level", resultSet.getInt("level"));
                bankData.put("max_balance", resultSet.getInt("max_balance"));
                bankData.put("upgrade_cost", resultSet.getInt("upgrade_cost"));
                bankData.put("final_level", resultSet.getBoolean("final_level"));
                return bankData;
            } else {
                throw new IllegalArgumentException("Player not found in database");
            }
        }
    }
}
