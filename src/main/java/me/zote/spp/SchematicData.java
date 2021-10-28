package me.zote.spp;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SchematicData {

    private final List<Vector> offsets = new ArrayList<>();
    private final List<Vector> air = new ArrayList<>();
    private final Clipboard clip;
    private final Player viewer;
    private Location lastSeen;
    private int rotation;

    public SchematicData(Player player, Clipboard clip) {

        viewer = player;
        this.clip = clip;

        BlockVector3 size = clip.getDimensions();
        BlockVector3 min = clip.getMinimumPoint();

        int sizeX = size.getX();
        int sizeY = size.getY();
        int sizeZ = size.getZ();

        int minX = min.getX();
        int minY = min.getY();
        int minZ = min.getZ();

        for (int x = 0; x < sizeX; x++)
            for (int y = 0; y < sizeY; y++)
                for (int z = 0; z < sizeZ; z++)
                    if (!clip.getBlock(BlockVector3.at(minX + x, minY + y, minZ + z)).toBaseBlock().getBlockType()
                            .getMaterial().isAir())
                        offsets.add(new Vector(x, y, z));
                    else
                        air.add(new Vector(x, y, z));
    }

    public boolean canBePlacedAt(Location loc) {
        boolean air = getAirListAtFor(loc).stream().anyMatch(l -> l.getBlock().getType() != Material.AIR);
        boolean regular = getLocListAtFor(loc).stream().anyMatch(l -> l.getBlock().getType() != Material.AIR);
        return !regular && !air;
    }

    public List<Location> getLocListAtFor(Location loc) {
        return getLocListOfFor(loc, offsets);
    }

    private List<Location> getAirListAtFor(Location loc) {
        return getLocListOfFor(loc, air);
    }

    private List<Location> getLocListOfFor(Location loc, List<Vector> offs) {
        List<Location> locs = new ArrayList<>();
        offs.forEach(v -> {
            Vector offset = v.clone();
            offset.rotateAroundY(Math.toRadians(rotation));
            locs.add(loc.clone().add(offset));
        });
        return locs;
    }

    public void showAt(Location loc) {
        clear();
        List<Location> locs = getLocListAtFor(loc);
        Material color = canBePlacedAt(loc) ? Material.WHITE_STAINED_GLASS
                : Material.RED_STAINED_GLASS;
        locs.forEach(l -> viewer.sendBlockChange(l, color.createBlockData()));
        lastSeen = loc;
    }

    public void clear() {
        if (lastSeen != null)
            removeAt(lastSeen);
    }

    public void removeAt(Location loc) {
        List<Location> locs = getLocListAtFor(loc);
        locs.forEach(l -> viewer.sendBlockChange(l, l.getBlock().getBlockData()));
    }

    public EditSession pasteAt(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        World world = loc.getWorld();
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory()
                .getEditSession(new BukkitWorld(world), -1)) {
            ClipboardHolder holder = new ClipboardHolder(clip);
            AffineTransform transform = new AffineTransform();
            transform = transform.rotateY(-rotation);
            holder.setTransform(holder.getTransform().combine(transform));
            Operation operation = holder.createPaste(editSession).to(BlockVector3.at(x, y, z)).ignoreAirBlocks(false)
                    .build();
            Operations.complete(operation);
            return editSession;
        } catch (WorldEditException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void rotate(int i) {
        rotation += i;
    }

}
