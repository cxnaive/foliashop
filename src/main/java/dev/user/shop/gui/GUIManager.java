package dev.user.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager {

    private static final Map<UUID, AbstractGUI> openGUIs = new HashMap<>();

    public static void registerGUI(UUID playerUuid, AbstractGUI gui) {
        openGUIs.put(playerUuid, gui);
    }

    public static void unregisterGUI(UUID playerUuid) {
        openGUIs.remove(playerUuid);
    }

    public static AbstractGUI getOpenGUI(UUID playerUuid) {
        return openGUIs.get(playerUuid);
    }

    public static boolean hasOpenGUI(UUID playerUuid) {
        return openGUIs.containsKey(playerUuid);
    }

    /**
     * 关闭所有打开的GUI（插件禁用时调用）
     */
    public static void closeAllGUIs() {
        for (Map.Entry<UUID, AbstractGUI> entry : openGUIs.entrySet()) {
            UUID playerUuid = entry.getKey();
            AbstractGUI gui = entry.getValue();

            // 如果是扭蛋动画GUI，取消动画任务
            if (gui instanceof GachaAnimationGUI animationGUI) {
                animationGUI.cancelAnimation();
            }

            // 关闭玩家背包
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        openGUIs.clear();
    }
}
