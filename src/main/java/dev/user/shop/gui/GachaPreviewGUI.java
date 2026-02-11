package dev.user.shop.gui;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.gacha.GachaMachine;
import dev.user.shop.gacha.GachaReward;
import dev.user.shop.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GachaPreviewGUI extends AbstractGUI {

    private final GachaMachine machine;
    private final List<GachaReward> sortedRewards;
    private final boolean hasAdminPermission;
    private int page = 0;

    public GachaPreviewGUI(FoliaShopPlugin plugin, Player player, GachaMachine machine) {
        super(plugin, player, "§8奖品预览 - " + plugin.getShopConfig().convertMiniMessage(machine.getName()), 54);
        this.machine = machine;
        this.hasAdminPermission = player.hasPermission("foliashop.admin");
        // 按获奖难度从高到低排序（概率从低到高）
        this.sortedRewards = machine.getRewards().stream()
            .sorted(Comparator.comparingDouble(GachaReward::getProbability))
            .collect(Collectors.toList());
    }

    @Override
    protected void initialize() {
        // 确保页码不会小于0
        if (page < 0) page = 0;

        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        // 清除之前的物品（10-43号槽位）
        for (int i = 10; i <= 43; i++) {
            if (i % 9 != 0 && i % 9 != 8) {
                inventory.setItem(i, null);
            }
        }

        List<GachaReward> rewards = sortedRewards;
        int itemsPerPage = 28; // 4行 * 7列
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, rewards.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            GachaReward reward = rewards.get(i);
            ItemStack display = reward.getDisplayItem();
            if (display == null) continue;

            ItemStack item = display.clone();
            item.setAmount(reward.getAmount());

            // 设置总概率以计算实际概率
            reward.setTotalProbability(machine.getTotalProbability());

            List<String> lore = new ArrayList<>();
            lore.add("");

            // 只有admin权限才显示概率和稀有度
            if (hasAdminPermission) {
                lore.add("§7概率: §e" + String.format("%.2f", reward.getActualProbability() * 100) + "%");
                lore.add("§7稀有度: " + reward.getRarityColor() + reward.getRarityPercent());
                if (reward.shouldBroadcast()) {
                    lore.add("§6★ 稀有奖品");
                }
            }

            lore.add("§7数量: §e" + reward.getAmount());

            ItemUtil.addLore(item, lore);

            // 跳过边框位置
            while (slot % 9 == 0 || slot % 9 == 8 || slot < 9 || slot > 44) {
                slot++;
            }

            setItem(slot, item);
            slot++;
        }

        // 上一页按钮
        if (page > 0) {
            ItemStack prevBtn = ItemUtil.createItemFromKey(plugin,
                plugin.getShopConfig().getGUIDecoration("prev-page").getMaterial());
            ItemUtil.setDisplayName(prevBtn, "§e上一页");
            setItem(45, prevBtn, p -> {
                page--;
                initialize();
            });
        }

        // 下一页按钮
        if (endIndex < rewards.size()) {
            ItemStack nextBtn = ItemUtil.createItemFromKey(plugin,
                plugin.getShopConfig().getGUIDecoration("next-page").getMaterial());
            ItemUtil.setDisplayName(nextBtn, "§e下一页");
            setItem(53, nextBtn, p -> {
                page++;
                initialize();
            });
        }

        // 页码显示
        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemUtil.setDisplayName(pageInfo, "§e第 " + (page + 1) + " 页");
        setItem(49, pageInfo);

        // 返回按钮
        addBackButton(48, () -> new GachaMachineGUI(plugin, player, machine).open());
    }
}
