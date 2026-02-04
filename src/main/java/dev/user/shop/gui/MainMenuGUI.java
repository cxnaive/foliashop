package dev.user.shop.gui;

import dev.user.shop.FoliaShopPlugin;
import dev.user.shop.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MainMenuGUI extends AbstractGUI {

    public MainMenuGUI(FoliaShopPlugin plugin, Player player) {
        super(plugin, player, plugin.getShopConfig().getGUITitle("main-menu"), 27);
    }

    @Override
    protected void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        // 商店按钮
        if (plugin.getShopConfig().isShopEnabled()) {
            ItemStack shopBtn = new ItemStack(Material.EMERALD);
            ItemUtil.setDisplayName(shopBtn, "§a§l系统商店");
            ItemUtil.setLore(shopBtn, java.util.Arrays.asList(
                "§7点击打开系统商店",
                "",
                "§e购买和出售各种物品"
            ));
            setItem(11, shopBtn, p -> {
                p.closeInventory();
                new ShopCategoryGUI(plugin, p).open();
            });
        }

        // 扭蛋按钮
        if (plugin.getShopConfig().isGachaEnabled()) {
            ItemStack gachaBtn = new ItemStack(Material.NETHER_STAR);
            ItemUtil.setDisplayName(gachaBtn, "§6§l扭蛋中心");
            ItemUtil.setLore(gachaBtn, java.util.Arrays.asList(
                "§7点击打开扭蛋中心",
                "",
                "§e试试你的手气！"
            ));
            setItem(15, gachaBtn, p -> {
                p.closeInventory();
                new GachaMainGUI(plugin, p).open();
            });
        }

        // 交易记录按钮
        ItemStack historyBtn = new ItemStack(Material.BOOK);
        ItemUtil.setDisplayName(historyBtn, "§b§l交易记录");
        ItemUtil.setLore(historyBtn, java.util.Arrays.asList(
            "§7查看最近的交易记录",
            "",
            "§e点击查询"
        ));
        setItem(13, historyBtn, p -> {
            p.closeInventory();
            new TransactionHistoryGUI(plugin, p, this).open();
        });

        // 关闭按钮
        addCloseButton(22);
    }
}
