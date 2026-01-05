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
}
