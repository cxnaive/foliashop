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

import de.tr7zw.nbtapi.iface.ReadWriteNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                return customItem.buildItemStack(ItemBuildContext.empty(), 1);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CE] 获取CE物品失败 " + namespace + ":" + id + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取物品的显示名称
     * 优先级：1. 自定义名称（如果有） 2. CE物品的translationKey 3. 原版翻译键
     */
    public static String getDisplayName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "<lang:block.minecraft.air>空气</lang>";
        }

        ItemMeta meta = item.getItemMeta();
        // 优先检查是否有CustomName
        if (meta != null && meta.hasCustomName()) {
            Component customName = meta.customName();
            if (customName != null) {
                return LegacyComponentSerializer.legacySection().serialize(customName);
            }
        }
        
        // 优先检查是否有ItemName名称（CE物品通常有）
        if (meta != null && meta.hasItemName()) {
            Component itemName = meta.itemName();
            if (itemName != null) {
                return LegacyComponentSerializer.legacySection().serialize(itemName);
            }
        }

        // 其次检查是否是CE物品，使用CE的translationKey
        String ceTranslationKey = getCEItemTranslationKey(item);
        if (ceTranslationKey != null) {
            return "<lang:" + ceTranslationKey + ">";
        }

        // 最后返回原版 <lang> 标签格式
        String matName = item.getType().name().toLowerCase();
        String translationKey;

        if (item.getType().isBlock()) {
            translationKey = "block.minecraft." + matName;
        } else {
            translationKey = "item.minecraft." + matName;
        }

        return "<lang:" + translationKey + ">";
    }

    /**
     * 获取CE物品的翻译键
     * @param item 物品
     * @return CE物品的translationKey，如果不是CE物品则返回null
     */
    private static String getCEItemTranslationKey(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            // 获取CE物品的自定义ID
            Key customId = CraftEngineItems.getCustomItemId(item);
            if (customId != null) {
                // 将CE物品ID转换为翻译键格式
                // 例如: myplugin:custom_item -> item.myplugin.custom_item
                String namespace = customId.namespace();
                String key = customId.value();
                return "item." + namespace + "." + key.replace("/", ".");
            }
        } catch (Exception e) {
            // CE插件可能未加载，忽略错误
        }
        return null;
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

    /**
     * 创建广播消息，支持可翻译的物品名称
     * 配置格式: "<gold><bold>🎉 恭喜 {player} 从 {machine} 抽中了 {item}！"
     * 其中 {item} 会被特殊处理为 Component.translatable()
     *
     * @param template 消息模板（MiniMessage 格式）
     * @param player 玩家名称
     * @param machine 扭蛋机名称
     * @param itemName 物品名称（包含 <lang:...> 格式）
     * @return 构建好的 Component
     */
    public static net.kyori.adventure.text.Component createBroadcastComponent(
            String template, String player, String machine, String itemName) {
        return createBroadcastComponent(template, player, machine, itemName, -1);
    }

    /**
     * 创建广播消息，支持可翻译的物品名称和抽奖次数
     * 配置格式: "<gold><bold>🎉 恭喜 {player} 从 {machine} 抽中了 {item}！"
     * 其中 {item} 会被特殊处理为 Component.translatable()
     * {draws} 会被替换为距离上次抽到该奖品的次数
     *
     * @param template 消息模板（MiniMessage 格式）
     * @param player 玩家名称
     * @param machine 扭蛋机名称
     * @param itemName 物品名称（包含 <lang:...> 格式）
     * @param drawsSinceLast 距离上次抽到该奖品的次数（-1表示不显示）
     * @return 构建好的 Component
     */
    public static net.kyori.adventure.text.Component createBroadcastComponent(
            String template, String player, String machine, String itemName, int drawsSinceLast) {

        // 提取翻译 key（去掉 <lang: 和 >）
        String translationKey = extractTranslationKey(itemName);

        // 将模板按 {item} 分割
        String[] parts = template.split("\\{item\\}");

        net.kyori.adventure.text.Component result = net.kyori.adventure.text.Component.empty();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // 替换 {player} 和 {machine}
            part = part.replace("{player}", player).replace("{machine}", machine);

            // 替换 {draws} 为抽奖次数（距离上次抽到该奖品的次数+1，表示第几抽才抽到）
            if (drawsSinceLast >= 0) {
                part = part.replace("{draws}", (drawsSinceLast + 1) + "抽");
            }

            // 解析 MiniMessage 格式（<gold><bold> 等）
            result = result.append(parseMiniMessage(part));

            // 在分割点之间插入可翻译的物品名称（除了最后一部分）
            if (i < parts.length - 1 && translationKey != null) {
                result = result.append(net.kyori.adventure.text.Component.translatable(translationKey));
            }
        }

        return result;
    }

    /**
     * 从 <lang:item.minecraft.diamond> 提取 item.minecraft.diamond
     */
    private static String extractTranslationKey(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return "item.minecraft.air";
        }
        if (itemName.startsWith("<lang:") && itemName.endsWith(">")) {
            return itemName.substring(6, itemName.length() - 1);
        }
        return itemName;
    }

    /**
     * 解析 MiniMessage 格式（简单的标签支持）
     */
    private static net.kyori.adventure.text.Component parseMiniMessage(String message) {
        if (message == null || message.isEmpty()) {
            return net.kyori.adventure.text.Component.empty();
        }

        // 使用 MiniMessage 解析器（如果可用）
        try {
            return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message);
        } catch (Exception e) {
            // 如果 MiniMessage 不可用，回退到 LegacyComponentSerializer
            // 将 <color> 标签转换为 § 格式
            String legacyMessage = convertMiniMessageToLegacy(message);
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
        }
    }

    /**
     * 简单的 MiniMessage 到 Legacy 格式转换
     */
    private static String convertMiniMessageToLegacy(String message) {
        if (message == null) return "";

        // 简单的颜色标签转换
        message = message.replace("<gold>", "§6").replace("</gold>", "§r");
        message = message.replace("<red>", "§c").replace("</red>", "§r");
        message = message.replace("<green>", "§a").replace("</green>", "§r");
        message = message.replace("<blue>", "§9").replace("</blue>", "§r");
        message = message.replace("<yellow>", "§e").replace("</yellow>", "§r");
        message = message.replace("<white>", "§f").replace("</white>", "§r");
        message = message.replace("<black>", "§0").replace("</black>", "§r");
        message = message.replace("<gray>", "§7").replace("</gray>", "§r");
        message = message.replace("<dark_gray>", "§8").replace("</dark_gray>", "§r");
        message = message.replace("<aqua>", "§b").replace("</aqua>", "§r");
        message = message.replace("<dark_aqua>", "§3").replace("</dark_aqua>", "§r");
        message = message.replace("<light_purple>", "§d").replace("</light_purple>", "§r");
        message = message.replace("<dark_purple>", "§5").replace("</dark_purple>", "§r");
        message = message.replace("<dark_red>", "§4").replace("</dark_red>", "§r");
        message = message.replace("<dark_green>", "§2").replace("</dark_green>", "§r");
        message = message.replace("<dark_blue>", "§1").replace("</dark_blue>", "§r");

        // 格式标签
        message = message.replace("<bold>", "§l").replace("</bold>", "§r");
        message = message.replace("<italic>", "§o").replace("</italic>", "§r");
        message = message.replace("<underlined>", "§n").replace("</underlined>", "§r");
        message = message.replace("<strikethrough>", "§m").replace("</strikethrough>", "§r");
        message = message.replace("<obfuscated>", "§k").replace("</obfuscated>", "§r");
        message = message.replace("<reset>", "§r");

        return message;
    }

    /**
     * 应用 NBT 组件到物品
     * @param item 目标物品（会被直接修改）
     * @param components NBT 组件配置 (path -> value)
     * @return 应用后的物品（同一个实例）
     */
    public static ItemStack applyComponents(ItemStack item, Map<String, String> components) {
        if (item == null || components == null || components.isEmpty()) {
            return item;
        }

        de.tr7zw.nbtapi.NBT.modifyComponents(item, (ReadWriteNBT nbt) -> {
            for (Map.Entry<String, String> entry : components.entrySet()) {
                String path = entry.getKey();
                String valueStr = entry.getValue();

                try {
                    // 解析值
                    Object value = NBTPathUtils.parseValue(valueStr);

                    // 应用设置
                    applySetNbt(nbt, path, value);
                } catch (Exception e) {
                    // 忽略单个组件的错误，继续处理其他组件
                }
            }
        });
        return item;
    }

    /**
     * 从配置解析组件配置（字符串列表格式）
     * 格式: "path+value" 或 "path+{nbt}"
     * 示例:
     *   - "minecraft:enchantments+{levels:{'minecraft:sharpness':5}}"
     *   - "minecraft:custom_name+'\"传说之剑\"'"
     * @param obj 配置对象（必须是字符串列表）
     * @return 组件映射，如果没有则返回空 map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> parseComponents(Object obj) {
        Map<String, String> components = new java.util.HashMap<>();
        if (obj == null) {
            return components;
        }

        // 只支持字符串列表格式: components: ["path+value", "path+value"]
        if (obj instanceof java.util.List) {
            java.util.List<String> list = (java.util.List<String>) obj;
            for (String entry : list) {
                parseComponentEntry(entry, components);
            }
        }

        return components;
    }

    /**
     * 解析单个组件条目
     * 格式: "path+value"
     */
    private static void parseComponentEntry(String entry, Map<String, String> components) {
        if (entry == null || entry.isEmpty()) {
            return;
        }

        // 找到第一个 + 分隔符
        int firstPlus = entry.indexOf('+');
        if (firstPlus == -1) {
            // 没有 +，视为只有路径，值为空
            return;
        }

        String path = entry.substring(0, firstPlus).trim();
        String value = entry.substring(firstPlus + 1).trim();

        if (!path.isEmpty()) {
            components.put(path, value);
        }
    }

    /**
     * 移除指定路径的 NBT
     */
    public static void applyRemoveNbt(de.tr7zw.nbtapi.iface.ReadWriteNBT nbt, String path) {
        NBTPathUtils.PathNavigationResult result = NBTPathUtils.navigateToParent(nbt, path);
        if (!result.isSuccess()) {
            return;
        }

        de.tr7zw.nbtapi.iface.ReadWriteNBT parent = result.getParent();
        String key = result.getLastKey();
        List<NBTPathUtils.PathSegment> segments = NBTPathUtils.parsePath(path);
        NBTPathUtils.PathSegment lastSegment = segments.get(segments.size() - 1);

        if (lastSegment.hasIndex()) {
            NBTPathUtils.removeListElement(parent, key, lastSegment.getIndex());
        } else if (lastSegment.hasFilter()) {
            Integer index = NBTPathUtils.resolveListFilterIndex(parent, key, lastSegment.getFilter());
            if (index != null) {
                NBTPathUtils.removeListElement(parent, key, index);
            }
        } else {
            parent.removeKey(key);
        }
    }

    /**
     * 设置指定路径的 NBT 值
     */
    public static void applySetNbt(de.tr7zw.nbtapi.iface.ReadWriteNBT nbt, String path, Object value) {
        NBTPathUtils.PathNavigationResult result = NBTPathUtils.navigateToParent(nbt, path);
        if (!result.isSuccess()) {
            return;
        }

        de.tr7zw.nbtapi.iface.ReadWriteNBT parent = result.getParent();
        String key = result.getLastKey();
        List<NBTPathUtils.PathSegment> segments = NBTPathUtils.parsePath(path);
        NBTPathUtils.PathSegment lastSegment = segments.get(segments.size() - 1);

        if (lastSegment.hasIndex()) {
            if (value instanceof de.tr7zw.nbtapi.iface.ReadableNBT) {
                NBTPathUtils.replaceInCompoundList(parent, key, lastSegment.getIndex(), (de.tr7zw.nbtapi.iface.ReadableNBT) value);
            }
        } else if (lastSegment.hasFilter()) {
            Integer index = NBTPathUtils.resolveListFilterIndex(parent, key, lastSegment.getFilter());
            if (index != null && value instanceof de.tr7zw.nbtapi.iface.ReadableNBT) {
                NBTPathUtils.replaceInCompoundList(parent, key, index, (de.tr7zw.nbtapi.iface.ReadableNBT) value);
            }
        } else {
            NBTPathUtils.setTypedValue(parent, key, value);
        }
    }

}
