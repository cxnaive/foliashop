package dev.user.shop.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.user.shop.FoliaShopPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final FoliaShopPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(FoliaShopPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        // 先关闭可能存在的旧连接（处理PlugManX重载情况）
        close();

        // 保存原始 ClassLoader
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 使用插件的 ClassLoader 作为上下文 ClassLoader
            // 这样 HikariCP 就能找到打包的 H2 驱动
            Thread.currentThread().setContextClassLoader(plugin.getClass().getClassLoader());

            String type = plugin.getShopConfig().getDatabaseType();

            if (type.equalsIgnoreCase("mysql")) {
                initMySQL();
            } else {
                initH2();
            }

            // 创建表
            createTables();

            plugin.getLogger().info("数据库连接成功！类型: " + type);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // 恢复原始 ClassLoader
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private void initMySQL() {
        HikariConfig config = new HikariConfig();
        String host = plugin.getShopConfig().getMysqlHost();
        int port = plugin.getShopConfig().getMysqlPort();
        String database = plugin.getShopConfig().getMysqlDatabase();
        String username = plugin.getShopConfig().getMysqlUsername();
        String password = plugin.getShopConfig().getMysqlPassword();
        int poolSize = plugin.getShopConfig().getMysqlPoolSize();

        // 手动注册 MySQL 驱动（使用重定位后的类名）
        try {
            Driver mysqlDriver = (Driver) Class.forName("dev.user.shop.libs.com.mysql.cj.jdbc.Driver", true, plugin.getClass().getClassLoader()).getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(mysqlDriver));
        } catch (Exception e) {
            plugin.getLogger().warning("MySQL 驱动注册失败（可能已注册）: " + e.getMessage());
        }

        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setDriverClassName("dev.user.shop.libs.com.mysql.cj.jdbc.Driver");

        dataSource = new HikariDataSource(config);
    }

    private void initH2() {
        HikariConfig config = new HikariConfig();
        String filename = plugin.getShopConfig().getH2Filename();
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // 手动注册 H2 驱动（使用重定位后的类名）
        try {
            Driver h2Driver = (Driver) Class.forName("dev.user.shop.libs.org.h2.Driver", true, plugin.getClass().getClassLoader()).getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(h2Driver));
        } catch (Exception e) {
            plugin.getLogger().warning("H2 驱动注册失败（可能已注册）: " + e.getMessage());
        }

        // DB_CLOSE_DELAY=0: 连接关闭时立即释放文件锁（对PlugMan重载很重要）
        // DB_CLOSE_ON_EXIT=FALSE: 防止VM关闭时自动关闭数据库
        // AUTO_RECONNECT=TRUE: 自动重连
        config.setJdbcUrl("jdbc:h2:" + new File(dataFolder, filename).getAbsolutePath() +
                ";AUTO_RECONNECT=TRUE;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setDriverClassName("dev.user.shop.libs.org.h2.Driver");
        // 连接测试查询，确保连接可用
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);
    }

    /**
     * JDBC 驱动包装类，用于在 Shadow 打包后正确注册驱动
     */
    private static class DriverShim implements Driver {
        private final Driver driver;

        DriverShim(Driver driver) {
            this.driver = driver;
        }

        @Override
        public java.sql.Connection connect(String url, java.util.Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        @Override
        public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getLogger(DriverShim.class.getName());
        }
    }

    /**
     * 清空商店商品表（用于重置数据库）
     */
    public void clearShopItems() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM shop_items");
        } catch (SQLException e) {
            plugin.getLogger().warning("清空商店商品表失败: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        boolean isMySQL = isMySQL();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 商店商品表
            String shopItemsTable = "CREATE TABLE IF NOT EXISTS shop_items (" +
                    "    id VARCHAR(64) PRIMARY KEY," +
                    "    item_key VARCHAR(128) NOT NULL," +
                    "    buy_price DECIMAL(18,2) DEFAULT 0," +
                    "    sell_price DECIMAL(18,2) DEFAULT 0," +
                    "    stock INT DEFAULT -1," +
                    "    category VARCHAR(32) DEFAULT 'misc'," +
                    "    slot INT DEFAULT 0," +
                    "    enabled BOOLEAN DEFAULT TRUE," +
                    "    daily_limit INT DEFAULT 0" +
                    ")";
            stmt.execute(shopItemsTable);

            // 数据库迁移：添加缺失的 daily_limit 列
            migrateAddDailyLimitColumn(conn);

            // 数据库迁移：添加缺失的 components 列
            migrateAddComponentsColumn(conn);

            // 交易记录表
            String idColumn = isMySQL ? "id BIGINT AUTO_INCREMENT PRIMARY KEY" : "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY";
            String transactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
                    idColumn + "," +
                    "    player_uuid VARCHAR(36) NOT NULL," +
                    "    player_name VARCHAR(32) NOT NULL," +
                    "    item_id VARCHAR(64) NOT NULL," +
                    "    item_key VARCHAR(128) NOT NULL," +
                    "    amount INT NOT NULL," +
                    "    price DECIMAL(18,2) NOT NULL," +
                    "    type VARCHAR(16) NOT NULL," +
                    "    timestamp BIGINT NOT NULL" +
                    ")";
            stmt.execute(transactionsTable);

            // 扭蛋记录表
            String gachaRecordsTable = "CREATE TABLE IF NOT EXISTS gacha_records (" +
                    idColumn + "," +
                    "    player_uuid VARCHAR(36) NOT NULL," +
                    "    player_name VARCHAR(32) NOT NULL," +
                    "    machine_id VARCHAR(32) NOT NULL," +
                    "    reward_id VARCHAR(64) NOT NULL," +
                    "    item_key VARCHAR(128) NOT NULL," +
                    "    amount INT NOT NULL," +
                    "    cost DECIMAL(18,2) NOT NULL," +
                    "    timestamp BIGINT NOT NULL" +
                    ")";
            stmt.execute(gachaRecordsTable);

            // 玩家每日购买限制表（每个物品独立计数）
            String dailyLimitsTable = "CREATE TABLE IF NOT EXISTS daily_limits (" +
                    "    player_uuid VARCHAR(36) NOT NULL," +
                    "    item_id VARCHAR(64) NOT NULL," +
                    "    buy_count INT DEFAULT 0," +
                    "    last_date VARCHAR(10) NOT NULL," +
                    "    PRIMARY KEY (player_uuid, item_id)" +
                    ")";
            stmt.execute(dailyLimitsTable);

            // 扭蛋保底计数表（多段保底，每个规则独立计数，使用rule_hash标识规则）
            String pityCounterTable = "CREATE TABLE IF NOT EXISTS gacha_pity (" +
                    "    player_uuid VARCHAR(36) NOT NULL," +
                    "    machine_id VARCHAR(32) NOT NULL," +
                    "    rule_hash VARCHAR(32) NOT NULL," +
                    "    draw_count INT DEFAULT 0," +
                    "    last_pity_reward VARCHAR(64)," +
                    "    last_draw_time BIGINT DEFAULT 0," +
                    "    PRIMARY KEY (player_uuid, machine_id, rule_hash)" +
                    ")";
            stmt.execute(pityCounterTable);

            // 扭蛋机方块绑定表
            String blockBindingIdColumn = isMySQL ? "id BIGINT AUTO_INCREMENT PRIMARY KEY" : "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY";
            String blockBindingUnique = isMySQL ? "UNIQUE KEY unique_block (world_uuid, block_x, block_y, block_z)" : "UNIQUE (world_uuid, block_x, block_y, block_z)";
            String blockBindingTable = "CREATE TABLE IF NOT EXISTS gacha_block_bindings (" +
                    blockBindingIdColumn + "," +
                    "    world_uuid VARCHAR(36) NOT NULL," +
                    "    block_x INT NOT NULL," +
                    "    block_y INT NOT NULL," +
                    "    block_z INT NOT NULL," +
                    "    machine_id VARCHAR(32) NOT NULL," +
                    "    created_by VARCHAR(36)," +
                    "    created_at BIGINT NOT NULL," +
                    "    display_entity_uuid VARCHAR(36)," +
                    "    outdated BOOLEAN DEFAULT FALSE," +
                    "    " + blockBindingUnique +
                    ")";
            stmt.execute(blockBindingTable);

            // 数据库迁移：添加缺失的 display_entity_uuid 列
            migrateAddDisplayEntityUuidColumn(conn);

            // 数据库迁移：添加缺失的 outdated 列
            migrateAddOutdatedColumn(conn);

            // 创建索引（H2和MySQL的索引语法略有不同）
            createIndexes(stmt, isMySQL);
        }
    }

    /**
     * 创建数据库索引
     */
    private void createIndexes(Statement stmt, boolean isMySQL) throws SQLException {
        if (isMySQL) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player ON transactions (player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON transactions (timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_gacha_player ON gacha_records (player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_gacha_timestamp ON gacha_records (timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pity_player ON gacha_pity (player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_world ON gacha_block_bindings (world_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_machine ON gacha_block_bindings (machine_id)");
        } else {
            // H2语法
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player ON transactions (player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON transactions (timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_gacha_player ON gacha_records (player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_gacha_timestamp ON gacha_records (timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pity_player ON gacha_pity (player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_world ON gacha_block_bindings (world_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_machine ON gacha_block_bindings (machine_id)");
        }
    }

    /**
     * 数据库迁移：添加 daily_limit 列到 shop_items 表
     */
    private void migrateAddDailyLimitColumn(Connection conn) {
        try {
            // 检查列是否存在
            DatabaseMetaData metaData = conn.getMetaData();
            boolean columnExists = false;
            try (ResultSet columns = metaData.getColumns(null, null, "SHOP_ITEMS", "DAILY_LIMIT")) {
                if (columns.next()) {
                    columnExists = true;
                }
            }

            // 如果列不存在，添加它
            if (!columnExists) {
                plugin.getLogger().info("[数据库迁移] 正在添加 daily_limit 列到 shop_items 表...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE shop_items ADD COLUMN daily_limit INT DEFAULT 0");
                    plugin.getLogger().info("[数据库迁移] daily_limit 列添加成功");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[数据库迁移] 添加 daily_limit 列失败: " + e.getMessage());
        }
    }

    /**
     * 数据库迁移：添加 components 列到 shop_items 表
     */
    private void migrateAddComponentsColumn(Connection conn) {
        try {
            // 检查列是否存在
            DatabaseMetaData metaData = conn.getMetaData();
            boolean columnExists = false;
            try (ResultSet columns = metaData.getColumns(null, null, "SHOP_ITEMS", "COMPONENTS")) {
                if (columns.next()) {
                    columnExists = true;
                }
            }

            // 如果列不存在，添加它
            if (!columnExists) {
                plugin.getLogger().info("[数据库迁移] 正在添加 components 列到 shop_items 表...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE shop_items ADD COLUMN components TEXT");
                    plugin.getLogger().info("[数据库迁移] components 列添加成功");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[数据库迁移] 添加 components 列失败: " + e.getMessage());
        }
    }

    /**
     * 数据库迁移：添加 display_entity_uuid 列到 gacha_block_bindings 表
     */
    private void migrateAddDisplayEntityUuidColumn(Connection conn) {
        try {
            // 检查列是否存在
            DatabaseMetaData metaData = conn.getMetaData();
            boolean columnExists = false;
            try (ResultSet columns = metaData.getColumns(null, null, "GACHA_BLOCK_BINDINGS", "DISPLAY_ENTITY_UUID")) {
                if (columns.next()) {
                    columnExists = true;
                }
            }

            // 如果列不存在，添加它
            if (!columnExists) {
                plugin.getLogger().info("[数据库迁移] 正在添加 display_entity_uuid 列到 gacha_block_bindings 表...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE gacha_block_bindings ADD COLUMN display_entity_uuid VARCHAR(36)");
                    plugin.getLogger().info("[数据库迁移] display_entity_uuid 列添加成功");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[数据库迁移] 添加 display_entity_uuid 列失败: " + e.getMessage());
        }
    }

    /**
     * 数据库迁移：添加 outdated 列到 gacha_block_bindings 表
     */
    private void migrateAddOutdatedColumn(Connection conn) {
        try {
            // 检查列是否存在
            DatabaseMetaData metaData = conn.getMetaData();
            boolean columnExists = false;
            try (ResultSet columns = metaData.getColumns(null, null, "GACHA_BLOCK_BINDINGS", "OUTDATED")) {
                if (columns.next()) {
                    columnExists = true;
                }
            }

            // 如果列不存在，添加它
            if (!columnExists) {
                plugin.getLogger().info("[数据库迁移] 正在添加 outdated 列到 gacha_block_bindings 表...");
                try (Statement stmt = conn.createStatement()) {
                    boolean isMySQL = plugin.getDatabaseManager().isMySQL();
                    if (isMySQL) {
                        stmt.execute("ALTER TABLE gacha_block_bindings ADD COLUMN outdated BOOLEAN DEFAULT FALSE");
                    } else {
                        stmt.execute("ALTER TABLE gacha_block_bindings ADD COLUMN outdated BOOLEAN DEFAULT FALSE");
                    }
                    plugin.getLogger().info("[数据库迁移] outdated 列添加成功");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[数据库迁移] 添加 outdated 列失败: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            // 关闭连接池（DB_CLOSE_DELAY=0 确保连接立即释放）
            dataSource.close();

            // H2 数据库在关闭后需要一点时间完全释放文件锁
            // PlugMan 重载时需要确保文件锁被释放
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}

            // 强制触发GC以清理未关闭的资源引用
            System.gc();

            // 再等待一段时间确保文件锁被释放
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {}

            // 尝试注销驱动（解决PlugMan重载时的驱动冲突）
            try {
                // 使用插件ClassLoader查找驱动类并注销
                ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();
                java.util.Enumeration<Driver> drivers = DriverManager.getDrivers();
                while (drivers.hasMoreElements()) {
                    Driver driver = drivers.nextElement();
                    // 检查驱动是否来自此插件的ClassLoader
                    if (driver.getClass().getClassLoader() == pluginClassLoader) {
                        DriverManager.deregisterDriver(driver);
                    }
                }
            } catch (Exception ignored) {
                // 驱动可能不存在或未注册，忽略错误
            }
        }
    }

    public boolean isMySQL() {
        String dbType = plugin.getShopConfig().getDatabaseType().toLowerCase();
        return dbType.equals("mysql") || dbType.equals("mariadb");
    }
}
