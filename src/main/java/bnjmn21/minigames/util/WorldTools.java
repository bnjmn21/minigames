package bnjmn21.minigames.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;


public class WorldTools {
    public static void clear(
            World world,
            int sx, int sy, int sz,
            int dx, int dy, int dz
    ) {
        for (int x = sx; x < sx + dx; x++) {
            for (int y = sy; y < sy + dy; y++) {
                for (int z = sz; z < sz + dz; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(Material.AIR, false);
                }
            }
        }

    }
}
