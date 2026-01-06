package bnjmn21.minigames;

import bnjmn21.minigames.framework.GameCommand;
import bnjmn21.minigames.framework.GameInstance;
import bnjmn21.minigames.framework.GameType;
import bnjmn21.minigames.framework.Settings;
import bnjmn21.minigames.maps.TempMaps;
import bnjmn21.minigames.the_bridge.TheBridge;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

public final class Minigames extends JavaPlugin implements Listener {
    public ScoreboardLibrary scoreboardLibrary;
    public Lobby lobby;
    public Settings settings;
    public TempMaps tempMaps;
    public TheBridge theBridge;
    GameInstance.GameListener currentGame;

    @Override
    public void onEnable() {
        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException e) {
            throw new RuntimeException(e);
        }

        lobby = new Lobby(this);
        settings = new Settings(Game.TheBridge, this);
        tempMaps = new TempMaps("temp_maps");
        theBridge = new TheBridge(this);
        currentGame = new GameInstance.GameListener();
        getServer().getPluginManager().registerEvents(currentGame, this);

        getServer().getPluginManager().registerEvents(this, this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::registerCommands);

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        board.getTeams().forEach(Team::unregister);
        board.getObjectives().forEach(Objective::unregister);

        getLogger().info("Loaded Minigames");
    }

    public @NotNull GameType getGameType(@NotNull Game game) {
        if (game == Game.TheBridge) {
            return theBridge;
        }
        throw new RuntimeException("Unreachable");
    }

    private void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> event) {
        Commands commands = event.registrar();
        commands.register(lobby.lobbyCommand("l"), "Teleport to the lobby");
        commands.register(lobby.lobbyCommand("lobby"), "Teleport to the lobby");
        commands.register(lobby.lobbyCommand("hub"), "Teleport to the lobby");
        commands.register(settings.settingsCommand("settings"), "Open the minigame settings");
        commands.register(new GameCommand(theBridge).buildCommand("the_bridge"), "'The Bridge' editor commands");
    }

    public void startGame() {
        if (currentGame.game != null) {
            currentGame.game.stopGame();
        }
        currentGame.game = getGameType(settings.game).start(settings);
    }

    public void killCurrentGame() {
        currentGame.game = null;
    }

    /**
     * NMS-dependent bugfix for minecraft-fakeplayer, until
     * <a href="https://github.com/tanyaofei/minecraft-fakeplayer/pull/190">#190</a>
     * gets merged.
     */
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTask(this, () -> {
            ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
            player.hasChangedDimension();
        });
    }

    @Override
    public void onDisable() {
        if (currentGame.game != null) {
            currentGame.game.stopGame();
        }
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
