package dev.user.shop.util;

import dev.user.shop.FoliaShopPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemUtil {

    /**
     * 从物品键创建物品（支持原版物品和CE物品）
     */
    public static ItemStack createItemFromKey(FoliaShopPlugin plugin, String key) {
        if (key == null || key.isEmpty()) {
            return new ItemStack(Material.STONE);
        }

        // 解析命名空间和ID
        String namespace = "minecraft";
        String id = key;

        if (key.contains(":")) {
            String[] parts = key.split(":", 2);
            namespace = parts[0];
            id = parts[1];
        }

        // 如果不是原版物品，尝试获取CE物品
        if (!namespace.equals("minecraft")) {
            ItemStack ceItem = getCEItem(plugin, namespace, id);
            if (ceItem != null) {
                return ceItem;
            }
        }

        // 尝试创建原版物品
        try {
            Material material = Material.valueOf(id.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("未知的物品: " + key);
            return new ItemStack(Material.BARRIER);
        }
    }

    /**
     * 获取CE物品
     */
    private static ItemStack getCEItem(FoliaShopPlugin plugin, String namespace, String id) {
        try {
            Key itemKey = Key.of(namespace, id);
            CustomItem<ItemStack> customItem = CraftEngineItems.byId(itemKey);
            if (customItem != null) {
                plugin.getLogger().info("[CE] 找到CE物品: " + namespace + ":" + id);
                ItemStack item = customItem.buildItemStack(ItemBuildContext.empty(), 1);
                plugin.getLogger().info("[CE] 成功构建CE物品: " + namespace + ":" + id + " -> " + item.getType());
                return item;
            } else {
                plugin.getLogger().warning("[CE] 未找到CE物品: " + namespace + ":" + id);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CE] 获取CE物品失败 " + namespace + ":" + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取物品的显示名称
     */
    public static String getDisplayName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "空气";
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component displayName = meta.displayName();
            if (displayName != null) {
                return LegacyComponentSerializer.legacySection().serialize(displayName);
            }
        }

        // 返回格式化的物品ID
        String matName = item.getType().name().toLowerCase().replace("_", " ");
        String[] words = matName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * 设置物品的显示名称（支持颜色代码 §）
     */
    public static void setDisplayName(ItemStack item, String name) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Component component = LegacyComponentSerializer.legacySection().deserialize(name)
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(component);
            item.setItemMeta(meta);
        }
    }

    /**
     * 设置物品的Lore（支持颜色代码 §），保留原有ItemMeta
     */
    public static void setLore(ItemStack item, List<String> lore) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            // 如果没有ItemMeta，尝试创建
            meta = org.bukkit.Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) return;
        }

        List<Component> components = lore.stream()
                .map(line -> LegacyComponentSerializer.legacySection().deserialize(line)
                        .decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
        meta.lore(components);
        item.setItemMeta(meta);
    }

    /**
     * 在原有Lore基础上添加新的Lore行（用于CE物品）
     */
    public static void addLore(ItemStack item, List<String> additionalLore) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 获取原有lore
        List<Component> existingLore = meta.lore();
        if (existingLore == null) {
            existingLore = new ArrayList<>();
        }

        // 添加新lore
        for (String line : additionalLore) {
            existingLore.add(LegacyComponentSerializer.legacySection().deserialize(line)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(existingLore);
        item.setItemMeta(meta);
    }

    /**
     * 创建一个简单的GUI装饰物品
     */
    public static ItemStack createDecoration(Material material, String name) {
        ItemStack item = new ItemStack(material);
        setDisplayName(item, name);
        return item;
    }

    /**
     * 克隆物品并设置数量
     */
    public static ItemStack cloneWithAmount(ItemStack item, int amount) {
        if (item == null) return null;
        ItemStack clone = item.clone();
        clone.setAmount(Math.max(1, Math.min(amount, item.getMaxStackSize())));
        return clone;
    }

    /**
     * 获取物品的Key (格式: "minecraft:stone" 或 "namespace:id")
     * 支持原版物品和 CraftEngine 自定义物品
     */
    public static String getItemKey(ItemStack item) {
        if (item == null) return "minecraft:air";

        // 尝试从 CraftEngine 获取自定义物品ID
        String ceKey = getCEItemKey(item);
        if (ceKey != null) {
            return ceKey;
        }

        // 默认返回原版物品ID
        return "minecraft:" + item.getType().name().toLowerCase();
    }

    /**
     * 尝试获取 CraftEngine 物品的Key
     * @param item 物品
     * @return CE物品key，如果不是CE物品则返回null
     */
    public static String getCEItemKey(ItemStack item) {
        Key customId = CraftEngineItems.getCustomItemId(item);
        if (customId != null) {
            return customId.toString();
        }
        return null;
    }

    /**
     * 将 § 格式颜色代码转换为 Adventure Component
     * @param message 包含 § 颜色代码的消息
     * @return Adventure Component 对象
     */
    public static net.kyori.adventure.text.Component deserializeLegacyMessage(String message) {
        if (message == null || message.isEmpty()) {
            return net.kyori.adventure.text.Component.empty();
        }
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(message);
    }
}
