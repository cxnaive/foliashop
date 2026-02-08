package dev.user.shop.gacha;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.util.ItemUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

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
            // 是否启用，默认true
            boolean enabled = machineSection.getBoolean("enabled", true);

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

            // 跳过禁用的扭蛋机
            if (!enabled) {
                plugin.getLogger().info("扭蛋机 '" + machineId + "' 已禁用，跳过加载");
                continue;
            }

            // 加载展示实体覆盖配置
            DisplayEntityConfig displayConfig = DisplayEntityConfig.fromConfig(
                machineSection.getConfigurationSection("display-entity")
            );

            // 加载 ICON NBT 组件配置
            Map<String, String> iconComponents = ItemUtil.parseComponents(
                machineSection.get("icon-components")
            );

            GachaMachine machine = new GachaMachine(
                machineId, name, description, icon, cost,
                animationDuration, animationDurationTen, broadcastRare, broadcastThreshold, slot, pityRules, enabled, displayConfig, iconComponents
            );

            // 加载奖品
            // 先尝试作为 ConfigurationSection 读取（支持 comments 和复杂结构）
            ConfigurationSection rewardsSection = machineSection.getConfigurationSection("rewards");
            List<Map<?, ?>> rewardsList = machineSection.getMapList("rewards");

            for (int i = 0; i < rewardsList.size(); i++) {
                Map<?, ?> rewardMap = rewardsList.get(i);
                String id = String.valueOf(rewardMap.get("id"));
                String itemKey = String.valueOf(rewardMap.get("item"));
                int amount = rewardMap.get("amount") instanceof Number ? ((Number) rewardMap.get("amount")).intValue() : 1;
                double probability = rewardMap.get("probability") instanceof Number ? ((Number) rewardMap.get("probability")).doubleValue() : 0.1;
                String displayName = rewardMap.get("display-name") != null ? String.valueOf(rewardMap.get("display-name")) : null;
                boolean broadcast = rewardMap.get("broadcast") instanceof Boolean ? (Boolean) rewardMap.get("broadcast") : false;

                if (displayName == null) {
                    displayName = itemKey;
                }

                // 加载奖品 NBT 组件配置
                // 直接从 rewardMap 获取 components（getMapList 已经把 YAML 列表项转为 Map）
                Map<String, String> rewardComponents = ItemUtil.parseComponents(rewardMap.get("components"));

                GachaReward reward = new GachaReward(id, itemKey, amount, probability, displayName, broadcast, rewardComponents);

                // 创建显示物品并应用 NBT 组件
                ItemStack item = ItemUtil.createItemFromKey(plugin, itemKey);
                if (item != null) {
                    if (!rewardComponents.isEmpty()) {
                        item = ItemUtil.applyComponents(item, rewardComponents);
                    }
                    reward.setDisplayItem(item);
                }

                machine.addReward(reward);
            }

            machines.put(machineId, machine);

            // 检查总概率
            double totalProb = machine.getTotalProbability();
            if (Math.abs(totalProb - 1.0) > 0.001) {
                plugin.getLogger().warning("扭蛋机 '" + machineId + "' 的总概率为 " + String.format("%.2f", totalProb) + "，建议调整为 1.0");
            }
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

    /**
     * 执行10连抽（带保底计算）
     * @param machine 扭蛋机
     * @param counters 当前计数器（会被修改）
     * @return 包含抽奖结果和满足规则的结果对象
     */
    public TenGachaResult performTenGacha(GachaMachine machine, Map<String, Integer> counters) {
        List<GachaReward> rewards = new ArrayList<>();
        Set<PityRule> satisfiedRulesSet = new HashSet<>();
        List<PityRule> allRules = machine.getPityRules();

        for (int i = 0; i < 10; i++) {
            // 使用保底抽奖
            GachaMachine.PityResult result = machine.rollWithPity(counters);
            GachaReward reward = result.reward();

            // 获取奖品满足的所有保底规则
            List<PityRule> rewardSatisfiedRules = machine.getSatisfiedPityRules(reward);
            satisfiedRulesSet.addAll(rewardSatisfiedRules);

            // 内存中模拟计数器变化
            for (PityRule rule : allRules) {
                String hash = rule.getRuleHash();
                if (rewardSatisfiedRules.contains(rule)) {
                    counters.put(hash, 0);
                } else {
                    counters.put(hash, counters.getOrDefault(hash, 0) + 1);
                }
            }

            rewards.add(reward);
        }

        // 计算最高稀有度（maxProbability 最小的规则）
        PityRule highestRule = null;
        for (PityRule rule : satisfiedRulesSet) {
            if (highestRule == null || rule.getMaxProbability() < highestRule.getMaxProbability()) {
                highestRule = rule;
            }
        }

        return new TenGachaResult(rewards, new ArrayList<>(satisfiedRulesSet), highestRule);
    }

    /**
     * 10连抽结果
     */
    public record TenGachaResult(List<GachaReward> rewards, List<PityRule> satisfiedRules, PityRule highestRule) {
        /**
         * 获取最高稀有度的百分比显示
         */
        public String getHighestRarityPercent() {
            if (highestRule == null) return null;
            double prob = highestRule.getMaxProbability();
            return String.format("%.1f%%", prob * 100);
        }
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
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        counters.put(rs.getString("rule_hash"), rs.getInt("draw_count"));
                    }
                }
            }
            return counters;
        }, callback, error -> {
            plugin.getLogger().warning("获取保底计数失败: " + error.getMessage());
            callback.accept(new HashMap<>());
        });
    }

    /**
     * 更新保底计数（单抽逻辑）
     * 复用 batchUpdatePityCounters 实现，避免代码重复
     * @param rules 所有保底规则列表
     * @param satisfiedRules 奖品满足的规则列表（这些规则的计数器重置为0）
     * @param rewardId 奖励ID
     */
    public void updatePityCounters(UUID playerUuid, String machineId, List<PityRule> rules, List<PityRule> satisfiedRules, String rewardId) {
        plugin.getDatabaseQueue().submit("updatePityCounters", conn -> {
            // 构建满足规则的哈希集合
            java.util.Set<String> satisfiedHashes = satisfiedRules.stream()
                .map(PityRule::getRuleHash)
                .collect(java.util.HashSet::new, java.util.HashSet::add, java.util.HashSet::addAll);

            // 查询当前计数
            java.util.Map<String, Integer> currentCounts = new java.util.HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT rule_hash, draw_count FROM gacha_pity WHERE player_uuid = ? AND machine_id = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, machineId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        currentCounts.put(rs.getString("rule_hash"), rs.getInt("draw_count"));
                    }
                }
            }

            // 计算新的计数器值
            java.util.Map<String, Integer> newCounters = new java.util.HashMap<>();
            for (PityRule rule : rules) {
                String ruleHash = rule.getRuleHash();
                if (satisfiedHashes.contains(ruleHash)) {
                    newCounters.put(ruleHash, 0); // 触发保底，重置为0
                } else {
                    newCounters.put(ruleHash, currentCounts.getOrDefault(ruleHash, 0) + 1);
                }
            }

            // 确定最后触发的保底规则（单抽场景最多只有一个）
            PityRule lastTriggeredRule = satisfiedRules.isEmpty() ? null : satisfiedRules.get(0);

            // 调用内部批量更新方法
            doBatchUpdatePityCounters(conn, playerUuid, machineId, rules, newCounters, lastTriggeredRule, rewardId);
            return null;
        });
    }

    /**
     * 内部方法：执行批量更新保底计数（实际的 SQL 执行逻辑）
     * 被 updatePityCounters 和 batchUpdatePityCounters 复用
     */
    private void doBatchUpdatePityCounters(Connection conn, UUID playerUuid, String machineId, List<PityRule> rules,
                                           java.util.Map<String, Integer> counters, PityRule lastTriggeredRule, String lastRewardId) throws SQLException {
        // 收集所有规则哈希
        List<String> ruleHashes = rules.stream()
            .map(PityRule::getRuleHash)
            .toList();

        // 批量查询当前的 last_pity_reward - 用于 H2 保持原值
        Map<String, String> currentRewards = new HashMap<>();
        boolean isMySQL = plugin.getDatabaseManager().isMySQL();

        if (!isMySQL && !ruleHashes.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(ruleHashes.size(), "?"));
            String selectSql = "SELECT rule_hash, last_pity_reward FROM gacha_pity " +
                              "WHERE player_uuid = ? AND machine_id = ? AND rule_hash IN (" + placeholders + ")";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, machineId);
                for (int i = 0; i < ruleHashes.size(); i++) {
                    ps.setString(3 + i, ruleHashes.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        currentRewards.put(rs.getString("rule_hash"), rs.getString("last_pity_reward"));
                    }
                }
            }
        }

        // 批量更新 - 使用批处理
        long currentTime = System.currentTimeMillis();
        String triggeredHash = lastTriggeredRule != null ? lastTriggeredRule.getRuleHash() : null;

        if (isMySQL) {
            // MySQL: 使用批处理
            String triggeredSql = "INSERT INTO gacha_pity (player_uuid, machine_id, rule_hash, draw_count, last_pity_reward, last_draw_time) " +
                                  "VALUES (?, ?, ?, ?, ?, ?) " +
                                  "ON DUPLICATE KEY UPDATE draw_count = ?, last_pity_reward = ?, last_draw_time = ?";
            String untriggeredSql = "INSERT INTO gacha_pity (player_uuid, machine_id, rule_hash, draw_count, last_pity_reward, last_draw_time) " +
                                    "VALUES (?, ?, ?, ?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE draw_count = ?, last_draw_time = ?";

            try (PreparedStatement triggeredPs = conn.prepareStatement(triggeredSql);
                 PreparedStatement untriggeredPs = conn.prepareStatement(untriggeredSql)) {

                for (PityRule rule : rules) {
                    String ruleHash = rule.getRuleHash();
                    int finalCount = counters.getOrDefault(ruleHash, 0);
                    boolean isTriggered = ruleHash.equals(triggeredHash);

                    if (isTriggered) {
                        triggeredPs.setString(1, playerUuid.toString());
                        triggeredPs.setString(2, machineId);
                        triggeredPs.setString(3, ruleHash);
                        triggeredPs.setInt(4, finalCount);
                        triggeredPs.setString(5, lastRewardId);
                        triggeredPs.setLong(6, currentTime);
                        triggeredPs.setInt(7, finalCount);
                        triggeredPs.setString(8, lastRewardId);
                        triggeredPs.setLong(9, currentTime);
                        triggeredPs.addBatch();
                    } else {
                        untriggeredPs.setString(1, playerUuid.toString());
                        untriggeredPs.setString(2, machineId);
                        untriggeredPs.setString(3, ruleHash);
                        untriggeredPs.setInt(4, finalCount);
                        untriggeredPs.setString(5, null);
                        untriggeredPs.setLong(6, currentTime);
                        untriggeredPs.setInt(7, finalCount);
                        untriggeredPs.setLong(8, currentTime);
                        untriggeredPs.addBatch();
                    }
                }

                triggeredPs.executeBatch();
                untriggeredPs.executeBatch();
            }
        } else {
            // H2: 使用批处理
            String sql = "MERGE INTO gacha_pity KEY(player_uuid, machine_id, rule_hash) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (PityRule rule : rules) {
                    String ruleHash = rule.getRuleHash();
                    int finalCount = counters.getOrDefault(ruleHash, 0);
                    boolean isTriggered = ruleHash.equals(triggeredHash);
                    String lastReward = isTriggered ? lastRewardId : currentRewards.get(ruleHash);

                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, machineId);
                    ps.setString(3, ruleHash);
                    ps.setInt(4, finalCount);
                    ps.setString(5, lastReward);
                    ps.setLong(6, currentTime);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
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
            doBatchUpdatePityCounters(conn, playerUuid, machineId, rules, counters, lastTriggeredRule, lastRewardId);
            return null;
        });
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
                try (ResultSet rs = ps.executeQuery()) {
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
