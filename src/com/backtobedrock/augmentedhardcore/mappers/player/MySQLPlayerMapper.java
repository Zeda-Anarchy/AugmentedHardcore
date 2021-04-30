package com.backtobedrock.augmentedhardcore.mappers.player;

import com.backtobedrock.augmentedhardcore.domain.Ban;
import com.backtobedrock.augmentedhardcore.domain.data.PlayerData;
import com.backtobedrock.augmentedhardcore.mappers.AbstractMapper;
import com.backtobedrock.augmentedhardcore.mappers.ban.MySQLBanMapper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.javatuples.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public class MySQLPlayerMapper extends AbstractMapper implements IPlayerMapper {
    private static MySQLPlayerMapper instance;

    public static MySQLPlayerMapper getInstance() {
        if (instance == null) {
            instance = new MySQLPlayerMapper();
        }
        return instance;
    }

    @Override
    public void insertPlayerDataAsync(PlayerData playerData) {
        CompletableFuture.runAsync(() -> this.updatePlayerData(playerData)).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public void insertPlayerDataSync(PlayerData playerData) {
        Bukkit.getScheduler().runTask(this.plugin, () -> this.updatePlayerData(playerData));
    }

    @Override
    public CompletableFuture<PlayerData> getByPlayer(OfflinePlayer player) {
        return CompletableFuture.supplyAsync(() -> this.getPlayerData(player));
    }

    private PlayerData getPlayerData(OfflinePlayer player) {
        String sql = "SELECT * "
                + "FROM ah_ban AS b "
                + "RIGHT OUTER JOIN ah_player as p ON p.player_uuid = b.player_uuid "
                + "WHERE p.player_uuid = ?;";

        try (Connection connection = this.database.getDataSource().getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            NavigableMap<Integer, Ban> deathBans = new TreeMap<>();
            PlayerData playerData = null;
            while (resultSet.next()) {
                if (playerData == null) {
                    playerData = new PlayerData(player, resultSet.getString("last_known_ip"), resultSet.getInt("lives"), resultSet.getInt("life_parts"), resultSet.getBoolean("spectator_banned"), resultSet.getLong("time_till_next_revive"), resultSet.getLong("time_till_next_life_part"), resultSet.getLong("time_till_next_max_health"), deathBans);
                }
                Pair<Integer, Ban> banPair = MySQLBanMapper.getInstance().getBanFromResultSetSync(resultSet);
                if (banPair != null) {
                    deathBans.put(banPair.getValue0(), banPair.getValue1());
                }
            }
            return playerData;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public PlayerData getByPlayerSync(OfflinePlayer player) {
        return this.getPlayerData(player);
    }

    @Override
    public void updatePlayerData(PlayerData playerData) {
        if (this.plugin.isStopping()) {
            this.updatePlayerDateSync(playerData);
        } else {
            CompletableFuture.runAsync(() -> this.updatePlayerDateSync(playerData)).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        }
    }

    private void updatePlayerDateSync(PlayerData playerData) {
        String sql = "INSERT INTO ah_player (`player_uuid`, `last_known_name`, `last_known_ip`, `lives`, `life_parts`, `spectator_banned`, `time_till_next_revive`, `time_till_next_life_part`, `time_till_next_max_health`)"
                + "VALUES(?,?,?,?,?,?,?,?,?)"
                + "ON DUPLICATE KEY UPDATE "
                + "`last_known_name` = ?,"
                + "`last_known_ip` = ?,"
                + "`lives` = ?,"
                + "`life_parts` = ?,"
                + "`spectator_banned` = ?,"
                + "`time_till_next_revive` = ?,"
                + "`time_till_next_life_part` = ?,"
                + "`time_till_next_max_health` = ?;";

        try (Connection connection = this.database.getDataSource().getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerData.getPlayer().getUniqueId().toString());
            preparedStatement.setString(2, playerData.getPlayer().getName());
            preparedStatement.setString(3, playerData.getLastKnownIp());
            preparedStatement.setInt(4, playerData.getLives());
            preparedStatement.setInt(5, playerData.getLifeParts());
            preparedStatement.setBoolean(6, playerData.isSpectatorBanned());
            preparedStatement.setLong(7, playerData.getTimeTillNextRevive());
            preparedStatement.setLong(8, playerData.getTimeTillNextLifePart());
            preparedStatement.setLong(9, playerData.getTimeTillNextMaxHealth());
            preparedStatement.setString(10, playerData.getPlayer().getName());
            preparedStatement.setString(11, playerData.getLastKnownIp());
            preparedStatement.setInt(12, playerData.getLives());
            preparedStatement.setInt(13, playerData.getLifeParts());
            preparedStatement.setBoolean(14, playerData.isSpectatorBanned());
            preparedStatement.setLong(15, playerData.getTimeTillNextRevive());
            preparedStatement.setLong(16, playerData.getTimeTillNextLifePart());
            preparedStatement.setLong(17, playerData.getTimeTillNextMaxHealth());
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deletePlayerData(OfflinePlayer player) {
        CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM ah_player " +
                    "WHERE `player_uuid` = ?;";

            try (Connection connection = this.database.getDataSource().getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, player.getUniqueId().toString());
                preparedStatement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}
