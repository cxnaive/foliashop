package dev.user.shop.config;

import dev.user.shop.FoliaShopPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ShopConfig {

    private final FoliaShopPlugin plugin;
    private FileConfiguration config;

    // 数据库设置
    private String databaseType;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private int mysqlPoolSize;
    private String h2Filename;

    // 经济设置
    private boolean economyEnabled;
    private String currencyName;
    private String currencyFormat;

    // 商店设置
    private boolean shopEnabled;
    private String shopTitle;
    private boolean allowSell;
    private double sellDiscount;
    private boolean logTransactions;
    private int refreshInterval;
    private int dailyBuyLimit;

    // 系统回收设置
    private boolean sellSystemEnabled;
    private String sellSystemMode;
    private boolean addStockOnSell;
    private Map<String, Double> customSellItems;

    // 扭蛋设置
    private boolean gachaEnabled;

    // GUI设置
    private Map<String, String> guiTitles;
    private Map<String, ItemConfig> guiDecorations;

    // 消息
    private Map<String, String> messages;

    public ShopConfig(FoliaShopPlugin plugin) {
        this.plugin = plugin;
        this.guiTitles = new HashMap<>();
        this.guiDecorations = new HashMap<>();
        this.messages = new HashMap<>();
        load();
    }

    public void load() {
        this.config = plugin.getConfig();

        // 数据库设置
        this.databaseType = config.getString("database.type", "h2");
        this.mysqlHost = config.getString("database.mysql.host", "localhost");
        this.mysqlPort = config.getInt("database.mysql.port", 3306);
        this.mysqlDatabase = config.getString("database.mysql.database", "foliashop");
        this.mysqlUsername = config.getString("database.mysql.username", "root");
        this.mysqlPassword = config.getString("database.mysql.password", "password");
        this.mysqlPoolSize = config.getInt("database.mysql.pool-size", 10);
        this.h2Filename = config.getString("database.h2.filename", "foliashop");

        // 经济设置
        this.economyEnabled = config.getBoolean("economy.enabled", true);
        this.currencyName = config.getString("economy.currency-name", "金币");
        this.currencyFormat = config.getString("economy.currency-format", "{amount} {currency}");

        // 商店设置
        this.shopEnabled = config.getBoolean("shop.enabled", true);
        this.shopTitle = config.getString("shop.title", "系统商店");
        this.allowSell = config.getBoolean("shop.allow-sell", true);
        this.sellDiscount = config.getDouble("shop.sell-discount", 0.7);
        this.logTransactions = config.getBoolean("shop.log-transactions", true);
        this.refreshInterval = config.getInt("shop.refresh-interval", 0);
        this.dailyBuyLimit = config.getInt("shop.daily-buy-limit", 0);

        // 系统回收设置
        this.sellSystemEnabled = config.getBoolean("shop.sell-system.enabled", true);
        this.sellSystemMode = config.getString("shop.sell-system.mode", "SHOP_ONLY").toUpperCase();
        this.addStockOnSell = config.getBoolean("shop.sell-system.add-stock-on-sell", false);
        this.customSellItems = new HashMap<>();
        ConfigurationSection customItemsSection = config.getConfigurationSection("shop.sell-system.custom-items");
        if (customItemsSection != null) {
            for (String itemKey : customItemsSection.getKeys(false)) {
                double price = customItemsSection.getDouble(itemKey, 0);
                if (price > 0) {
                    customSellItems.put(itemKey, price);
                }
            }
        }

        // 扭蛋设置
        this.gachaEnabled = config.getBoolean("gacha.enabled", true);

        // GUI标题
        ConfigurationSection titlesSection = config.getConfigurationSection("gui.titles");
        if (titlesSection != null) {
            for (String key : titlesSection.getKeys(false)) {
                guiTitles.put(key, titlesSection.getString(key));
            }
        }

        // GUI装饰
        ConfigurationSection decorationSection = config.getConfigurationSection("gui.decoration");
        if (decorationSection != null) {
            for (String key : decorationSection.getKeys(false)) {
                ConfigurationSection itemSection = decorationSection.getConfigurationSection(key);
                if (itemSection != null) {
                    guiDecorations.put(key, new ItemConfig(
                        itemSection.getString("material", "minecraft:stone"),
                        itemSection.getString("name", "")
                    ));
                }
            }
        }

        // 消息
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key));
            }
        }
    }

    // Getters
    public String getDatabaseType() { return databaseType; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }
    public int getMysqlPoolSize() { return mysqlPoolSize; }
    public String getH2Filename() { return h2Filename; }

    public boolean isEconomyEnabled() { return economyEnabled; }
    public String getCurrencyName() { return currencyName; }
    public String getCurrencyFormat() { return currencyFormat; }
    public String formatCurrency(double amount) {
        return currencyFormat.replace("{amount}", String.format("%.2f", amount))
                           .replace("{currency}", currencyName);
    }

    public boolean isShopEnabled() { return shopEnabled; }
    public String getShopTitle() { return shopTitle; }
    public boolean isAllowSell() { return allowSell; }
    public double getSellDiscount() { return sellDiscount; }
    public boolean isLogTransactions() { return logTransactions; }
    public int getRefreshInterval() { return refreshInterval; }
    public int getDailyBuyLimit() { return dailyBuyLimit; }

    // 系统回收设置 Getter
    public boolean isSellSystemEnabled() { return sellSystemEnabled; }
    public String getSellSystemMode() { return sellSystemMode; }
    public boolean isAddStockOnSell() { return addStockOnSell; }
    public Map<String, Double> getCustomSellItems() { return customSellItems; }
    public double getCustomSellPrice(String itemKey) { return customSellItems.getOrDefault(itemKey, 0.0); }

    public boolean isGachaEnabled() { return gachaEnabled; }

    public String getGUITitle(String key) {
        // 提供有意义的默认值，当配置文件中没有时显示
        String title = guiTitles.getOrDefault(key, switch (key) {
            case "main-menu" -> "主菜单";
            case "shop" -> "系统商店";
            case "gacha" -> "扭蛋中心";
            case "gacha-animation" -> "扭蛋抽奖中...";
            case "gacha-ten-animation" -> "10连抽抽奖中...";
            case "gacha-ten-result" -> "10连抽结果";
            case "gacha-result" -> "抽奖结果";
            case "gacha-history" -> "抽奖记录";
            case "sell-menu" -> "出售物品";
            case "transaction-history" -> "交易记录";
            default -> "菜单";
        });
        return convertMiniMessage(title);
    }

    public ItemConfig getGUIDecoration(String key) {
        return guiDecorations.get(key);
    }

    public String getMessage(String key) {
        String msg = messages.get(key);
        // 为某些消息提供默认值
        if (msg == null) {
            msg = switch (key) {
                case "sell-success-batch" -> "<green>✔ 成功出售 <yellow>{count}</green> 种物品共 <yellow>{total}</yellow> 个，获得 <yellow>{reward} {currency}";
                default -> "";
            };
        }
        msg = msg.replace("<prefix>", messages.getOrDefault("prefix", ""));
        return convertMiniMessage(msg);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        // 先转换占位符值中的 MiniMessage 为 § 格式
        Map<String, String> convertedPlaceholders = new HashMap<>();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            convertedPlaceholders.put(entry.getKey(), convertMiniMessage(entry.getValue()));
        }

        String msg = getMessage(key);
        for (Map.Entry<String, String> entry : convertedPlaceholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }

    /**
     * 将 MiniMessage 格式转换为传统 § 颜色代码
     */
    public String convertMiniMessage(String message) {
        if (message == null) return "";

        // 简单替换常见的 MiniMessage 标签为传统颜色代码
        return message
            .replace("<black>", "§0").replace("</black>", "")
            .replace("<dark_blue>", "§1").replace("</dark_blue>", "")
            .replace("<dark_green>", "§2").replace("</dark_green>", "")
            .replace("<dark_aqua>", "§3").replace("</dark_aqua>", "")
            .replace("<dark_red>", "§4").replace("</dark_red>", "")
            .replace("<dark_purple>", "§5").replace("</dark_purple>", "")
            .replace("<gold>", "§6").replace("</gold>", "")
            .replace("<gray>", "§7").replace("</gray>", "")
            .replace("<dark_gray>", "§8").replace("</dark_gray>", "")
            .replace("<blue>", "§9").replace("</blue>", "")
            .replace("<green>", "§a").replace("</green>", "")
            .replace("<aqua>", "§b").replace("</aqua>", "")
            .replace("<red>", "§c").replace("</red>", "")
            .replace("<light_purple>", "§d").replace("</light_purple>", "")
            .replace("<yellow>", "§e").replace("</yellow>", "")
            .replace("<white>", "§f").replace("</white>", "")
            .replace("<bold>", "§l").replace("</bold>", "")
            .replace("<italic>", "§o").replace("</italic>", "")
            .replace("<underline>", "§n").replace("</underline>", "")
            .replace("<strikethrough>", "§m").replace("</strikethrough>", "")
            .replace("<obfuscated>", "§k").replace("</obfuscated>", "")
            .replace("<reset>", "§r").replace("</reset>", "");
    }

    public ConfigurationSection getShopCategories() {
        return config.getConfigurationSection("shop.categories");
    }

    public ConfigurationSection getShopItems() {
        return config.getConfigurationSection("shop.items");
    }

    public ConfigurationSection getGachaMachines() {
        return config.getConfigurationSection("gacha.machines");
    }

    public static class ItemConfig {
        private final String material;
        private final String name;

        public ItemConfig(String material, String name) {
            this.material = material;
            this.name = name;
        }

        public String getMaterial() { return material; }
        public String getName() { return name; }

        public Material getBukkitMaterial() {
            String mat = material.toUpperCase();
            if (mat.startsWith("MINECRAFT:")) {
                mat = mat.substring(10);
            }
            try {
                return Material.valueOf(mat);
            } catch (IllegalArgumentException e) {
                return Material.STONE;
            }
        }
    }
}
