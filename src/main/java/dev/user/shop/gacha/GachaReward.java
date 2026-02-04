package dev.user.shop.gacha;

import org.bukkit.inventory.ItemStack;

public class GachaReward {

    private final String id;
    private final String itemKey;
    private final int amount;
    private final double probability;
    private final String displayName;
    private final boolean broadcast;
    private ItemStack displayItem;

    public GachaReward(String id, String itemKey, int amount, double probability, String displayName, boolean broadcast) {
        this.id = id;
        this.itemKey = itemKey;
        this.amount = amount;
        this.probability = probability;
        this.displayName = displayName;
        this.broadcast = broadcast;
    }

    // Getters
    public String getId() { return id; }
    public String getItemKey() { return itemKey; }
    public int getAmount() { return amount; }
    public double getProbability() { return probability; }
    public String getDisplayName() { return displayName; }
    public boolean shouldBroadcast() { return broadcast; }

    public ItemStack getDisplayItem() { return displayItem; }
    public void setDisplayItem(ItemStack displayItem) { this.displayItem = displayItem; }

    /**
     * 获取稀有度等级（基于概率）
     */
    public int getRarityLevel() {
        if (probability <= 0.02) return 5; // 传说
        if (probability <= 0.05) return 4; // 史诗
        if (probability <= 0.10) return 3; // 稀有
        if (probability <= 0.20) return 2; // 精良
        return 1; // 普通
    }

    public String getRarityName() {
        return switch (getRarityLevel()) {
            case 5 -> "传说";
            case 4 -> "史诗";
            case 3 -> "稀有";
            case 2 -> "精良";
            default -> "普通";
        };
    }

    public String getRarityColor() {
        return switch (getRarityLevel()) {
            case 5 -> "§6"; // 金色
            case 4 -> "§5"; // 紫色
            case 3 -> "§9"; // 蓝色
            case 2 -> "§a"; // 绿色
            default -> "§f"; // 白色
        };
    }
}
