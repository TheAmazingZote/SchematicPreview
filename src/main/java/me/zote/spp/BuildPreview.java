package me.zote.spp;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.RayTraceResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class BuildPreview implements Listener {

	private final File schematic;
	private final Player player;
	private SchematicData data;
	private Block lastSeen;

	public BuildPreview(Player player, File schematic) {
		this.player = player;
		this.schematic = schematic;
	}

	public void start() {
		data = new SchematicData(player, getClipboard());
		Bukkit.getServer().getPluginManager().registerEvents(this, SchematicPreview.getInstance());
	}

	public void stop() {
		data.clear();
		player.getInventory().remove(SchematicPreview.getInstance().getSchemPlacer());
		PlayerMoveEvent.getHandlerList().unregister(this);
		SchematicPreview.getInstance().getManager().remove(player);
	}

	public void place() {
		if (lastSeen == null)
			return;
		Location loc = lastSeen.getRelative(BlockFace.UP).getLocation();
		if (!data.canBePlacedAt(loc)) {
			player.sendMessage("The structure cant be placed here");
			return;
		}

		player.sendMessage("The structure was placed");
		data.pasteAt(loc);
		stop();
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();

		if (!player.getUniqueId().equals(this.player.getUniqueId()))
			return;
		
		Bukkit.getServer().getScheduler().runTaskAsynchronously(SchematicPreview.getInstance(), () -> {
			RayTraceResult result = player.rayTraceBlocks(5);
			if (result == null || result.getHitBlock() == null) {
				data.clear();
				return;
			}

			Block target = result.getHitBlock();

			if (target.equals(lastSeen))
				return;

			lastSeen = target;
			data.showAt(target.getRelative(BlockFace.UP).getLocation());
		});
	}

	public SchematicData data() {
		return data;
	}

	public Clipboard getClipboard() {
		ClipboardFormat format = ClipboardFormats.findByFile(schematic);
		try (ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
			return reader.read();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
