package dev.user.shop.command;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.gui.ShopCategoryGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final FoliaShopPlugin plugin;

    public ShopCommand(FoliaShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getShopConfig().getComponent("player-only"));
            return true;
        }

        if (!plugin.getShopConfig().isShopEnabled()) {
            player.sendMessage(plugin.getShopConfig().getComponent("feature-disabled"));
            return true;
        }

        if (!player.hasPermission("foliashop.shop.use")) {
            player.sendMessage(plugin.getShopConfig().getComponent("no-permission"));
            return true;
        }

        if (args.length > 0) {
            String categoryId = args[0].toLowerCase();
            var category = plugin.getShopManager().getCategory(categoryId);
            if (category != null) {
                new dev.user.shop.gui.ShopItemsGUI(plugin, player, category).open();
            } else {
                player.sendMessage(Component.text("分类不存在: " + categoryId).color(NamedTextColor.RED));
            }
        } else {
            new ShopCategoryGUI(plugin, player).open();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 返回所有分类ID
            for (var category : plugin.getShopManager().getAllCategories()) {
                completions.add(category.getId());
            }
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
        }

        return completions;
    }
}
