package bnjmn21.minigames.data.data_types;

import org.bukkit.Location;
import org.bukkit.World;

public class IVec3 {
    public int x;
    public int y;
    public int z;

    public IVec3(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location center(World world) {
        return new Location(world, (double) x + 0.5, (double) y + 0.5, (double) z + 0.5);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IVec3 iVec3)) return false;
        return x == iVec3.x && y == iVec3.y && z == iVec3.z;
    }

    @Override
    public int hashCode() {
        return x ^ (y << 10) ^ (y >> 22) ^ (z << 20) ^ (z >> 12);
    }

    public static IVec3 of(Location pos) {
        return new IVec3(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
    }
}
