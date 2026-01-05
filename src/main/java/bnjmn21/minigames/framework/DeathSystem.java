package bnjmn21.minigames.framework;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DeathSystem {
    public static final int ASSIST_SECS = 8;
    private final HashMap<UUID, HashMap<UUID, Instant>> assists = new HashMap<>();

    public void hit(Player hit, Player source) {
        assists.computeIfAbsent(hit.getUniqueId(), ignored -> new HashMap<>()).put(source.getUniqueId(), Instant.now());
    }

    public void reset(Player player) {
        assists.remove(player.getUniqueId());
    }

    public List<Player> getAssists(Player player) {
        Instant threshold = Instant.now().minusSeconds(ASSIST_SECS);
        List<Player> playerAssists = assists.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>()).entrySet().stream()
                .filter(entry -> entry.getValue().isAfter(threshold))
                .map(entry -> Bukkit.getPlayer(entry.getKey()))
                .toList();
        reset(player);
        return playerAssists;
    }
}
