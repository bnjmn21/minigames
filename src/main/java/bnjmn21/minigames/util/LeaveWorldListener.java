package bnjmn21.minigames.util;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public interface LeaveWorldListener extends Listener {
    void onLeaveWorld(LeaveWorldEvent event);

    @EventHandler
    default void onChangedWorld(PlayerChangedWorldEvent event) {
        onLeaveWorld(new LeaveWorldEvent(
                event.getFrom(),
                event.getPlayer(),
                event.getFrom().getPlayers().size()
        ));
    }

    @EventHandler
    default void onQuit(PlayerQuitEvent event) {
        onLeaveWorld(new LeaveWorldEvent(
                event.getPlayer().getWorld(),
                event.getPlayer(),
                event.getPlayer().getWorld().getPlayers().size() - 1
        ));
    }

    record LeaveWorldEvent(World world, Player player, int playersRemaining) {}
}
