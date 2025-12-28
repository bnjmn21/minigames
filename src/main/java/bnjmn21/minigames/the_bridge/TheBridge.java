package bnjmn21.minigames.the_bridge;

import bnjmn21.minigames.Minigames;

public class TheBridge {
    public TheBridgeMap map;

    public TheBridge(Minigames plugin) {
        this.map = new TheBridgeMap(plugin);
    }
}
