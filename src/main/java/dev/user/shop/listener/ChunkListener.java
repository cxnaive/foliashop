package dev.user.shop.listener;

import dev.user.shop.FoliaShopPlugin;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * 区块加载监听器
 * 用于在区块加载时恢复扭蛋机展示实体
 */
public class ChunkListener implements Listener {

    private final FoliaShopPlugin plugin;

    public ChunkListener(FoliaShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        // 调度到区块位置的区域线程执行
        plugin.getServer().getRegionScheduler().execute(plugin, chunk.getBlock(0, 0, 0).getLocation(), () -> {
            plugin.getGachaDisplayManager().onChunkLoad(
                chunk.getWorld().getUID(),
                chunk.getX(),
                chunk.getZ()
            );
        });
    }
}
