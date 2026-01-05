package bnjmn21.minigames.the_bridge;

import bnjmn21.minigames.Game;
import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.data.MapDataBuilder;
import bnjmn21.minigames.framework.GameInstance;
import bnjmn21.minigames.framework.GameType;
import bnjmn21.minigames.framework.Settings;
import bnjmn21.minigames.maps.MapManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TheBridge implements GameType {
    public final MapManager mapManager;
    public final TheBridgeMap map;
    public final Component[] teamNames = new Component[] {
            Component.text("Red").color(NamedTextColor.RED),
            Component.text("Blue").color(NamedTextColor.BLUE),
    };
    private final Minigames plugin;

    public TheBridge(Minigames plugin) {
        this.mapManager = new MapManager(
                Game.TheBridge,
                "maps/the_bridge",
                "map_editor/the_bridge",
                plugin
        );
        this.map = new TheBridgeMap(plugin);
        this.plugin = plugin;
    }

    @Override
    public MapDataBuilder getMapData() {
        return map.data;
    }

    @Override
    public Component[] getTeamNames() {
        return teamNames;
    }

    @Override
    public MapManager getMapManager() {
        return mapManager;
    }

    @Override
    public GameInstance start(Settings settings) {
        return new TheBridgeGame(settings, plugin);
    }
}
