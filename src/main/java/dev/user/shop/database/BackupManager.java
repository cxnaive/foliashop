package dev.user.shop.database;

import dev.user.shop.FoliaShopPlugin;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 数据库备份管理器
 * 支持 H2 和 MySQL 之间的互导
 */
public class BackupManager {

    private final FoliaShopPlugin plugin;
    private final File backupDir;

    // 需要备份的表（按依赖顺序）
    private static final String[] CONFIG_TABLES = {
        "shop_items",
        "gacha_block_bindings"
    };

    private static final String[] STATE_TABLES = {
        "player_item_limits",
        "gacha_pity",
        "daily_limits"
    };

    private static final String[] LOG_TABLES = {
        "transactions",
        "gacha_records"
    };

    public BackupManager(FoliaShopPlugin plugin) {
        this.plugin = plugin;
        this.backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    /**
     * 导出数据到 SQL 文件
     * @param tables 要导出的表（null 表示导出配置+状态表，不包含日志）
     * @param callback 回调函数，参数为导出的文件路径（null 表示失败）
     */
    public void exportToSql(String[] tables, Consumer<File> callback) {
        final String[] exportTables = (tables == null) ? getDefaultTables() : tables;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupFile = new File(backupDir, "backup_" + timestamp + ".sql");

        plugin.getDatabaseQueue().submit("exportBackup", conn -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))) {
                // 写入文件头
                writer.write("-- FoliaShop Database Backup\n");
                writer.write("-- Generated: " + new Date() + "\n");
                writer.write("-- Source Database: " + (plugin.getDatabaseManager().isMySQL() ? "MySQL" : "H2") + "\n");
                writer.write("-- Tables: " + String.join(", ", exportTables) + "\n");
                writer.write("-- ============================================\n\n");

                // 禁用外键检查（如果有的话）
                writer.write("-- Disable foreign key checks\n");
                if (plugin.getDatabaseManager().isMySQL()) {
                    writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
                } else {
                    writer.write("SET REFERENTIAL_INTEGRITY FALSE;\n\n");
                }

                for (String table : exportTables) {
                    exportTable(conn, writer, table);
                }

                // 恢复外键检查
                writer.write("-- Restore foreign key checks\n");
                if (plugin.getDatabaseManager().isMySQL()) {
                    writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
                } else {
                    writer.write("SET REFERENTIAL_INTEGRITY TRUE;\n");
                }

                writer.flush();
                return backupFile;
            } catch (IOException e) {
                throw new SQLException("写入备份文件失败: " + e.getMessage(), e);
            }
        }, callback, error -> {
            plugin.getLogger().warning("导出备份失败: " + error.getMessage());
            callback.accept(null);
        });
    }

    /**
     * 从 SQL 文件导入数据
     * @param backupFile 备份文件
     * @param mode 导入模式：REPLACE(清空后导入) 或 MERGE(保留现有，跳过冲突)
     * @param callback 回调函数，参数为导入的行数（-1 表示失败）
     */
    public void importFromSql(File backupFile, ImportMode mode, Consumer<Integer> callback) {
        if (!backupFile.exists()) {
            plugin.getLogger().warning("备份文件不存在: " + backupFile.getAbsolutePath());
            callback.accept(-1);
            return;
        }

        plugin.getDatabaseQueue().submit("importBackup", conn -> {
            conn.setAutoCommit(false);
            int totalRows = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
                StringBuilder sqlBuilder = new StringBuilder();
                String line;

                // 首先清空表（如果是 REPLACE 模式）
                if (mode == ImportMode.REPLACE) {
                    List<String> tables = extractTablesFromBackup(reader);
                    for (String table : tables) {
                        if (tableExists(conn, table)) {
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute("DELETE FROM " + table);
                                plugin.getLogger().info("已清空表: " + table);
                            }
                        }
                    }
                    reader.close();
                }

                // 重新打开文件读取 SQL 语句
                try (BufferedReader sqlReader = new BufferedReader(new FileReader(backupFile))) {
                    while ((line = sqlReader.readLine()) != null) {
                        line = line.trim();

                        // 跳过注释和空行
                        if (line.isEmpty() || line.startsWith("--")) {
                            continue;
                        }

                        sqlBuilder.append(line).append(" ");

                        // SQL 语句以分号结束
                        if (line.endsWith(";")) {
                            String sql = sqlBuilder.toString().trim();
                            sqlBuilder.setLength(0);

                            // 跳过 SET 语句（数据库特定的设置）
                            if (sql.toUpperCase().startsWith("SET ")) {
                                continue;
                            }

                            // 执行 INSERT 语句
                            if (sql.toUpperCase().startsWith("INSERT INTO ")) {
                                try {
                                    // 转换语法以适配目标数据库
                                    String adaptedSql = adaptSqlForDatabase(sql);
                                    try (Statement stmt = conn.createStatement()) {
                                        stmt.execute(adaptedSql);
                                        totalRows++;
                                    }
                                } catch (SQLException e) {
                                    if (mode == ImportMode.MERGE && isDuplicateError(e)) {
                                        // 重复键错误，在 MERGE 模式下跳过
                                        continue;
                                    }
                                    throw e;
                                }
                            }
                        }
                    }
                }

                conn.commit();
                return totalRows;

            } catch (IOException e) {
                conn.rollback();
                throw new SQLException("读取备份文件失败: " + e.getMessage(), e);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }, callback, error -> {
            plugin.getLogger().warning("导入备份失败: " + error.getMessage());
            callback.accept(-1);
        });
    }

    /**
     * 列出所有备份文件
     */
    public List<File> listBackups() {
        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".sql"));
        if (files == null) return new ArrayList<>();

        List<File> backupFiles = new ArrayList<>();
        for (File file : files) {
            backupFiles.add(file);
        }
        // 按修改时间倒序
        backupFiles.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return backupFiles;
    }

    /**
     * 获取备份目录
     */
    public File getBackupDir() {
        return backupDir;
    }

    /**
     * 获取默认导出的表（配置+状态，不含日志）
     */
    private String[] getDefaultTables() {
        List<String> tables = new ArrayList<>();
        for (String t : CONFIG_TABLES) tables.add(t);
        for (String t : STATE_TABLES) tables.add(t);
        return tables.toArray(new String[0]);
    }

    /**
     * 导出单个表
     */
    private void exportTable(Connection conn, BufferedWriter writer, String table) throws SQLException, IOException {
        writer.write("\n-- ============================================\n");
        writer.write("-- Table: " + table + "\n");
        writer.write("-- ============================================\n");

        if (!tableExists(conn, table)) {
            writer.write("-- Table does not exist\n");
            return;
        }

        // 获取表结构信息
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // 构建列名列表（排除自增 ID 列）
            List<String> columns = new ArrayList<>();
            List<Integer> autoIncrementColumns = new ArrayList<>();

            for (int i = 1; i <= columnCount; i++) {
                String colName = meta.getColumnName(i);
                // 跳过自增 ID 列（通常是第一个 BIGINT 类型的 id 列）
                if (i == 1 && colName.equalsIgnoreCase("id") &&
                    (meta.getColumnTypeName(i).equalsIgnoreCase("BIGINT") ||
                     meta.getColumnTypeName(i).equalsIgnoreCase("NUMBER"))) {
                    autoIncrementColumns.add(i);
                    continue;
                }
                columns.add(colName);
            }

            String columnNames = columns.stream()
                .map(this::quoteIdentifier)
                .collect(Collectors.joining(", "));

            int rowCount = 0;
            while (rs.next()) {
                StringBuilder values = new StringBuilder();
                for (int i = 0; i < columns.size(); i++) {
                    String colName = columns.get(i);
                    int colIndex = rs.findColumn(colName);

                    if (i > 0) values.append(", ");
                    values.append(formatValue(rs, colIndex));
                }

                String insert = String.format("INSERT INTO %s (%s) VALUES (%s);\n",
                    quoteIdentifier(table), columnNames, values);
                writer.write(insert);
                rowCount++;
            }

            writer.write("-- Exported " + rowCount + " rows\n");
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(Connection conn, String table) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, table.toUpperCase(), null)) {
            if (rs.next()) return true;
        }
        // H2 可能是小写
        try (ResultSet rs = conn.getMetaData().getTables(null, null, table.toLowerCase(), null)) {
            return rs.next();
        }
    }

    /**
     * 格式化值为 SQL 值
     */
    private String formatValue(ResultSet rs, int columnIndex) throws SQLException {
        Object value = rs.getObject(columnIndex);
        if (value == null) {
            return "NULL";
        }

        if (value instanceof Number) {
            return value.toString();
        }

        if (value instanceof Boolean) {
            return (Boolean) value ? "TRUE" : "FALSE";
        }

        // 字符串转义
        String str = value.toString();
        str = str.replace("'", "''");  // 单引号转义
        str = str.replace("\\", "\\\\"); // 反斜杠转义
        return "'" + str + "'";
    }

    /**
     * 标识符引用（处理关键字和特殊字符）
     */
    private String quoteIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }

    /**
     * 调整 SQL 语句以适配目标数据库
     */
    private String adaptSqlForDatabase(String sql) {
        boolean isTargetMySQL = plugin.getDatabaseManager().isMySQL();

        // MySQL 使用反引号，H2 使用双引号
        if (isTargetMySQL) {
            // 双引号转反引号
            sql = sql.replace('\"', '`');
        } else {
            // H2 保持双引号，但移除 MySQL 特定的语法
            sql = sql.replace('`', '\"');
        }

        return sql;
    }

    /**
     * 检查是否为重复键错误
     */
    private boolean isDuplicateError(SQLException e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("duplicate") ||
               message.contains("unique constraint") ||
               message.contains("already exists");
    }

    /**
     * 从备份文件中提取表名
     */
    private List<String> extractTablesFromBackup(BufferedReader reader) throws IOException {
        List<String> tables = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("-- Table: ")) {
                String table = line.substring(10).trim();
                tables.add(table);
            }
        }

        return tables;
    }

    public enum ImportMode {
        REPLACE,  // 清空现有数据后导入
        MERGE     // 保留现有数据，跳过冲突
    }
}
