package dev.user.shop.command;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.gacha.GachaBlockBinding;
import dev.user.shop.gui.MainMenuGUI;
import dev.user.shop.gui.ShopAdminGUI;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

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
                if (!plugin.getShopConfig().isShopEnabled()) {
                    player.sendMessage(plugin.getShopConfig().getMessage("feature-disabled"));
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
                if (!plugin.getShopConfig().isGachaEnabled()) {
                    player.sendMessage(plugin.getShopConfig().getMessage("feature-disabled"));
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
            case "bindblock" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c此命令只能由玩家执行。");
                    return true;
                }
                if (!player.hasPermission("foliashop.admin")) {
                    player.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                handleBindBlockCommand(player, args);
            }
            case "unbindblock" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c此命令只能由玩家执行。");
                    return true;
                }
                if (!player.hasPermission("foliashop.admin")) {
                    player.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                handleUnbindBlockCommand(player);
            }
            case "listblocks" -> {
                if (!sender.hasPermission("foliashop.admin")) {
                    sender.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                handleListBlocksCommand(sender, args);
            }
            case "exportshop" -> {
                if (!sender.hasPermission("foliashop.admin")) {
                    sender.sendMessage(plugin.getShopConfig().getMessage("no-permission"));
                    return true;
                }
                handleExportShopCommand(sender);
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
                completions.add("bindblock");
                completions.add("unbindblock");
                completions.add("listblocks");
                completions.add("exportshop");
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

        // bindblock 命令的参数补全（扭蛋机ID）
        if (args.length == 2 && args[0].equalsIgnoreCase("bindblock")) {
            if (sender.hasPermission("foliashop.admin")) {
                return plugin.getGachaManager().getAllMachines().stream()
                    .map(machine -> machine.getId())
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
        }

        // listblocks 命令的参数补全（扭蛋机ID，可选）
        if (args.length == 2 && args[0].equalsIgnoreCase("listblocks")) {
            if (sender.hasPermission("foliashop.admin")) {
                return plugin.getGachaManager().getAllMachines().stream()
                    .map(machine -> machine.getId())
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
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
        if (sender.hasPermission("foliashop.admin")) {
            sender.sendMessage("§e/foliashop bindblock <machineId> §7- 绑定看向的方块到扭蛋机");
            sender.sendMessage("§e/foliashop unbindblock §7- 解绑看向的方块");
            sender.sendMessage("§e/foliashop listblocks [machineId] §7- 列出方块绑定");
            sender.sendMessage("§e/foliashop exportshop §7- 导出商店数据到 backup_shop.yml");
        }
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

    private void handleBindBlockCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /foliashop bindblock <machineId>");
            player.sendMessage("§7可用扭蛋机: §e" + String.join(", ",
                plugin.getGachaManager().getAllMachines().stream()
                    .map(m -> m.getId())
                    .toList()));
            return;
        }

        String machineId = args[1].toLowerCase();

        // 检查扭蛋机是否存在
        if (plugin.getGachaManager().getMachine(machineId) == null) {
            player.sendMessage("§c错误：扭蛋机 '" + machineId + "' 不存在！");
            return;
        }

        // 获取玩家看向的方块（10格内）
        Block targetBlock = getTargetBlock(player, 10);
        if (targetBlock == null) {
            player.sendMessage("§c请看向10格内的方块！");
            return;
        }

        // 检查是否已绑定
        if (plugin.getGachaBlockManager().isBlockBound(targetBlock)) {
            String existingMachine = plugin.getGachaBlockManager().getMachineByBlock(targetBlock);
            player.sendMessage("§c该方块已绑定到扭蛋机 '" + existingMachine + "'，请先解绑！");
            return;
        }

        // 执行绑定
        player.sendMessage("§e正在绑定...");
        plugin.getGachaBlockManager().bindBlock(targetBlock, machineId, player, result -> {
            if (result.success()) {
                player.sendMessage("§a✔ 成功将方块绑定到扭蛋机 '" + machineId + "'！");
                player.sendMessage("§7左键点击方块：预览奖品");
                player.sendMessage("§7右键点击方块：打开抽奖界面");
            } else {
                player.sendMessage("§c✘ 绑定失败: " + result.message());
            }
        });
    }

    private void handleUnbindBlockCommand(Player player) {
        // 获取玩家看向的方块（10格内）
        Block targetBlock = getTargetBlock(player, 10);
        if (targetBlock == null) {
            player.sendMessage("§c请看向10格内的方块！");
            return;
        }

        // 检查是否已绑定
        String existingMachine = plugin.getGachaBlockManager().getMachineByBlock(targetBlock);
        if (existingMachine == null) {
            player.sendMessage("§c该方块未绑定任何扭蛋机！");
            return;
        }

        // 执行解绑
        player.sendMessage("§e正在解绑...");
        plugin.getGachaBlockManager().unbindBlock(targetBlock, result -> {
            if (result.success()) {
                player.sendMessage("§a✔ 成功解绑方块！原绑定扭蛋机: '" + result.message() + "'");
            } else {
                player.sendMessage("§c✘ 解绑失败: " + result.message());
            }
        });
    }

    private void handleListBlocksCommand(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String machineId = args[1].toLowerCase();
            if (plugin.getGachaManager().getMachine(machineId) == null) {
                sender.sendMessage("§c错误：扭蛋机 '" + machineId + "' 不存在！");
                return;
            }
            String title = "扭蛋机 '" + machineId + "' 的方块绑定列表";
            plugin.getGachaBlockManager().getBindingsByMachine(machineId, bindings -> {
                sendBindingsList(sender, title, bindings);
            });
        } else {
            String title = "所有扭蛋机方块绑定列表";
            plugin.getGachaBlockManager().getAllBindings(bindings -> {
                sendBindingsList(sender, title, bindings);
            });
        }
    }

    private void sendBindingsList(CommandSender sender, String title, List<GachaBlockBinding> bindings) {
        sender.sendMessage("§6========== " + title + " ==========");
        sender.sendMessage("§7共 " + bindings.size() + " 个绑定");

        if (bindings.isEmpty()) {
            sender.sendMessage("§7暂无绑定");
        } else {
            int index = 1;
            for (GachaBlockBinding binding : bindings) {
                String worldName = plugin.getGachaBlockManager().getWorldName(binding.getWorldUuid());
                sender.sendMessage("§e" + index + ". §7扭蛋机: §f" + binding.getMachineId() +
                    " §7世界: §f" + worldName +
                    " §7坐标: §f" + binding.getPosition().getBlockX() + "," +
                    binding.getPosition().getBlockY() + "," +
                    binding.getPosition().getBlockZ());
                index++;
                if (index > 20) {
                    sender.sendMessage("§7... 还有 " + (bindings.size() - 20) + " 个绑定未显示");
                    break;
                }
            }
        }
        sender.sendMessage("§6==================================");
    }

    private void handleExportShopCommand(CommandSender sender) {
        sender.sendMessage("§e正在导出商店数据到 backup_shop.yml，请稍候...");

        plugin.getShopManager().exportToYaml(count -> {
            if (count > 0) {
                sender.sendMessage("§a✔ 成功导出 " + count + " 个商品到 backup_shop.yml");
                sender.sendMessage("§7文件位置: §e" + plugin.getDataFolder().getAbsolutePath() + "/backup_shop.yml");
            } else {
                sender.sendMessage("§c✘ 导出失败，请查看控制台日志");
            }
        });
    }

    /**
     * 获取玩家看向的方块
     * @param player 玩家
     * @param maxDistance 最大距离
     * @return 目标方块，未找到返回 null
     */
    private Block getTargetBlock(Player player, int maxDistance) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            maxDistance,
            FluidCollisionMode.NEVER,
            true
        );
        return result != null ? result.getHitBlock() : null;
    }
}
