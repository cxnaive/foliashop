package dev.user.shop.config;

import dev.user.shop.FoliaShopPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ShopConfig {

    private final FoliaShopPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration shopConfig;
    private FileConfiguration gachaConfig;

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

    // 展示实体设置
    private boolean displayEntityEnabled;
    private float displayEntityScale;
    private float displayEntityRotationY;
    private boolean displayEntityFacePlayer;
    private boolean displayEntityFloatingAnimation;
    private float displayEntityFloatAmplitude;
    private float displayEntityFloatSpeed;
    private float displayEntityHeightOffset;
    private int displayEntityAnimationPeriod;
    private float displayEntityViewRange;
    private float displayEntityShadowRadius;
    private float displayEntityShadowStrength;

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

    /**
     * 加载或创建单独的配置文件
     */
    private FileConfiguration loadOrCreateConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * 从主配置或单独配置获取值（优先使用单独配置）
     */
    private String getString(String path, String defaultValue) {
        // 优先从单独配置读取
        if (shopConfig != null && shopConfig.contains(path)) {
            return shopConfig.getString(path, defaultValue);
        }
        if (gachaConfig != null && gachaConfig.contains(path)) {
            return gachaConfig.getString(path, defaultValue);
        }
        // 回退到主配置
        return config.getString(path, defaultValue);
    }

    private boolean getBoolean(String path, boolean defaultValue) {
        if (shopConfig != null && shopConfig.contains(path)) {
            return shopConfig.getBoolean(path, defaultValue);
        }
        if (gachaConfig != null && gachaConfig.contains(path)) {
            return gachaConfig.getBoolean(path, defaultValue);
        }
        return config.getBoolean(path, defaultValue);
    }

    private double getDouble(String path, double defaultValue) {
        if (shopConfig != null && shopConfig.contains(path)) {
            return shopConfig.getDouble(path, defaultValue);
        }
        if (gachaConfig != null && gachaConfig.contains(path)) {
            return gachaConfig.getDouble(path, defaultValue);
        }
        return config.getDouble(path, defaultValue);
    }

    private int getInt(String path, int defaultValue) {
        if (shopConfig != null && shopConfig.contains(path)) {
            return shopConfig.getInt(path, defaultValue);
        }
        if (gachaConfig != null && gachaConfig.contains(path)) {
            return gachaConfig.getInt(path, defaultValue);
        }
        return config.getInt(path, defaultValue);
    }

    private ConfigurationSection getConfigurationSection(String path) {
        if (shopConfig != null) {
            ConfigurationSection section = shopConfig.getConfigurationSection(path);
            if (section != null) return section;
        }
        if (gachaConfig != null) {
            ConfigurationSection section = gachaConfig.getConfigurationSection(path);
            if (section != null) return section;
        }
        return config.getConfigurationSection(path);
    }

    public void load() {
        this.config = plugin.getConfig();

        // 加载单独的配置文件（如果存在）
        this.shopConfig = loadOrCreateConfig("shop.yml");
        this.gachaConfig = loadOrCreateConfig("gacha.yml");

        // 数据库设置（仅从主配置读取）
        this.databaseType = config.getString("database.type", "h2");
        this.mysqlHost = config.getString("database.mysql.host", "localhost");
        this.mysqlPort = config.getInt("database.mysql.port", 3306);
        this.mysqlDatabase = config.getString("database.mysql.database", "foliashop");
        this.mysqlUsername = config.getString("database.mysql.username", "root");
        this.mysqlPassword = config.getString("database.mysql.password", "password");
        this.mysqlPoolSize = config.getInt("database.mysql.pool-size", 10);
        this.h2Filename = config.getString("database.h2.filename", "foliashop");

        // 经济设置（仅从主配置读取）
        this.economyEnabled = config.getBoolean("economy.enabled", true);
        this.currencyName = config.getString("economy.currency-name", "金币");
        this.currencyFormat = config.getString("economy.currency-format", "{amount} {currency}");

        // 商店设置（优先从 shop.yml 读取，否则从 config.yml 读取）
        this.shopEnabled = getBoolean("shop.enabled", true);
        this.shopTitle = getString("shop.title", "系统商店");
        this.allowSell = getBoolean("shop.allow-sell", true);
        this.sellDiscount = getDouble("shop.sell-discount", 0.7);
        this.logTransactions = getBoolean("shop.log-transactions", true);
        this.refreshInterval = getInt("shop.refresh-interval", 0);
        this.dailyBuyLimit = getInt("shop.daily-buy-limit", 0);

        // 系统回收设置
        this.sellSystemEnabled = getBoolean("shop.sell-system.enabled", true);
        this.sellSystemMode = getString("shop.sell-system.mode", "SHOP_ONLY").toUpperCase();
        this.addStockOnSell = getBoolean("shop.sell-system.add-stock-on-sell", false);
        this.customSellItems = new HashMap<>();
        ConfigurationSection customItemsSection = getConfigurationSection("shop.sell-system.custom-items");
        if (customItemsSection != null) {
            for (String itemKey : customItemsSection.getKeys(false)) {
                double price = customItemsSection.getDouble(itemKey, 0);
                if (price > 0) {
                    customSellItems.put(itemKey, price);
                }
            }
        }

        // 扭蛋设置（优先从 gacha.yml 读取，否则从 config.yml 读取）
        this.gachaEnabled = getBoolean("gacha.enabled", true);

        // 展示实体设置
        this.displayEntityEnabled = getBoolean("gacha.display-entity.enabled", true);
        this.displayEntityScale = (float) getDouble("gacha.display-entity.scale", 0.8);
        this.displayEntityRotationY = (float) getDouble("gacha.display-entity.rotation-y", 45.0);
        this.displayEntityFacePlayer = getBoolean("gacha.display-entity.face-player", false);
        this.displayEntityFloatingAnimation = getBoolean("gacha.display-entity.floating-animation", true);
        this.displayEntityFloatAmplitude = (float) getDouble("gacha.display-entity.float-amplitude", 0.1);
        this.displayEntityFloatSpeed = (float) getDouble("gacha.display-entity.float-speed", 1.0);
        this.displayEntityHeightOffset = (float) getDouble("gacha.display-entity.height-offset", 1.5);
        this.displayEntityAnimationPeriod = getInt("gacha.display-entity.animation-period", 3);
        // 确保至少为1
        if (this.displayEntityAnimationPeriod < 1) {
            this.displayEntityAnimationPeriod = 1;
        }
        this.displayEntityViewRange = (float) getDouble("gacha.display-entity.view-range", 32.0);
        this.displayEntityShadowRadius = (float) getDouble("gacha.display-entity.shadow-radius", 0.3);
        this.displayEntityShadowStrength = (float) getDouble("gacha.display-entity.shadow-strength", 0.3);

        // GUI标题（仅从主配置读取）
        ConfigurationSection titlesSection = config.getConfigurationSection("gui.titles");
        if (titlesSection != null) {
            for (String key : titlesSection.getKeys(false)) {
                guiTitles.put(key, titlesSection.getString(key));
            }
        }

        // GUI装饰（仅从主配置读取）
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

        // 消息（仅从主配置读取）
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

    // 展示实体设置 Getters
    public boolean isDisplayEntityEnabled() { return displayEntityEnabled; }
    public float getDisplayEntityScale() { return displayEntityScale; }
    public float getDisplayEntityRotationY() { return displayEntityRotationY; }
    public boolean isDisplayEntityFacePlayer() { return displayEntityFacePlayer; }
    public boolean isDisplayEntityFloatingAnimation() { return displayEntityFloatingAnimation; }
    public float getDisplayEntityFloatAmplitude() { return displayEntityFloatAmplitude; }
    public float getDisplayEntityFloatSpeed() { return displayEntityFloatSpeed; }
    public float getDisplayEntityHeightOffset() { return displayEntityHeightOffset; }
    public int getDisplayEntityAnimationPeriod() { return displayEntityAnimationPeriod; }
    public float getDisplayEntityViewRange() { return displayEntityViewRange; }
    public float getDisplayEntityShadowRadius() { return displayEntityShadowRadius; }
    public float getDisplayEntityShadowStrength() { return displayEntityShadowStrength; }

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
        // 优先从 shop.yml 读取
        if (shopConfig != null) {
            ConfigurationSection section = shopConfig.getConfigurationSection("shop.categories");
            if (section != null) return section;
        }
        return config.getConfigurationSection("shop.categories");
    }

    public ConfigurationSection getShopItems() {
        // 优先从 shop.yml 读取
        if (shopConfig != null) {
            ConfigurationSection section = shopConfig.getConfigurationSection("shop.items");
            if (section != null) return section;
        }
        return config.getConfigurationSection("shop.items");
    }

    public ConfigurationSection getGachaMachines() {
        // 优先从 gacha.yml 读取
        if (gachaConfig != null) {
            ConfigurationSection section = gachaConfig.getConfigurationSection("gacha.machines");
            if (section != null) return section;
        }
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
