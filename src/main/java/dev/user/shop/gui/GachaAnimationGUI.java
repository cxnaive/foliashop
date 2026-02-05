package dev.user.shop.gui;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.gacha.GachaMachine;
import dev.user.shop.gacha.GachaReward;
import dev.user.shop.gacha.PityRule;
import dev.user.shop.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GachaAnimationGUI extends AbstractGUI {

    private enum AnimationState { PENDING, COMPLETED, CANCELLED }

    // 中间一行显示的槽位 (9-17)，中间是13
    private static final int[] MIDDLE_ROW_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int CENTER_SLOT = 13;
    // 动画显示的可见窗口大小（中间5个格子）
    private static final int VISIBLE_SLOTS = 5;
    private static final int[] VISIBLE_SLOTS_ARRAY = {11, 12, 13, 14, 15};

    private final GachaMachine machine;
    private final GachaReward finalReward;
    private final List<PityRule> satisfiedRules; // 奖品满足的所有保底规则（用于重置计数器）
    private final boolean isPityTriggered; // 是否触发了保底
    private final List<ItemStack> animationItems;
    private final LinkedList<ItemStack> rollingItems; // 滚动物品队列
    private final AtomicInteger animationTick = new AtomicInteger(0);
    private final int animationDuration;
    private final Random random;
    private io.papermc.paper.threadedregions.scheduler.ScheduledTask animationTask;
    private final AtomicReference<AnimationState> state = new AtomicReference<>(AnimationState.PENDING);

    // 动画阶段
    private int currentSpeed = 1; // 当前速度（每N tick移动一次）
    private int speedCounter = 0; // 速度计数器
    private boolean isSlowingDown = false; // 是否正在减速
    private int slowdownStartTick = 0; // 开始减速的tick

    public GachaAnimationGUI(FoliaShopPlugin plugin, Player player, GachaMachine machine, GachaReward finalReward, List<PityRule> satisfiedRules, boolean isPityTriggered) {
        super(plugin, player, plugin.getShopConfig().getGUITitle("gacha-animation"), 27);
        this.machine = machine;
        this.finalReward = finalReward;
        this.satisfiedRules = satisfiedRules;
        this.isPityTriggered = isPityTriggered;
        this.animationDuration = machine.getAnimationDuration() * 20; // 转换为tick
        this.random = new Random();

        // 准备动画物品（使用缓存）
        this.animationItems = machine.getAnimationItems();
        // 初始化滚动队列
        this.rollingItems = new LinkedList<>();
        // 预填充滚动队列
        for (int i = 0; i < VISIBLE_SLOTS + 10; i++) {
            rollingItems.add(getRandomAnimationItem());
        }
    }

    @Override
    protected void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        // 开始动画
        startAnimation();
    }

    private ItemStack getRandomAnimationItem() {
        return animationItems.get(random.nextInt(animationItems.size()));
    }

    private void startAnimation() {
        player.sendMessage(plugin.getShopConfig().getMessage("gacha-start"));

        // 播放开始音效
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 2.0f);

        // 计算减速开始时间（总时长的70%开始减速）
        slowdownStartTick = (int) (animationDuration * 0.7);

        // 使用玩家调度器来运行动画
        animationTask = plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, player.getLocation(), task -> {
            // 检查玩家是否在线和界面是否打开
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                task.cancel();
                return;
            }

            int tick = animationTick.incrementAndGet();

            // 检查是否到达结束时间
            if (tick >= animationDuration) {
                task.cancel();
                // 确保最终奖品在中间
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

        }, 1L, 1L); // 每tick检查一次，但更新频率由 speed 控制
    }

    /**
     * 根据当前tick更新滚动速度
     */
    private void updateSpeed(int tick) {
        if (!isSlowingDown && tick >= slowdownStartTick) {
            isSlowingDown = true;
        }

        if (isSlowingDown) {
            // 减速阶段：速度逐渐变慢 (1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 8 -> 10)
            int ticksSinceSlowdown = tick - slowdownStartTick;
            if (ticksSinceSlowdown < 10) {
                currentSpeed = 1;
            } else if (ticksSinceSlowdown < 20) {
                currentSpeed = 2;
            } else if (ticksSinceSlowdown < 35) {
                currentSpeed = 3;
            } else if (ticksSinceSlowdown < 50) {
                currentSpeed = 4;
            } else if (ticksSinceSlowdown < 70) {
                currentSpeed = 5;
            } else if (ticksSinceSlowdown < 90) {
                currentSpeed = 6;
            } else if (ticksSinceSlowdown < 110) {
                currentSpeed = 8;
            } else {
                currentSpeed = 10;
            }
        } else {
            // 高速滚动阶段
            currentSpeed = 1;
        }
    }

    /**
     * 更新滚动动画
     */
    private void updateRollingAnimation() {
        // 移除队列头部（最左边的物品）
        if (!rollingItems.isEmpty()) {
            rollingItems.pollFirst();
        }

        // 在队列尾部添加新物品（从右边进入）
        // 如果接近结束，确保最终奖品会到达中间
        int remainingTicks = animationDuration - animationTick.get();
        if (remainingTicks <= 15 && remainingTicks > 5) {
            // 接近结束时，开始向队列中添加最终奖品
            rollingItems.addLast(finalReward.getDisplayItem());
        } else {
            rollingItems.addLast(getRandomAnimationItem());
        }

        // 更新可见格子
        updateVisibleSlots();

        // 播放滚动音效
        playTickSound();
    }

    /**
     * 更新可见格子的显示
     */
    private void updateVisibleSlots() {
        // 获取队列中当前可见的物品（从右往左数5个）
        // 队列头部是最左边，尾部是最右边
        // VISIBLE_SLOTS_ARRAY = {11, 12, 13, 14, 15}，11是最左，15是最右（进入方向）

        int queueSize = rollingItems.size();
        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            int slot = VISIBLE_SLOTS_ARRAY[i];
            // 从队列右侧取物品（i=0是11最左，对应队列中较旧的物品）
            int queueIndex = queueSize - VISIBLE_SLOTS + i;
            if (queueIndex >= 0 && queueIndex < queueSize) {
                ItemStack item = rollingItems.get(queueIndex);
                inventory.setItem(slot, item != null ? item.clone() : null);
            } else {
                inventory.setItem(slot, null);
            }
        }
    }

    /**
     * 播放滚动音效
     */
    private void playTickSound() {
        // 根据速度调整音调：越快音调越高
        float pitch = Math.min(2.0f, 0.8f + (1.0f / currentSpeed));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.MASTER, 0.5f, pitch);
    }

    /**
     * 动画结束，显示最终结果
     */
    private void finishAnimation() {
        // 只有处于PENDING状态时才显示结果
        if (!state.compareAndSet(AnimationState.PENDING, AnimationState.COMPLETED)) {
            return;
        }

        // 确保最终奖品在中间(13)
        ItemStack rewardItem = finalReward.getDisplayItem();
        if (rewardItem != null) {
            ItemStack display = rewardItem.clone();
            display.setAmount(finalReward.getAmount());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7稀有度: " + finalReward.getRarityColor() + finalReward.getRarityPercent());
            lore.add("");
            lore.add("§a恭喜获得！");
            ItemUtil.setLore(display, lore);

            inventory.setItem(CENTER_SLOT, display);
        }

        // 清除其他可见格子，突出中间奖品
        for (int slot : VISIBLE_SLOTS_ARRAY) {
            if (slot != CENTER_SLOT) {
                inventory.setItem(slot, null);
            }
        }

        // 播放中奖音效
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
        // 额外的成功音效
        plugin.getServer().getRegionScheduler().runDelayed(plugin, player.getLocation(), t -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 1.0f, 2.0f);
            }
        }, 5L);

        // 动画完成后显示提示
        if (isPityTriggered) {
            player.sendMessage("§6§l✦ 触发保底机制！获得稀有度<" + finalReward.getRarityPercent() + ">奖品！");
        } else if (!satisfiedRules.isEmpty()) {
            player.sendMessage("§6§l✦ 获得稀有度<" + finalReward.getRarityPercent() + ">奖品！");
        }

        // 给予奖品并更新保底计数器（此时才更新，确保已抽不能退）
        giveRewardAndUpdatePity();

        showResult();
    }

    /**
     * 给予奖品并更新保底计数器
     */
    private void giveRewardAndUpdatePity() {
        // 给予奖品
        giveReward();

        // 更新保底计数器（此时才更新，防止提前关闭界面刷保底）
        // 满足的规则（satisfiedRules）计数器重置为0，其他规则+1
        plugin.getGachaManager().updatePityCounters(
            player.getUniqueId(),
            machine.getId(),
            machine.getPityRules(),
            satisfiedRules,
            finalReward.getId()
        );
    }

    /**
     * 取消动画任务
     */
    public void cancelAnimation() {
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }
    }

    private void showResult() {
        // 关闭按钮
        addCloseButton(22);

        // 再抽一次按钮
        ItemStack againBtn = new ItemStack(Material.NETHER_STAR);
        ItemUtil.setDisplayName(againBtn, "§6§l再抽一次");
        ItemUtil.setLore(againBtn, List.of(
            "§7花费 " + plugin.getShopConfig().formatCurrency(machine.getCost()) + " 再次抽奖"
        ));
        setItem(26, againBtn, p -> {
            p.closeInventory();
            new GachaMachineGUI(plugin, p, machine).open();
        });
    }

    private void giveReward() {
        ItemStack rewardItem = finalReward.getDisplayItem();
        if (rewardItem == null) return;

        ItemStack give = rewardItem.clone();
        give.setAmount(finalReward.getAmount());

        // 尝试给予物品
        if (player.getInventory().firstEmpty() == -1) {
            // 背包满了，掉落在地上
            player.getWorld().dropItemNaturally(player.getLocation(), give);
            player.sendMessage("§e背包已满，物品已掉落在地上！");
        } else {
            player.getInventory().addItem(give);
        }

        // 发送消息
        player.sendMessage(plugin.getShopConfig().getMessage("gacha-result",
            java.util.Map.of("item", finalReward.getPlainDisplayName())));

        // 广播稀有奖品
        if (machine.shouldBroadcast(finalReward)) {
            String broadcast = plugin.getShopConfig().getMessage("gacha-broadcast",
                java.util.Map.of("player", player.getName(),
                                "machine", machine.getName(),
                                "item", finalReward.getPlainDisplayName()));
            // 处理颜色代码（§ 格式）
            Component broadcastComponent = ItemUtil.deserializeLegacyMessage(broadcast);
            plugin.getServer().broadcast(broadcastComponent);
        }

        // 记录抽奖
        plugin.getGachaManager().logGacha(
            player.getUniqueId(), player.getName(),
            machine.getId(), finalReward, machine.getCost()
        );
    }

    @Override
    public void onClose() {
        super.onClose();
        // 确保动画停止
        cancelAnimation();

        // 如果动画未完成（PENDING状态），强制完成（给予奖品并更新计数器，不退款）
        // 因为已经扣款，必须完成抽奖，不能退款
        if (state.get() == AnimationState.PENDING) {
            finishAnimation();
        }
    }

}
