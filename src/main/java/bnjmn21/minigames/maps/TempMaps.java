package bnjmn21.minigames.maps;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.function.Consumer;

import static bnjmn21.minigames.util.Paths.path;

public class TempMaps {
    private final String location;
    private int currentTempWorld = 0;

    public TempMaps(String location) {
        this.location = location;
        GameMap.deleteDirectory(path(location));
        if (!path(location).toFile().mkdir()) {
            throw new RuntimeException("Failed to create temporary maps directory");
        }
    }

    /**
     * Creates a readonly copy of the map.
     * You must call {@link TempMaps#destroy} once it's no longer used.
     */
    public void create(GameMap map, Consumer<World> onCompleted) {
        map.createReadonlyCopy(location + "/temp" + currentTempWorld++, onCompleted);
    }

    public void destroy(World world) {
        String name = world.getName();
        Bukkit.unloadWorld(world, false);
        GameMap.deleteDirectory(path(name));
    }
}
