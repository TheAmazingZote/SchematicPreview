package me.zote.spp.managers;

import me.zote.spp.BuildPreview;
import me.zote.spp.SchematicPreview;
import me.zote.spp.gui.ItemBuilder;
import me.zote.spp.gui.buttons.GUIButton;
import me.zote.spp.gui.config.GUIConfig;
import me.zote.spp.gui.types.PaginatedGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class BuildManager {

    private final Map<UUID, BuildPreview> previews = new HashMap<>();
    private final List<File> schematics = new ArrayList<>();

    public BuildManager() {
        File buildFolder = new File(SchematicPreview.getInstance().getDataFolder(), "schematics");
        if (!buildFolder.exists())
            buildFolder.mkdirs();

        Collections.addAll(schematics, buildFolder.listFiles());
    }

    public BuildPreview get(Player player) {
        return previews.get(player.getUniqueId());
    }

    public void remove(Player player) {
        previews.remove(player.getUniqueId());
    }

    public void openBuildMenu(Player player) {
        GUIConfig cfg = new GUIConfig();
        PaginatedGUI menu = new PaginatedGUI("Schematics", cfg);
        for (File build : schematics) {
            GUIButton butt = ItemBuilder.start(Material.PAPER)
                    .name("Building info:")
                    .lore(lore(build))
                    .buildButton()
                    .setListener(event -> {
                        BuildPreview bp = new BuildPreview(player, build);
                        previews.put(player.getUniqueId(), bp);
                        bp.start();
                        player.getInventory().setItemInMainHand(SchematicPreview.getInstance().getSchemPlacer());
                        player.closeInventory();
                    });
            menu.addButton(butt);
        }
        player.openInventory(menu.getInventory());
    }

    private List<String> lore(File b) {
        List<String> lore = new ArrayList<>();
        lore.add("Name: " + b.getName());
        lore.add(" ");
        lore.add("&eClick to place this structure");
        return lore;
    }

}
