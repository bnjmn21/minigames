package bnjmn21.minigames.framework;

import bnjmn21.minigames.util.LeaveWorldListener;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import javax.annotation.Nullable;

public interface GameInstance {
    World getWorld();
    void stopGame();
    default void onPlayerLeaveWorld(LeaveWorldListener.LeaveWorldEvent event) {}
    default void onBlockPlace(BlockPlaceEvent event) {}
    default void onBlockBreak(BlockBreakEvent event) {}
    default void onPlayerDropItem(PlayerDropItemEvent event) {}
    default void onInventoryClick(InventoryClickEvent event) {}
    default void onPlayerRespawn(PlayerRespawnEvent event) {}
    default void onPlayerPostRespawn(PlayerPostRespawnEvent event) {}
    default void onPlayerMove(PlayerMoveEvent event) {}
    default void onEntityDamage(EntityDamageEvent event) {}
    default void onPlayerItemConsume(PlayerItemConsumeEvent event) {}
    default void tick() {}

    class GameListener implements LeaveWorldListener {
        @Nullable
        public GameInstance game;

        @Override
        public void onLeaveWorld(LeaveWorldEvent event) {
            if (game != null && game.getWorld() == event.world()) {
                game.onPlayerLeaveWorld(event);
            }
        }

        @EventHandler
        public void onBlockPlace(BlockPlaceEvent event) {
            if (game != null && game.getWorld() == event.getBlockPlaced().getWorld()) {
                game.onBlockPlace(event);
            }
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            if (game != null && game.getWorld() == event.getBlock().getWorld()) {
                game.onBlockBreak(event);
            }
        }

        @EventHandler
        public void onPlayerDropItem(PlayerDropItemEvent event) {
            if (game != null && game.getWorld() == event.getPlayer().getWorld()) {
                game.onPlayerDropItem(event);
            }
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (game != null && game.getWorld() == event.getWhoClicked().getWorld()) {
                game.onInventoryClick(event);
            }
        }

        @EventHandler
        public void onPlayerRespawn(PlayerRespawnEvent event) {
            if (game != null && game.getWorld() == event.getPlayer().getWorld()) {
                game.onPlayerRespawn(event);
            }
        }

        @EventHandler
        public void onPlayerPostRespawn(PlayerPostRespawnEvent event) {
            if (game != null && game.getWorld() == event.getPlayer().getWorld()) {
                game.onPlayerPostRespawn(event);
            }
        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent event) {
            if (game != null && game.getWorld() == event.getPlayer().getWorld()) {
                game.onPlayerMove(event);
            }
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent event) {
            if (game != null && game.getWorld() == event.getEntity().getWorld()) {
                game.onEntityDamage(event);
            }
        }

        @EventHandler
        public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
            if (game != null && game.getWorld() == event.getPlayer().getWorld()) {
                game.onPlayerItemConsume(event);
            }
        }

        @EventHandler
        public void onTick(ServerTickEndEvent event) {
            if (game != null) {
                game.tick();
            }
        }
    }
}
