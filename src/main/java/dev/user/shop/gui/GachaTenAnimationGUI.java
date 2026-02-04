package dev.user.shop.gui;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.gacha.GachaMachine;
import dev.user.shop.gacha.GachaReward;
import dev.user.shop.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GachaTenAnimationGUI extends AbstractGUI {

    private enum AnimationState { PENDING, COMPLETED, CANCELLED }

    // 10连抽显示格子 (中间两行)
    private static final int[] DISPLAY_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};

    private final GachaMachine machine;
    private final List<GachaReward> finalRewards;
    private final List<ItemStack> animationItems;
    private final AtomicInteger animationTick = new AtomicInteger(0);
    private final int animationDuration;
    private final Random random;
    private io.papermc.paper.threadedregions.scheduler.ScheduledTask animationTask;
    private final AtomicReference<AnimationState> state = new AtomicReference<>(AnimationState.PENDING);

    // 动画阶段
    private int currentSpeed = 1;
    private int speedCounter = 0;
    private boolean isSlowingDown = false;
    private int slowdownStartTick = 0;
    private int revealedCount = 0; // 已经揭示的奖品数量

    public GachaTenAnimationGUI(FoliaShopPlugin plugin, Player player, GachaMachine machine, List<GachaReward> finalRewards) {
        super(plugin, player, plugin.getShopConfig().getGUITitle("gacha-ten-animation"), 36);
        this.machine = machine;
        this.finalRewards = finalRewards;
        this.animationDuration = machine.getAnimationDurationTen() * 20; // 使用10连抽独立配置
        this.random = new Random();
        this.animationItems = machine.getAnimationItems();
    }

    @Override
    protected void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        // 标题
        ItemStack titleItem = new ItemStack(Material.NETHER_STAR);
        ItemUtil.setDisplayName(titleItem, "§e§l10连抽抽奖中...");
        ItemUtil.setLore(titleItem, List.of(
            "§7正在抽取10个奖品...",
            "§7请稍候！"
        ));
        setItem(4, titleItem);

        // 初始化显示格子为随机物品
        for (int slot : DISPLAY_SLOTS) {
            inventory.setItem(slot, getRandomAnimationItem());
        }

        startAnimation();
    }

    private ItemStack getRandomAnimationItem() {
        return animationItems.get(random.nextInt(animationItems.size()));
    }

    private void startAnimation() {
        player.sendMessage(plugin.getShopConfig().getMessage("gacha-start"));

        // 播放开始音效
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 2.0f);

        // 计算减速开始时间（总时长的60%开始减速）
        slowdownStartTick = (int) (animationDuration * 0.6);

        animationTask = plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, player.getLocation(), task -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                task.cancel();
                return;
            }

            int tick = animationTick.incrementAndGet();

            // 检查是否到达结束时间
            if (tick >= animationDuration || revealedCount >= 10) {
                task.cancel();
                finishAnimation();
                return;
            }

            // 计算当前速度
            updateSpeed(tick);

            // 根据速度更新动画
            speedCounter++;
            if (speedCounter >= currentSpeed) {
                speedCounter = 0;
                updateRollingAnimation();
            }

        }, 1L, 1L);
    }

    private void updateSpeed(int tick) {
        if (!isSlowingDown && tick >= slowdownStartTick) {
            isSlowingDown = true;
        }

        if (isSlowingDown) {
            int ticksSinceSlowdown = tick - slowdownStartTick;
            // 10连抽减速更快，因为需要揭示10个奖品
            if (ticksSinceSlowdown < 15) {
                currentSpeed = 1;
            } else if (ticksSinceSlowdown < 30) {
                currentSpeed = 2;
            } else if (ticksSinceSlowdown < 50) {
                currentSpeed = 3;
            } else if (ticksSinceSlowdown < 70) {
                currentSpeed = 4;
            } else if (ticksSinceSlowdown < 90) {
                currentSpeed = 5;
            } else {
                currentSpeed = 8;
            }
        } else {
            currentSpeed = 1;
        }
    }

    private void updateRollingAnimation() {
        // 随机更新未揭示的格子
        for (int i = revealedCount; i < 10; i++) {
            int slot = DISPLAY_SLOTS[i];

            // 随着进度逐渐揭示奖品
            int remainingTicks = animationDuration - animationTick.get();
            int expectedReveals = 10 - (remainingTicks / (animationDuration / 10));

            if (i < expectedReveals && i == revealedCount && random.nextInt(3) == 0) {
                // 揭示这个奖品
                revealReward(i);
                revealedCount++;

                // 播放揭示音效
                float pitch = 0.8f + (revealedCount * 0.1f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 0.8f, pitch);
                break;
            } else {
                // 继续滚动显示随机物品
                inventory.setItem(slot, getRandomAnimationItem());
            }
        }

        // 播放滚动音效
        playTickSound();
    }

    private void revealReward(int index) {
        GachaReward reward = finalRewards.get(index);
        ItemStack display = reward.getDisplayItem();
        if (display != null) {
            display = display.clone();
            display.setAmount(reward.getAmount());

            List<String> lore = new ArrayList<>();
            lore.add("§7稀有度: " + reward.getRarityColor() + reward.getRarityName());
            lore.add("§e已揭示!");
            ItemUtil.setLore(display, lore);

            inventory.setItem(DISPLAY_SLOTS[index], display);
        }
    }

    private void playTickSound() {
        float pitch = Math.min(2.0f, 0.8f + (1.0f / currentSpeed));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.MASTER, 0.3f, pitch);
    }

    private void finishAnimation() {
        // 确保所有奖品都已显示（此时 state 仍为 PENDING，关闭界面可退款）
        for (int i = 0; i < 10; i++) {
            revealFinalReward(i);
        }

        // 播放中奖音效
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.8f, 1.0f);

        // 延迟后显示结果界面
        plugin.getServer().getRegionScheduler().runDelayed(plugin, player.getLocation(), t -> {
            // 延迟结束后，尝试将 state 从 PENDING 改为 COMPLETED
            // 如果失败（说明玩家已关闭界面触发过 CANCELLED），就不打开结果界面
            if (!state.compareAndSet(AnimationState.PENDING, AnimationState.COMPLETED)) {
                return;
            }

            // 成功改为 COMPLETED，现在打开结果界面
            if (player.isOnline()) {
                new GachaTenResultGUI(plugin, player, machine, finalRewards).open();
            }
        }, 40L); // 2秒后显示结果
    }

    private void revealFinalReward(int index) {
        GachaReward reward = finalRewards.get(index);
        ItemStack display = reward.getDisplayItem();
        if (display != null) {
            display = display.clone();
            display.setAmount(reward.getAmount());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7稀有度: " + reward.getRarityColor() + reward.getRarityName());
            ItemUtil.setLore(display, lore);

            inventory.setItem(DISPLAY_SLOTS[index], display);
        }
    }

    public void cancelAnimation() {
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        cancelAnimation();

        if (state.compareAndSet(AnimationState.PENDING, AnimationState.CANCELLED)) {
            refundPlayer();
        }
    }

    private void refundPlayer() {
        double totalCost = machine.getCost() * 10;
        // 异步退款
        plugin.getEconomyManager().depositAsync(player, totalCost, success -> {
            player.sendMessage("§e10连抽已取消，已退还 " +
                plugin.getShopConfig().formatCurrency(totalCost));
        });
    }
}
