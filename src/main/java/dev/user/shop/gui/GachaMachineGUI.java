package dev.user.shop.gui;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.gacha.GachaMachine;
import dev.user.shop.gacha.GachaReward;
import dev.user.shop.gacha.PityRule;
import dev.user.shop.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        ItemUtil.setDisplayName(icon, plugin.getShopConfig().convertMiniMessage("<yellow><bold>" + machine.getName()));
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
            plugin.getGachaManager().getPityCounters(player.getUniqueId(), machine.getId(), counters -> {
                // 使用保底抽奖
                GachaMachine.PityResult result = machine.rollWithPity(counters);
                GachaReward reward = result.reward();
                PityRule triggeredRule = result.triggeredRule();

                // 更新保底计数
                plugin.getGachaManager().updatePityCounters(
                    player.getUniqueId(),
                    machine.getId(),
                    machine.getPityRules(),
                    triggeredRule != null ? triggeredRule.getRuleHash() : null,
                    reward.getId()
                );

                // 如果触发保底，发送提示
                if (triggeredRule != null) {
                    player.sendMessage("§6§l✦ 触发保底机制！获得稀有奖品！");
                }

                // 打开动画GUI
                new GachaAnimationGUI(plugin, player, machine, reward).open();
            });
        });
    }

    private void startTenGacha(Player player) {
        double totalCost = machine.getCost() * 10;

        executeGachaWithPayment(player, totalCost, () -> {
            // 获取保底计数并进行10连抽（带保底）
            plugin.getGachaManager().getPityCounters(player.getUniqueId(), machine.getId(), counters -> {
                java.util.List<GachaReward> rewards = new java.util.ArrayList<>();
                java.util.List<PityRule> triggeredRules = new java.util.ArrayList<>();
                java.util.List<PityRule> allRules = machine.getPityRules();

                for (int i = 0; i < 10; i++) {
                    // 使用保底抽奖
                    GachaMachine.PityResult result = machine.rollWithPity(counters);
                    GachaReward reward = result.reward();
                    PityRule triggeredRule = result.triggeredRule();

                    // 记录触发的保底规则
                    if (triggeredRule != null) {
                        triggeredRules.add(triggeredRule);
                        // 重置触发的规则计数器
                        counters.put(triggeredRule.getRuleHash(), 0);
                    }

                    // 增加所有规则计数器
                    for (PityRule rule : allRules) {
                        String hash = rule.getRuleHash();
                        counters.put(hash, counters.getOrDefault(hash, 0) + 1);
                    }

                    rewards.add(reward);
                }

                // 批量更新保底计数（直接设置内存中计算的最终值）
                GachaReward lastReward = rewards.get(rewards.size() - 1);
                PityRule lastTriggeredRule = triggeredRules.isEmpty() ? null : triggeredRules.get(triggeredRules.size() - 1);
                plugin.getGachaManager().batchUpdatePityCounters(
                    player.getUniqueId(),
                    machine.getId(),
                    allRules,
                    counters,
                    lastTriggeredRule,
                    lastReward.getId()
                );

                // 如果触发保底，发送提示
                if (!triggeredRules.isEmpty()) {
                    player.sendMessage("§6§l✦ 10连抽触发保底机制！");
                }

                // 打开10连抽动画GUI
                new GachaTenAnimationGUI(plugin, player, machine, rewards).open();
            });
        });
    }

    /**
     * 执行扭蛋的通用支付和验证流程
     * @param player 玩家
     * @param cost 花费
     * @param onSuccess 支付成功后的回调
     */
    private void executeGachaWithPayment(Player player, double cost, Runnable onSuccess) {
        plugin.getEconomyManager().hasEnoughAsync(player, cost, hasEnough -> {
            if (!hasEnough) {
                player.sendMessage(plugin.getShopConfig().getMessage("insufficient-funds",
                    java.util.Map.of("cost", String.format("%.2f", cost),
                                    "currency", plugin.getShopConfig().getCurrencyName())));
                return;
            }

            // 关闭GUI
            player.closeInventory();

            // 检查玩家是否仍然在线
            if (!player.isOnline()) {
                return;
            }

            // 异步扣除金钱
            plugin.getEconomyManager().withdrawAsync(player, cost, success -> {
                if (!success) {
                    player.sendMessage(plugin.getShopConfig().getMessage("insufficient-funds",
                        java.util.Map.of("cost", String.format("%.2f", cost),
                                        "currency", plugin.getShopConfig().getCurrencyName())));
                    return;
                }

                // 检查玩家是否仍然在线（防止扣钱后掉线）
                if (!player.isOnline()) {
                    // 玩家已掉线，退款
                    plugin.getEconomyManager().deposit(player, cost);
                    return;
                }

                // 执行抽奖逻辑
                onSuccess.run();
            });
        });
    }
}
