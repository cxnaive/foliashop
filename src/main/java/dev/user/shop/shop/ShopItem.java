package dev.user.shop.shop;

import org.bukkit.inventory.ItemStack;

public class ShopItem {

    private final String id;
    private final String itemKey;
    private ItemStack displayItem;
    private double buyPrice;
    private double sellPrice;
    private int stock;
    private String category;
    private int slot;
    private boolean enabled;
    private int dailyLimit; // 每日购买限额，0表示无限制

    public ShopItem(String id, String itemKey, double buyPrice, double sellPrice, int stock, String category, int slot) {
        this(id, itemKey, buyPrice, sellPrice, stock, category, slot, 0);
    }

    public ShopItem(String id, String itemKey, double buyPrice, double sellPrice, int stock, String category, int slot, int dailyLimit) {
        this.id = id;
        this.itemKey = itemKey;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.stock = stock;
        this.category = category;
        this.slot = slot;
        this.enabled = true;
        this.dailyLimit = dailyLimit;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getItemKey() { return itemKey; }

    public ItemStack getDisplayItem() { return displayItem; }
    public void setDisplayItem(ItemStack displayItem) { this.displayItem = displayItem; }

    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }

    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public boolean hasUnlimitedStock() { return stock < 0; }
    public boolean isInStock(int amount) { return stock < 0 || stock >= amount; }
    public void reduceStock(int amount) {
        if (stock > 0) {
            stock = Math.max(0, stock - amount);
        }
    }
    public void addStock(int amount) {
        if (stock >= 0) {
            stock += amount;
        }
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean canBuy() { return enabled && buyPrice > 0; }
    public boolean canSell() { return enabled && sellPrice > 0; }

    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
    public boolean hasDailyLimit() { return dailyLimit > 0; }
}
