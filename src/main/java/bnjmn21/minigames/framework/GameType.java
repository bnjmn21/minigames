package bnjmn21.minigames.framework;

import bnjmn21.minigames.data.MapDataBuilder;
import bnjmn21.minigames.maps.MapManager;
import net.kyori.adventure.text.Component;

public interface GameType {
    MapDataBuilder getMapData();
    Component[] getTeamNames();
    MapManager getMapManager();
    GameInstance start(Settings settings);
}
