package bnjmn21.minigames.maps;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;

public record BlockSnapshot(int x, int y, int z, Material type, BlockData data) {
    public static List<BlockSnapshot> copyAndClear(
            World world,
            int sx, int sy, int sz,
            int dx, int dy, int dz
    ) {
        final int size = dx * dy * dz;
        List<BlockSnapshot> snapshots = new ArrayList<>(size);
        for (int x = sx; x < sx + dx; x++) {
            for (int y = sy; y < sy + dy; y++) {
                for (int z = sz; z < sz + dz; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type == Material.AIR) continue;
                    snapshots.add(new BlockSnapshot(x, y, z, type, block.getBlockData()));
                    block.setType(Material.AIR, false);
                }
            }
        }

        return snapshots;
    }

    public static void restore(World world, List<BlockSnapshot> snapshots) {
        for (BlockSnapshot snap : snapshots) {
            Block block = world.getBlockAt(snap.x, snap.y, snap.z);
            block.setType(snap.type, false);
            block.setBlockData(snap.data, false);
        }
    }
}
