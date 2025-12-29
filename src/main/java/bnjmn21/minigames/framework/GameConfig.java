package bnjmn21.minigames.framework;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class GameConfig<T> {
    public String map;
    public HashMap<Player, T> teams;
}
