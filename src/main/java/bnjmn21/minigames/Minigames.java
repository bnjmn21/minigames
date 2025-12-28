package bnjmn21.minigames;

import bnjmn21.minigames.data.MapDataBuilder;
import bnjmn21.minigames.maps.MapManager;
import bnjmn21.minigames.the_bridge.TheBridge;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

public final class Minigames extends JavaPlugin implements Listener {
    public ScoreboardLibrary scoreboardLibrary;
    public Lobby lobby;
    public MapManager mapManager;
    public TheBridge theBridge;

    @Override
    public void onEnable() {
        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException e) {
            throw new RuntimeException(e);
        }

        lobby = new Lobby(this);
        mapManager = new MapManager(this);
        mapManager.loadGameMaps(Game.TheBridge, "the_bridge");
        theBridge = new TheBridge(this);

        getServer().getPluginManager().registerEvents(this, this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::registerCommands);
        getLogger().info("Loaded Minigames");
    }

    public @NotNull MapDataBuilder getGameMapData(@NotNull Game game) {
        if (game == Game.TheBridge) {
            return theBridge.map.data;
        }
        throw new RuntimeException("Unreachable");
    }

    private void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> event) {
        Commands commands = event.registrar();
        commands.register(lobby.lobbyCommand("l"), "Teleport to the lobby");
        commands.register(lobby.lobbyCommand("lobby"), "Teleport to the lobby");
        commands.register(lobby.lobbyCommand("hub"), "Teleport to the lobby");
        commands.register(mapManager.editCommand(), "Edit a map");
        commands.register(theBridge.map.command, "Change settings for a 'The Bridge' map");
    }

    @Override
    public void onDisable() {
        scoreboardLibrary.close();
    }

    /**
     * Resets player health, hunger, exp, effects, etc.
     */
    public static void resetPlayer(Player player, GameMode gameMode) {
        var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }

        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
        player.setLevel(0);
        player.setExp(0f);
        player.setTotalExperience(0);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setFallDistance(0f);

        player.setGameMode(gameMode);
        player.getInventory().clear();
        player.setFlying(false);
    }
}
