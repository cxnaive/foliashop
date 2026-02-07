package dev.user.shop;

import dev.user.shop.command.FoliaShopCommand;
import dev.user.shop.command.GachaCommand;
import dev.user.shop.command.ShopCommand;
import dev.user.shop.config.ShopConfig;
import dev.user.shop.database.DatabaseManager;
import dev.user.shop.database.DatabaseQueue;
import dev.user.shop.economy.EconomyManager;
import dev.user.shop.gacha.GachaBlockManager;
import dev.user.shop.gacha.GachaDisplayManager;
import dev.user.shop.gacha.GachaManager;
import dev.user.shop.gui.GUIManager;
import dev.user.shop.listener.BlockInteractListener;
import dev.user.shop.listener.ChunkListener;
import dev.user.shop.listener.GUIListener;
import dev.user.shop.shop.ShopManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FoliaShopPlugin extends JavaPlugin {

    private static FoliaShopPlugin instance;

    private ShopConfig shopConfig;
    private DatabaseManager databaseManager;
    private DatabaseQueue databaseQueue;
    private EconomyManager economyManager;
    private ShopManager shopManager;
    private GachaManager gachaManager;
    private GachaBlockManager gachaBlockManager;
    private GachaDisplayManager gachaDisplayManager;

    @Override
    public void onEnable() {
        instance = this;

        // 保存默认配置
        saveDefaultConfig();

        // 初始化配置
        this.shopConfig = new ShopConfig(this);

        // 初始化数据库
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.init()) {
            getLogger().severe("数据库初始化失败，插件将禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 检查跨服配置
        if (shopConfig.getDatabaseType().equalsIgnoreCase("h2")) {
            getLogger().warning("============================================");
            getLogger().warning("当前使用 H2 数据库（本地文件模式）");
            getLogger().warning("H2 不支持多服务器同时访问！");
            getLogger().warning("如需跨服部署，请改用 MySQL 数据库");
            getLogger().warning("跨服使用 H2 会导致数据不一致和文件锁冲突");
            getLogger().warning("============================================");
        } else {
            getLogger().info("使用 MySQL 数据库，支持跨服部署");
        }

        // 初始化数据库队列
        this.databaseQueue = new DatabaseQueue(this);

        // 初始化经济系统
        this.economyManager = new EconomyManager(this);
        economyManager.init();

        

        // 延迟初始化商店和扭蛋管理器（等待 CraftEngine 注册物品）
        getServer().getGlobalRegionScheduler().runDelayed(this, t -> {
            // 初始化商店管理器
            this.shopManager = new ShopManager(this);

            // 初始化扭蛋管理器
            this.gachaManager = new GachaManager(this);

            // 初始化扭蛋机方块绑定管理器
            this.gachaBlockManager = new GachaBlockManager(this);

            // 初始化扭蛋机展示实体管理器
            this.gachaDisplayManager = new GachaDisplayManager(this);
            this.gachaDisplayManager.loadAllDisplays();

            getLogger().info("商店和扭蛋系统已加载完成！");
        }, 2L);

        // 注册命令（提前注册，不影响命令使用）
        registerCommands();

        // 注册监听器
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkListener(this), this);

        getLogger().info("FoliaShop 插件已启用！");
    }

    @Override
    public void onDisable() {
        // 先关闭所有打开的GUI（包括取消扭蛋动画）
        GUIManager.closeAllGUIs();

        // 关闭经济队列（等待所有任务完成）
        if (economyManager != null) {
            economyManager.shutdown();
        }

        // 关闭数据库队列（等待所有任务完成）
        if (databaseQueue != null) {
            databaseQueue.shutdown();
        }

        // 关闭数据库连接池
        if (databaseManager != null) {
            databaseManager.close();
        }

        // 清理商店和扭蛋管理器
        if (shopManager != null) {
            shopManager = null;
        }
        if (gachaManager != null) {
            gachaManager = null;
        }

        getLogger().info("FoliaShop 插件已禁用！");
    }

    private void registerCommands() {
        FoliaShopCommand foliaShopCommand = new FoliaShopCommand(this);
        getCommand("foliashop").setExecutor(foliaShopCommand);
        getCommand("foliashop").setTabCompleter(foliaShopCommand);

        ShopCommand shopCommand = new ShopCommand(this);
        getCommand("shop").setExecutor(shopCommand);
        getCommand("shop").setTabCompleter(shopCommand);

        GachaCommand gachaCommand = new GachaCommand(this);
        getCommand("gacha").setExecutor(gachaCommand);
        getCommand("gacha").setTabCompleter(gachaCommand);
    }

    public void reload() {
        reloadConfig();
        shopConfig.load();
        if (shopManager != null) {
            shopManager.reload();
        }
        if (gachaManager != null) {
            gachaManager.reload();
        }
        if (gachaDisplayManager != null) {
            gachaDisplayManager.reload();
        }
    }

    public static FoliaShopPlugin getInstance() {
        return instance;
    }

    public ShopConfig getShopConfig() {
        return shopConfig;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DatabaseQueue getDatabaseQueue() {
        return databaseQueue;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ShopManager getShopManager() {
        // 如果延迟加载未完成，先初始化
        if (shopManager == null) {
            getLogger().warning("ShopManager 未初始化，正在紧急初始化...");
            this.shopManager = new ShopManager(this);
        }
        return shopManager;
    }

    public GachaManager getGachaManager() {
        // 如果延迟加载未完成，先初始化
        if (gachaManager == null) {
            getLogger().warning("GachaManager 未初始化，正在紧急初始化...");
            this.gachaManager = new GachaManager(this);
        }
        return gachaManager;
    }

    public GachaBlockManager getGachaBlockManager() {
        // 如果延迟加载未完成，先初始化
        if (gachaBlockManager == null) {
            getLogger().warning("GachaBlockManager 未初始化，正在紧急初始化...");
            this.gachaBlockManager = new GachaBlockManager(this);
        }
        return gachaBlockManager;
    }

    public GachaDisplayManager getGachaDisplayManager() {
        // 如果延迟加载未完成，先初始化
        if (gachaDisplayManager == null) {
            getLogger().warning("GachaDisplayManager 未初始化，正在紧急初始化...");
            this.gachaDisplayManager = new GachaDisplayManager(this);
        }
        return gachaDisplayManager;
    }
}
