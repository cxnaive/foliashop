package dev.user.shop.gacha;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.util.ItemUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class GachaManager {

    private final FoliaShopPlugin plugin;
    private final Map<String, GachaMachine> machines;

    public GachaManager(FoliaShopPlugin plugin) {
        this.plugin = plugin;
        this.machines = new HashMap<>();
        load();
    }

    public void load() {
        machines.clear();

        ConfigurationSection section = plugin.getShopConfig().getGachaMachines();
        if (section == null) {
            plugin.getLogger().warning("未配置扭蛋机");
            return;
        }

        for (String machineId : section.getKeys(false)) {
            ConfigurationSection machineSection = section.getConfigurationSection(machineId);
            if (machineSection == null) continue;

            String name = machineSection.getString("name", machineId);
            List<String> description = machineSection.getStringList("description");
            String icon = machineSection.getString("icon", "minecraft:chest");
            double cost = machineSection.getDouble("cost", 100.0);
            int animationDuration = machineSection.getInt("animation-duration", 3);
            // 10连抽动画时间，默认是单抽的3倍
            int animationDurationTen = machineSection.getInt("animation-duration-ten", animationDuration * 3);
            boolean broadcastRare = machineSection.getBoolean("broadcast-rare", true);
            double broadcastThreshold = machineSection.getDouble("broadcast-threshold", 0.05);
            // GUI位置（0-26），默认为0表示自动分配
            int slot = machineSection.getInt("slot", 0);

            // 加载保底规则
            List<PityRule> pityRules = new ArrayList<>();
            List<Map<?, ?>> pityList = machineSection.getMapList("pity-rules");
            if (pityList != null) {
                for (Map<?, ?> pityMap : pityList) {
                    int count = pityMap.get("count") instanceof Number ? ((Number) pityMap.get("count")).intValue() : 0;
                    double maxProb = pityMap.get("max-probability") instanceof Number ? ((Number) pityMap.get("max-probability")).doubleValue() : 1.0;
                    if (count > 0 && maxProb > 0) {
                        pityRules.add(new PityRule(count, maxProb));
                    }
                }
            }

            GachaMachine machine = new GachaMachine(
                machineId, name, description, icon, cost,
                animationDuration, animationDurationTen, broadcastRare, broadcastThreshold, slot, pityRules
            );

            // 加载奖品
            List<Map<?, ?>> rewardsList = machineSection.getMapList("rewards");
            for (Map<?, ?> rewardMap : rewardsList) {
                String id = String.valueOf(rewardMap.get("id"));
                String itemKey = String.valueOf(rewardMap.get("item"));
                int amount = rewardMap.get("amount") instanceof Number ? ((Number) rewardMap.get("amount")).intValue() : 1;
                double probability = rewardMap.get("probability") instanceof Number ? ((Number) rewardMap.get("probability")).doubleValue() : 0.1;
                String displayName = rewardMap.get("display-name") != null ? String.valueOf(rewardMap.get("display-name")) : null;
                boolean broadcast = rewardMap.get("broadcast") instanceof Boolean ? (Boolean) rewardMap.get("broadcast") : false;

                if (displayName == null) {
                    displayName = itemKey;
                }

                GachaReward reward = new GachaReward(id, itemKey, amount, probability, displayName, broadcast);

                // 创建显示物品
                ItemStack item = ItemUtil.createItemFromKey(plugin, itemKey);
                if (item != null) {
                    reward.setDisplayItem(item);
                }

                machine.addReward(reward);
            }

            machines.put(machineId, machine);
        }

        plugin.getLogger().info("已加载 " + machines.size() + " 个扭蛋机");
    }

    public void reload() {
        load();
    }

    /**
     * 记录扭蛋抽奖
     */
    public void logGacha(UUID playerUuid, String playerName, String machineId, GachaReward reward, double cost) {
        plugin.getDatabaseQueue().submit("logGacha", conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO gacha_records (player_uuid, player_name, machine_id, reward_id, item_key, amount, cost, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, playerName);
                ps.setString(3, machineId);
                ps.setString(4, reward.getId());
                ps.setString(5, reward.getItemKey());
                ps.setInt(6, reward.getAmount());
                ps.setDouble(7, cost);
                ps.setLong(8, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public GachaMachine getMachine(String id) {
        return machines.get(id);
    }

    public Collection<GachaMachine> getAllMachines() {
        return machines.values();
    }

    public boolean hasMachine(String id) {
        return machines.containsKey(id);
    }

    /**
     * 获取玩家的各规则保底计数
     * @return Map<ruleHash, count>
     */
    public void getPityCounters(UUID playerUuid, String machineId, Consumer<Map<String, Integer>> callback) {
        plugin.getDatabaseQueue().submit("getPityCounters", conn -> {
            Map<String, Integer> counters = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT rule_hash, draw_count FROM gacha_pity WHERE player_uuid = ? AND machine_id = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, machineId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    counters.put(rs.getString("rule_hash"), rs.getInt("draw_count"));
                }
            }
            return counters;
        }, callback, error -> {
            plugin.getLogger().warning("获取保底计数失败: " + error.getMessage());
            callback.accept(new HashMap<>());
        });
    }

    /**
     * 更新保底计数
     * @param rules 所有保底规则列表
     * @param triggeredRuleHash 触发的规则哈希（null表示未触发）
     * @param rewardId 奖励ID
     */
    public void updatePityCounters(UUID playerUuid, String machineId, List<PityRule> rules, String triggeredRuleHash, String rewardId) {
        plugin.getDatabaseQueue().submit("updatePityCounters", conn -> {
            // 为每个规则更新计数
            for (PityRule rule : rules) {
                String ruleHash = rule.getRuleHash();
                int newCount;
                if (ruleHash.equals(triggeredRuleHash)) {
                    // 触发的规则重置为0
                    newCount = 0;
                } else {
                    // 其他规则正常+1
                    newCount = getCurrentCount(conn, playerUuid, machineId, ruleHash) + 1;
                }

                String sql = "MERGE INTO gacha_pity (player_uuid, machine_id, rule_hash, draw_count, last_pity_reward, last_draw_time) " +
                            "KEY (player_uuid, machine_id, rule_hash) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, machineId);
                    ps.setString(3, ruleHash);
                    ps.setInt(4, newCount);
                    ps.setString(5, ruleHash.equals(triggeredRuleHash) ? rewardId : null);
                    ps.setLong(6, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    /**
     * 批量更新保底计数（用于10连抽等多次抽奖场景）
     * 直接设置计数器值，而不是基于数据库值+1
     * @param counters 各规则的最终计数值 Map<ruleHash, count>
     * @param lastTriggeredRule 最后触发的保底规则（用于记录last_pity_reward）
     * @param lastRewardId 最后触发的奖励ID
     */
    public void batchUpdatePityCounters(UUID playerUuid, String machineId, List<PityRule> rules,
                                        Map<String, Integer> counters, PityRule lastTriggeredRule, String lastRewardId) {
        plugin.getDatabaseQueue().submit("batchUpdatePityCounters", conn -> {
            for (PityRule rule : rules) {
                String ruleHash = rule.getRuleHash();
                int finalCount = counters.getOrDefault(ruleHash, 0);

                // 只有最后触发的规则才记录rewardId
                String rewardIdToRecord = (lastTriggeredRule != null && ruleHash.equals(lastTriggeredRule.getRuleHash()))
                    ? lastRewardId : null;

                String sql = plugin.getDatabaseManager().isMySQL()
                    ? "INSERT INTO gacha_pity (player_uuid, machine_id, rule_hash, draw_count, last_pity_reward, last_draw_time) " +
                      "VALUES (?, ?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE draw_count = ?, last_pity_reward = ?, last_draw_time = ?"
                    : "MERGE INTO gacha_pity (player_uuid, machine_id, rule_hash, draw_count, last_pity_reward, last_draw_time) " +
                      "KEY (player_uuid, machine_id, rule_hash) " +
                      "VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, machineId);
                    ps.setString(3, ruleHash);
                    ps.setInt(4, finalCount);
                    ps.setString(5, rewardIdToRecord);
                    ps.setLong(6, System.currentTimeMillis());

                    if (plugin.getDatabaseManager().isMySQL()) {
                        ps.setInt(7, finalCount);
                        ps.setString(8, rewardIdToRecord);
                        ps.setLong(9, System.currentTimeMillis());
                    }

                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    private int getCurrentCount(Connection conn, UUID playerUuid, String machineId, String ruleHash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT draw_count FROM gacha_pity WHERE player_uuid = ? AND machine_id = ? AND rule_hash = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, machineId);
            ps.setString(3, ruleHash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("draw_count");
            }
            return 0;
        }
    }

    /**
     * 获取玩家的抽奖记录（最近20次）
     */
    public void getPlayerGachaRecords(UUID playerUuid, Consumer<List<GachaRecord>> callback) {
        plugin.getDatabaseQueue().submit("getGachaRecords", conn -> {
            List<GachaRecord> records = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM gacha_records WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT 20")) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    records.add(new GachaRecord(
                        rs.getString("player_name"),
                        rs.getString("machine_id"),
                        rs.getString("reward_id"),
                        rs.getString("item_key"),
                        rs.getInt("amount"),
                        rs.getDouble("cost"),
                        rs.getLong("timestamp")
                    ));
                }
            }
            return records;
        }, callback, error -> {
            plugin.getLogger().warning("查询抽奖记录失败: " + error.getMessage());
            callback.accept(new ArrayList<>());
        });
    }

    /**
     * 抽奖记录数据类
     */
    public static class GachaRecord {
        private final String playerName;
        private final String machineId;
        private final String rewardId;
        private final String itemKey;
        private final int amount;
        private final double cost;
        private final long timestamp;

        public GachaRecord(String playerName, String machineId, String rewardId, String itemKey,
                          int amount, double cost, long timestamp) {
            this.playerName = playerName;
            this.machineId = machineId;
            this.rewardId = rewardId;
            this.itemKey = itemKey;
            this.amount = amount;
            this.cost = cost;
            this.timestamp = timestamp;
        }

        public String getPlayerName() { return playerName; }
        public String getMachineId() { return machineId; }
        public String getRewardId() { return rewardId; }
        public String getItemKey() { return itemKey; }
        public int getAmount() { return amount; }
        public double getCost() { return cost; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 清理旧的抽奖记录
     * @param days 清理多少天以前的数据
     * @param callback 回调函数，参数为删除的记录数
     */
    public void cleanupOldRecords(int days, java.util.function.Consumer<Integer> callback) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);

        plugin.getDatabaseQueue().submit("cleanupGachaRecords", conn -> {
            int deleted = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM gacha_records WHERE timestamp < ?")) {
                ps.setLong(1, cutoffTime);
                deleted = ps.executeUpdate();
            }
            return deleted;
        }, callback, error -> {
            plugin.getLogger().warning("清理抽奖记录失败: " + error.getMessage());
            callback.accept(0);
        });
    }
}
