package dev.user.shop.gui;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.gacha.GachaMachine;
import dev.user.shop.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GachaMachineGUI extends AbstractGUI {

    private final GachaMachine machine;

    public GachaMachineGUI(FoliaShopPlugin plugin, Player player, GachaMachine machine) {
        super(plugin, player, plugin.getShopConfig().convertMiniMessage(machine.getName()), 27);
        this.machine = machine;
    }

    @Override
    protected void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        // 扭蛋机图标
        ItemStack icon = machine.createIconItem(plugin);
        ItemUtil.setDisplayName(icon, plugin.getShopConfig().convertMiniMessage(machine.getName()));

        // 添加 description 到 lore
        List<String> iconLore = new ArrayList<>();
        for (String desc : machine.getDescription()) {
            iconLore.add(plugin.getShopConfig().convertMiniMessage(desc));
        }
        ItemUtil.setLore(icon, iconLore);

        setItem(4, icon);

        // 抽奖按钮
        ItemStack rollBtn = new ItemStack(Material.NETHER_STAR);
        ItemUtil.setDisplayName(rollBtn, "§6§l开始抽奖");
        ItemUtil.setLore(rollBtn, List.of(
            "§7每次抽奖花费:",
            "§e" + plugin.getShopConfig().formatCurrency(machine.getCost()),
            "",
            "§e点击开始抽奖！"
        ));
        setItem(13, rollBtn, this::startGacha);

        // 预览奖品按钮
        ItemStack previewBtn = new ItemStack(Material.BOOK);
        ItemUtil.setDisplayName(previewBtn, "§b§l奖品预览");
        ItemUtil.setLore(previewBtn, List.of(
            "§7点击查看所有可能获得的奖品",
            ""
        ));
        setItem(11, previewBtn, p -> {
            p.closeInventory();
            new GachaPreviewGUI(plugin, p, machine).open();
        });

        // 10连抽按钮
        ItemStack tenRollBtn = new ItemStack(Material.DIAMOND);
        ItemUtil.setDisplayName(tenRollBtn, "§b§l10连抽");
        double tenCost = machine.getCost() * 10;
        ItemUtil.setLore(tenRollBtn, List.of(
            "§7连续抽奖10次，获得10个奖品",
            "§7花费:",
            "§e" + plugin.getShopConfig().formatCurrency(tenCost),
            "",
            "§e点击开始10连抽！"
        ));
        setItem(15, tenRollBtn, this::startTenGacha);

        // 历史记录按钮
        ItemStack historyBtn = new ItemStack(Material.CLOCK);
        ItemUtil.setDisplayName(historyBtn, "§7§l抽奖记录");
        ItemUtil.setLore(historyBtn, List.of(
            "§7查看你的抽奖历史（最近20次）",
            ""
        ));
        setItem(26, historyBtn, p -> {
            p.closeInventory();
            new GachaHistoryGUI(plugin, p, this).open();
        });

        // 返回按钮
        addBackButton(22, () -> new GachaMainGUI(plugin, player).open());
    }

    private void startGacha(Player player) {
        double cost = machine.getCost();

        executeGachaWithPayment(player, cost, () -> {
            // 获取保底计数并抽奖
            plugin.getGachaManager().getPityCount(player.getUniqueId(), machine.getId(), pityCount -> {
                if (!player.isOnline()) return;

                // 使用软保底抽奖
                GachaMachine.PityResult result = machine.rollWithPity(pityCount);

                // 立即更新保底计数，确保后续抽奖基于最新状态
                // 注意：即使玩家提前关闭界面，保底计数也不会回滚
                plugin.getGachaManager().updatePityCount(
                    player.getUniqueId(),
                    machine.getId(),
                    machine.isPityTarget(result.reward())
                );

                // 打开动画GUI
                new GachaAnimationGUI(plugin, player, machine, result).open();
            });
        });
    }

    private void startTenGacha(Player player) {
        double totalCost = machine.getCost() * 10;

        executeGachaWithPayment(player, totalCost, () -> {
            // 获取保底计数并进行10连抽（异步查询历史记录）
            plugin.getGachaManager().getPityCount(player.getUniqueId(), machine.getId(), pityCount -> {
                if (!player.isOnline()) return;

                // 执行10连抽（异步查询历史并计算显示次数）
                plugin.getGachaManager().performTenGacha(machine, pityCount, player.getUniqueId(), result -> {
                    if (!player.isOnline()) return;

                    // 打开10连抽动画GUI
                    new GachaTenAnimationGUI(plugin, player, machine, result).open();
                });
            });
        });
    }

    /**
     * 执行扭蛋的通用支付和验证流程
     */
    private void executeGachaWithPayment(Player player, double cost, Runnable onSuccess) {
        plugin.getEconomyManager().hasEnoughAsync(player, cost, hasEnough -> {
            if (!hasEnough) {
                player.sendMessage(plugin.getShopConfig().getMessage("insufficient-funds",
                    java.util.Map.of("cost", String.format("%.2f", cost),
                                    "currency", plugin.getShopConfig().getCurrencyName())));
                return;
            }

            player.closeInventory();
            if (!player.isOnline()) return;

            plugin.getEconomyManager().withdrawAsync(player, cost, success -> {
                if (!success) {
                    player.sendMessage(plugin.getShopConfig().getMessage("insufficient-funds",
                        java.util.Map.of("cost", String.format("%.2f", cost),
                                        "currency", plugin.getShopConfig().getCurrencyName())));
                    return;
                }

                if (!player.isOnline()) {
                    plugin.getEconomyManager().deposit(player, cost);
                    return;
                }

                onSuccess.run();
            });
        });
    }
}
