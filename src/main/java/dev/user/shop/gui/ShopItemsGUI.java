package dev.user.shop.gui;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.shop.ShopItem;
import dev.user.shop.shop.ShopManager;
import dev.user.shop.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopItemsGUI extends AbstractGUI {

    private final ShopManager.ShopCategory category;
    private final java.util.Map<Integer, ShopItem> slotToItem;

    public ShopItemsGUI(FoliaShopPlugin plugin, Player player, ShopManager.ShopCategory category) {
        super(plugin, player, plugin.getShopConfig().convertMiniMessage(category.getName()), 54);
        this.category = category;
        this.slotToItem = new java.util.HashMap<>();
    }

    @Override
    protected void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        slotToItem.clear();

        List<ShopItem> items = plugin.getShopManager().getItemsByCategory(category.getId());

        for (ShopItem shopItem : items) {
            if (!shopItem.isEnabled()) continue;

            ItemStack item = createItemDisplay(shopItem);
            if (item == null) continue;

            int slot = shopItem.getSlot();
            if (slot > 0 && slot < 45 && slot % 9 != 0 && slot % 9 != 8) {
                setItem(slot, item);  // 不设置action，让GUIListener处理点击
                slotToItem.put(slot, shopItem);
            } else {
                // 自动寻找空位
                for (int i = 10; i < 44; i++) {
                    if (i % 9 == 0 || i % 9 == 8) continue;
                    if (inventory.getItem(i) == null) {
                        setItem(i, item);  // 不设置action，让GUIListener处理点击
                        slotToItem.put(i, shopItem);
                        break;
                    }
                }
            }
        }

        // 返回按钮
        addBackButton(49, () -> new ShopCategoryGUI(plugin, player).open());

        // 关闭按钮
        addCloseButton(52);
    }

    /**
     * 通过slot处理物品点击 - 由GUIListener调用
     */
    public void handleItemClick(Player player, int slot, ClickType clickType) {
        ShopItem shopItem = slotToItem.get(slot);
        if (shopItem == null) return;
        handleItemClick(player, shopItem, clickType);
    }

    /**
     * 处理物品点击 - 由GUIListener调用，保存了点击事件信息
     * 左键购买1个，Shift+左键购买64个，右键和Shift+右键不处理
     */
    public void handleItemClick(Player player, ShopItem shopItem, ClickType clickType) {
        plugin.getLogger().info("[ShopDebug] Click detected: " + clickType + " on item " + shopItem.getItemKey());

        // 右键和Shift+右键不处理
        if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            plugin.getLogger().info("[ShopDebug] Ignoring right click");
            return;
        }

        // 购买
        if (!shopItem.canBuy()) {
            player.sendMessage("§c该物品不可购买！");
            return;
        }
        int amount = (clickType == ClickType.SHIFT_LEFT) ? 64 : 1;
        plugin.getLogger().info("[ShopDebug] Buying amount: " + amount);
        buyItem(player, shopItem, amount);
    }

    /**
     * 兼容旧的处理方法
     */
    private void handleItemClick(Player player, ShopItem shopItem) {
        // 默认购买1个
        buyItem(player, shopItem, 1);
    }

    private void buyItem(Player player, ShopItem shopItem, int amount) {
        // 限制单次最大购买数量
        final int finalAmount = Math.min(amount, 64);

        // 注意：库存检查在数据库层面进行（原子操作），不在内存中预检查
        // 这是为了防止跨服竞态条件

        final double totalCost = shopItem.getBuyPrice() * finalAmount;

        // 检查背包空间
        ItemStack item = shopItem.getDisplayItem().clone();
        item.setAmount(finalAmount);
        final ItemStack finalItem = item;

        int canFit = calculateCanFit(player, item);
        if (canFit < finalAmount) {
            player.sendMessage("§c背包空间不足！最多还能放入 " + canFit + " 个");
            return;
        }

        // 检查每日限额（原子操作：检查+增加计数在同一事务中）
        if (shopItem.hasDailyLimit()) {
            plugin.getShopManager().tryIncrementDailyBuyCount(
                player.getUniqueId(), shopItem.getId(), finalAmount, shopItem.getDailyLimit(),
                success -> {
                    if (success) {
                        // 限额检查通过且已增加计数，继续购买流程
                        processBuyWithEconomy(player, shopItem, finalItem, finalAmount, totalCost);
                    } else {
                        // 超过限额，查询当前已购买数量显示给玩家
                        player.sendMessage("§c该物品今日购买限额已满（限额 " + shopItem.getDailyLimit() + "），明日再来！");
                    }
                }
            );
        } else {
            // 无每日限额，直接进行经济检查
            processBuyWithEconomy(player, shopItem, finalItem, finalAmount, totalCost);
        }
    }

    private void processBuyWithEconomy(Player player, ShopItem shopItem, ItemStack item, int amount, double totalCost) {
        // 异步检查余额并扣款
        plugin.getEconomyManager().hasEnoughAsync(player, totalCost, hasEnough -> {
            if (!hasEnough) {
                player.sendMessage(plugin.getShopConfig().getMessage("insufficient-funds",
                    java.util.Map.of("cost", String.format("%.2f", totalCost),
                                    "currency", plugin.getShopConfig().getCurrencyName())));
                return;
            }

            // 异步扣款
            plugin.getEconomyManager().withdrawAsync(player, totalCost, success -> {
                if (!success) {
                    player.sendMessage(plugin.getShopConfig().getMessage("insufficient-funds",
                        java.util.Map.of("cost", String.format("%.2f", totalCost),
                                        "currency", plugin.getShopConfig().getCurrencyName())));
                    return;
                }

                // 扣款成功，处理库存
                processBuyAfterWithdraw(player, shopItem, item, amount, totalCost);
            });
        });
    }

    /**
     * 扣款成功后处理购买
     */
    private void processBuyAfterWithdraw(Player player, ShopItem shopItem, ItemStack item,
                                         int amount, double totalCost) {
        // 无限库存商品直接完成购买
        if (shopItem.hasUnlimitedStock()) {
            completePurchase(player, shopItem, item, amount, totalCost);
            return;
        }

        // 有限库存商品使用原子扣减防止超卖
        final int finalAmount = amount;
        final double finalCost = totalCost;
        final ItemStack finalItem = item;

        plugin.getShopManager().atomicReduceStock(shopItem.getId(), amount, actualAmount -> {
            if (actualAmount == 0) {
                // 库存扣减失败，退款（已在异步线程，使用同步方法）
                plugin.getEconomyManager().deposit(player, finalCost);
                player.sendMessage(plugin.getShopConfig().getMessage("item-out-of-stock",
                    java.util.Map.of("item", shopItem.getItemKey())));
                return;
            }

            // 如果实际扣减数量与请求不同（理论上不应该），调整物品数量
            if (actualAmount != finalAmount) {
                finalItem.setAmount(actualAmount);
            }

            completePurchase(player, shopItem, finalItem, actualAmount,
                shopItem.getBuyPrice() * actualAmount);
        });
    }

    /**
     * 完成购买流程（给予物品、记录日志等）
     */
    private void completePurchase(Player player, ShopItem shopItem, ItemStack item,
                                  int amount, double totalCost) {
        // 给予物品
        player.getInventory().addItem(item);

        // 记录交易
        plugin.getShopManager().logTransaction(
            player.getUniqueId(), player.getName(),
            shopItem.getId(), shopItem.getItemKey(),
            amount, totalCost, "BUY"
        );

        // 发送消息
        player.sendMessage(plugin.getShopConfig().getMessage("purchase-success",
            java.util.Map.of("item", ItemUtil.getDisplayName(item),
                            "amount", String.valueOf(amount),
                            "cost", String.format("%.2f", totalCost),
                            "currency", plugin.getShopConfig().getCurrencyName())));

        // 更新GUI显示
        refreshItemDisplay(shopItem);
    }

    /**
     * 刷新指定商品的显示（更新库存等信息）
     */
    private void refreshItemDisplay(ShopItem shopItem) {
        // 找到该商品所在的槽位
        for (java.util.Map.Entry<Integer, ShopItem> entry : slotToItem.entrySet()) {
            if (entry.getValue().getId().equals(shopItem.getId())) {
                int slot = entry.getKey();
                // 重新创建物品显示
                ItemStack newDisplay = createItemDisplay(shopItem);
                inventory.setItem(slot, newDisplay);
                break;
            }
        }
    }

    /**
     * 创建商品的显示物品
     */
    private ItemStack createItemDisplay(ShopItem shopItem) {
        ItemStack displayItem = shopItem.getDisplayItem();
        if (displayItem == null) return new ItemStack(Material.BARRIER);

        ItemStack item = displayItem.clone();
        java.util.List<String> shopLore = new java.util.ArrayList<>();

        // 价格信息
        if (shopItem.canBuy()) {
            shopLore.add("§7购买价格: §e" + plugin.getShopConfig().formatCurrency(shopItem.getBuyPrice()));
        }
        // 只在系统回收启用时显示出售价格
        if (shopItem.canSell() && plugin.getShopConfig().isSellSystemEnabled()) {
            shopLore.add("§7回收价格: §e" + plugin.getShopConfig().formatCurrency(shopItem.getSellPrice()));
        }

        // 库存信息
        if (shopItem.hasUnlimitedStock()) {
            shopLore.add("§7库存: §a无限");
        } else {
            shopLore.add("§7库存: §e" + shopItem.getStock());
        }

        shopLore.add("");

        // 操作提示 - 只显示购买操作
        if (shopItem.canBuy()) {
            shopLore.add("§e左键 §7购买 1 个");
            shopLore.add("§eShift+左键 §7购买 64 个");
        }

        // 保留原有lore，在末尾添加商店信息
        addShopLore(item, shopLore);
        return item;
    }

    /**
     * 在物品原有lore基础上添加商店信息
     */
    private void addShopLore(ItemStack item, java.util.List<String> shopLore) {
        ItemUtil.addLore(item, shopLore);
    }

    private void sellItem(Player player, ShopItem shopItem, int amount) {
        // 计算玩家拥有的该物品数量
        int hasAmount = countItems(player, shopItem.getDisplayItem());

        if (hasAmount == 0) {
            player.sendMessage("§c你没有该物品！");
            return;
        }

        // 如果是Shift+右键，出售全部
        if (amount == Integer.MAX_VALUE) {
            amount = hasAmount;
        }

        // 限制出售数量不超过拥有的数量
        amount = Math.min(amount, hasAmount);

        // 移除物品
        int removed = removeItems(player, shopItem.getDisplayItem(), amount);
        if (removed == 0) {
            player.sendMessage("§c物品移除失败！");
            return;
        }

        double totalReward = shopItem.getSellPrice() * removed;
        final int finalRemoved = removed;

        // 异步给予金钱
        plugin.getEconomyManager().depositAsync(player, totalReward, success -> {
            if (!success) {
                // 给予金钱失败，返还物品
                returnItemsToPlayer(player, shopItem.getDisplayItem(), finalRemoved);
                player.sendMessage("§c经济系统错误，出售已取消，物品已返还！");
                return;
            }

            // 增加库存（如果是有限库存且配置允许）
            if (!shopItem.hasUnlimitedStock() && plugin.getShopConfig().isAddStockOnSell()) {
                plugin.getShopManager().atomicAddStock(shopItem.getId(), finalRemoved);
            }

            // 记录交易
            plugin.getShopManager().logTransaction(
                player.getUniqueId(), player.getName(),
                shopItem.getId(), shopItem.getItemKey(),
                finalRemoved, totalReward, "SELL"
            );

            // 发送消息
            player.sendMessage(plugin.getShopConfig().getMessage("sell-success",
                java.util.Map.of("item", ItemUtil.getDisplayName(shopItem.getDisplayItem()),
                                "amount", String.valueOf(finalRemoved),
                                "reward", String.format("%.2f", totalReward),
                                "currency", plugin.getShopConfig().getCurrencyName())));
        });
    }

    /**
     * 返还物品给玩家（背包满了则掉落）
     */
    private void returnItemsToPlayer(Player player, ItemStack template, int amount) {
        ItemStack toReturn = template.clone();
        toReturn.setAmount(amount);

        java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toReturn);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    /**
     * 计算背包可以容纳指定物品的数量
     */
    private int calculateCanFit(Player player, ItemStack item) {
        int maxStackSize = item.getMaxStackSize();
        int amountNeeded = item.getAmount();
        int canFit = 0;

        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (canFit >= amountNeeded) break;

            if (invItem == null || invItem.getType() == Material.AIR) {
                canFit += maxStackSize;
            } else if (invItem.isSimilar(item)) {
                int space = maxStackSize - invItem.getAmount();
                if (space > 0) {
                    canFit += space;
                }
            }
        }

        return Math.min(canFit, amountNeeded);
    }

    /**
     * 统计玩家拥有的指定物品数量
     */
    private int countItems(Player player, ItemStack target) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.isSimilar(target)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 从玩家背包移除指定数量的物品
     * @return 实际移除的数量
     */
    private int removeItems(Player player, ItemStack target, int amountToRemove) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();

        for (int i = 0; i < contents.length && removed < amountToRemove; i++) {
            ItemStack item = contents[i];
            if (item != null && item.isSimilar(target)) {
                int toRemove = Math.min(item.getAmount(), amountToRemove - removed);
                if (toRemove >= item.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - toRemove);
                }
                removed += toRemove;
            }
        }

        return removed;
    }
}
