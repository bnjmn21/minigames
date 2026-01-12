package bnjmn21.minigames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import javax.annotation.Nonnull;

public class Lobby implements Listener {
    @Nonnull
    public final World world;
    public final Minigames plugin;

    Lobby(Minigames plugin) {
        this.plugin = plugin;
        World world =  new WorldCreator("lobby").createWorld();
        if (world == null) {
            throw new RuntimeException("Failed to load lobby world!");
        }
        this.world = world;

        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        plugin.getLogger().info("Lobby world loaded.");
    }

    public void teleportToLobby(Player player) {
        plugin.ranks.addToTeam(player);
        player.teleport(world.getSpawnLocation());
        applyLobbyState(player);
    }

    private void applyLobbyState(Player player) {
        Minigames.resetPlayer(player, GameMode.ADVENTURE);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        teleportToLobby(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer().getLocation().getWorld() == world) {
            event.setRespawnLocation(world.getSpawnLocation());
        }
    }

    @EventHandler
    public void onRespawnPost(PlayerRespawnEvent event) {
        if (event.getPlayer().getLocation().getWorld() == world) {
            Player player = event.getPlayer();

            Bukkit.getScheduler().runTask(plugin, () -> applyLobbyState(player));
        }
    }

    public LiteralCommandNode<CommandSourceStack> lobbyCommand(String name) {
        return Commands.literal(name).executes(ctx -> {
            if (ctx.getSource().getSender() instanceof Player player) {
                teleportToLobby(player);
                player.sendMessage(Component.translatable("lobby.tp"));
                return Command.SINGLE_SUCCESS;
            } else {
                ctx.getSource().getSender().sendMessage(Component.translatable("commands.player_only"));
                return 0;
            }
        }).build();
    }
}
