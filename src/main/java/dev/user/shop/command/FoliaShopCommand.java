package dev.user.shop.command;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.gui.MainMenuGUI;
import dev.user.shop.gui.ShopAdminGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FoliaShopCommand implements CommandExecutor, TabCompleter {

    private final FoliaShopPlugin plugin;

    public FoliaShopCommand(FoliaShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c此命令只能由玩家执行。");
                return true;
            }

            if (!player.hasPermission("foliashop.use")) {
                player.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                return true;
            }

            new MainMenuGUI(plugin, player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                if (!sender.hasPermission("foliashop.admin")) {
                    sender.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(plugin.getShopConfig().getMessage("config-reloaded"));
            }
            case "shop" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c此命令只能由玩家执行。");
                    return true;
                }
                if (!player.hasPermission("foliashop.shop.use")) {
                    player.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                new dev.user.shop.gui.ShopCategoryGUI(plugin, player).open();
            }
            case "gacha" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c此命令只能由玩家执行。");
                    return true;
                }
                if (!player.hasPermission("foliashop.gacha.use")) {
                    player.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                new dev.user.shop.gui.GachaMainGUI(plugin, player).open();
            }
            case "admin" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c此命令只能由玩家执行。");
                    return true;
                }
                if (!player.hasPermission("foliashop.admin")) {
                    player.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                new ShopAdminGUI(plugin, player).open();
            }
            case "reset" -> {
                if (!sender.hasPermission("foliashop.admin")) {
                    sender.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                plugin.getShopManager().reloadFromConfig();
                sender.sendMessage("§a已清空数据库并从配置文件重新加载商店商品！");
            }
            case "clean" -> {
                if (!sender.hasPermission("foliashop.admin")) {
                    sender.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                handleCleanCommand(sender, args);
            }
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage("§c未知命令。使用 /foliashop help 查看帮助。");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("help");
            if (sender.hasPermission("foliashop.use")) {
                completions.add("shop");
                completions.add("gacha");
            }
            if (sender.hasPermission("foliashop.admin")) {
                completions.add("reload");
                completions.add("admin");
                completions.add("reset");
                completions.add("clean");
            }
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
        }

        // clean 命令的参数补全
        if (args.length == 2 && args[0].equalsIgnoreCase("clean")) {
            if (sender.hasPermission("foliashop.admin")) {
                return List.of("5", "10", "30").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .toList();
            }
        }

        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6========== FoliaShop 帮助 ==========");
        sender.sendMessage("§e/foliashop §7- 打开主菜单");
        sender.sendMessage("§e/foliashop shop §7- 打开商店");
        sender.sendMessage("§e/foliashop gacha §7- 打开扭蛋");
        if (sender.hasPermission("foliashop.admin")) {
            sender.sendMessage("§e/foliashop reload §7- 重载配置");
            sender.sendMessage("§e/foliashop admin §7- 打开商店管理界面");
            sender.sendMessage("§e/foliashop reset §7- 清空数据库并从配置重新加载");
            sender.sendMessage("§e/foliashop clean <天数> §7- 清理旧数据 (5/10/30)");
        }
        sender.sendMessage("§e/shop §7- 打开商店");
        sender.sendMessage("§e/gacha §7- 打开扭蛋");
        sender.sendMessage("§6==================================");
    }

    private void handleCleanCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /foliashop clean <天数>");
            sender.sendMessage("§7可用天数: 5, 10, 30 (清理多少天以前的数据)");
            return;
        }

        int days;
        try {
            days = Integer.parseInt(args[1]);
            if (days != 5 && days != 10 && days != 30) {
                sender.sendMessage("§c错误: 天数必须是 5, 10 或 30");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c错误: 天数必须是数字 (5, 10, 30)");
            return;
        }

        sender.sendMessage("§e正在清理 " + days + " 天以前的数据，请稍候...");

        final int finalDays = days;

        // 清理商店相关数据
        plugin.getShopManager().cleanupOldData(days, result -> {
            int deletedTransactions = result[0];
            int deletedDailyLimits = result[1];

            // 清理扭蛋记录
            plugin.getGachaManager().cleanupOldRecords(finalDays, deletedGacha -> {
                sender.sendMessage("§a===== 数据清理完成 =====");
                sender.sendMessage("§7清理范围: §e" + finalDays + " 天以前的数据");
                sender.sendMessage("§7交易记录: §e" + deletedTransactions + " §7条已删除");
                sender.sendMessage("§7过期购买计数: §e" + deletedDailyLimits + " §7条已删除");
                sender.sendMessage("§7抽奖记录: §e" + deletedGacha + " §7条已删除");
                sender.sendMessage("§a========================");
            });
        });
    }
}
