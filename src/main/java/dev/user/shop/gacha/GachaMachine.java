package dev.user.shop.gacha;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class GachaMachine {

    private final String id;
    private final String name;
    private final List<String> description;
    private final String icon;
    private final double cost;
    private final int animationDuration;
    private final int animationDurationTen; // 10连抽动画时间（秒）
    private final boolean broadcastRare;
    private final double broadcastThreshold;
    private final int slot; // 在选择界面的位置
    private final List<PityRule> pityRules; // 保底规则列表
    private final List<GachaReward> rewards;

    private double totalProbability;
    private List<org.bukkit.inventory.ItemStack> cachedAnimationItems;

    public GachaMachine(String id, String name, List<String> description, String icon, double cost,
                        int animationDuration, int animationDurationTen, boolean broadcastRare, double broadcastThreshold, int slot,
                        List<PityRule> pityRules) {
        this.id = id;
        this.name = name;
        this.description = description != null ? description : new ArrayList<>();
        this.icon = icon;
        this.cost = cost;
        this.animationDuration = animationDuration;
        this.animationDurationTen = animationDurationTen;
        this.broadcastRare = broadcastRare;
        this.broadcastThreshold = broadcastThreshold;
        this.slot = slot;
        this.pityRules = pityRules != null ? pityRules.stream()
            .sorted(Comparator.comparingInt(PityRule::getCount).reversed()) // 按次数降序，先检查高保底
            .collect(Collectors.toList()) : new ArrayList<>();
        this.rewards = new ArrayList<>();
    }

    public void addReward(GachaReward reward) {
        rewards.add(reward);
        recalculateProbabilities();
    }

    private void recalculateProbabilities() {
        totalProbability = 0;
        for (GachaReward reward : rewards) {
            totalProbability += reward.getProbability();
        }
    }

    /**
     * 随机抽取一个奖品
     */
    public GachaReward roll() {
        if (rewards.isEmpty()) return null;

        double random = Math.random() * totalProbability;
        double current = 0;

        for (GachaReward reward : rewards) {
            current += reward.getProbability();
            if (random <= current) {
                return reward;
            }
        }

        // 兜底返回最后一个
        return rewards.get(rewards.size() - 1);
    }

    /**
     * 获取指定稀有度及以上的奖品（用于动画展示）
     */
    public List<GachaReward> getRarifiedRewards(int minRarity) {
        List<GachaReward> result = new ArrayList<>();
        for (GachaReward reward : rewards) {
            if (reward.getRarityLevel() >= minRarity) {
                result.add(reward);
            }
        }
        return result.isEmpty() ? rewards : result;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getDescription() { return description; }
    public String getIcon() { return icon; }
    public double getCost() { return cost; }
    public int getAnimationDuration() { return animationDuration; }
    public int getAnimationDurationTen() { return animationDurationTen; }
    public boolean isBroadcastRare() { return broadcastRare; }
    public double getBroadcastThreshold() { return broadcastThreshold; }
    public List<GachaReward> getRewards() { return new ArrayList<>(rewards); }

    public int getSlot() { return slot; }

    public List<PityRule> getPityRules() { return pityRules; }

    public boolean hasPityRules() { return !pityRules.isEmpty(); }

    /**
     * 检查指定计数器是否触发保底
     * @param counters 各规则的计数器 Map<ruleHash, count>
     * @return 触发的保底规则，未触发返回null
     */
    public PityRule checkPityTrigger(Map<String, Integer> counters) {
        // 从高到低检查，优先触发高档
        for (int i = pityRules.size() - 1; i >= 0; i--) {
            PityRule rule = pityRules.get(i);
            int count = counters.getOrDefault(rule.getRuleHash(), 0);
            if (count >= rule.getCount()) {
                return rule;
            }
        }
        return null;
    }

    /**
     * 获取保底奖品池（概率小于等于maxProbability的奖品）
     * @param maxProbability 最大概率阈值
     * @return 符合条件的奖品列表
     */
    public List<GachaReward> getPityRewards(double maxProbability) {
        return rewards.stream()
            .filter(r -> r.getProbability() <= maxProbability)
            .collect(Collectors.toList());
    }

    /**
     * 带保底的抽奖
     * @param counters 各规则的计数器 Map<ruleHash, count>
     * @return 抽奖结果和触发的规则（null表示未触发）
     */
    public PityResult rollWithPity(Map<String, Integer> counters) {
        // 检查是否触发保底（从高到低）
        PityRule triggeredRule = checkPityTrigger(counters);
        if (triggeredRule != null) {
            List<GachaReward> pityRewards = getPityRewards(triggeredRule.getMaxProbability());
            if (!pityRewards.isEmpty()) {
                GachaReward reward = pityRewards.get((int) (Math.random() * pityRewards.size()));
                return new PityResult(reward, triggeredRule);
            }
        }
        // 正常随机
        return new PityResult(roll(), null);
    }

    /**
     * 保底抽奖结果
     */
    public record PityResult(GachaReward reward, PityRule triggeredRule) {
    }

    public boolean shouldBroadcast(GachaReward reward) {
        return broadcastRare && reward.getProbability() <= broadcastThreshold;
    }

    /**
     * 获取动画物品列表（带缓存）
     */
    public List<org.bukkit.inventory.ItemStack> getAnimationItems() {
        if (cachedAnimationItems == null) {
            cachedAnimationItems = new ArrayList<>();
            for (GachaReward reward : rewards) {
                org.bukkit.inventory.ItemStack item = reward.getDisplayItem();
                if (item != null) {
                    cachedAnimationItems.add(item.clone());
                }
            }
            Collections.shuffle(cachedAnimationItems);
        }
        return new ArrayList<>(cachedAnimationItems);
    }

    /**
     * 清除动画物品缓存（当奖品改变时调用）
     */
    public void clearAnimationCache() {
        cachedAnimationItems = null;
    }

    public ItemStack createIconItem(FoliaShopPlugin plugin) {
        ItemStack item = ItemUtil.createItemFromKey(plugin, icon);
        if (item == null) {
            item = new ItemStack(Material.CHEST);
        }
        return item;
    }
}
