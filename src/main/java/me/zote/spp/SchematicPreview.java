package me.zote.spp;

import lombok.Getter;
import me.zote.spp.gui.ItemBuilder;
import me.zote.spp.gui.types.PaginatedGUI;
import me.zote.spp.managers.BuildManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SchematicPreview extends JavaPlugin implements Listener {

    @Getter
    private static SchematicPreview instance;

    @Getter
    private ItemStack schemPlacer;

    @Getter
    private BuildManager manager;
    private final String head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjI1YjI3Y2U2MmNhODg3NDM4NDBhOTVkMWMzOTg2OGY0M2NhNjA2OTZhODRmNTY0ZmJkN2RkYTI1OWJlMDBmZSJ9fX0=";

    @Override
    public void onEnable() {

        instance = this;
        manager = new BuildManager();

        PaginatedGUI.prepare(this);

        if (!getDataFolder().exists())
            getDataFolder().mkdirs();

        schemPlacer = ItemBuilder.start(Material.PLAYER_HEAD)
                .texture(head)
                .name("&eBuilder item")
                .lore("&aRight-Click to place the Structure", "&aScroll to rotate", "&aDrop to cancel")
                .build();

        getCommand("build").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (schemPlacer.isSimilar(hand))
            player.getInventory().setItemInMainHand(null);

        manager.get(player).stop();
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        int prev = event.getPreviousSlot();
        int next = event.getNewSlot();

        if (prev == next)
            return;

        Player player = event.getPlayer();

        ItemStack hand = player.getInventory().getItem(prev);

        if (hand == null || !hand.isSimilar(schemPlacer))
            return;

        event.setCancelled(true);
        manager.get(player).data().rotate((prev - next) * 90);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        Player player = event.getPlayer();

        if (dropped == null || !dropped.isSimilar(schemPlacer))
            return;

        event.getItemDrop().remove();
        manager.get(player).stop();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        ItemStack hand = event.getItem();
        Block at = event.getClickedBlock();
        Player player = event.getPlayer();

        if (at == null || hand == null)
            return;

        if (!hand.isSimilar(schemPlacer))
            return;

        event.setCancelled(true);
        BuildPreview data = manager.get(player);

        if (data == null)
            return;

        Action action = event.getAction();

        switch (action) {
            case RIGHT_CLICK_BLOCK -> data.place();
            case LEFT_CLICK_BLOCK -> data.stop();
            default -> {
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player))
            return false;

        BuildPreview data = manager.get(player);

        if (data != null) {
            data.stop();
            return false;
        }

        manager.openBuildMenu(player);
        return false;
    }

}
