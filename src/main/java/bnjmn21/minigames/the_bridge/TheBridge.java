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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class TheBridge implements GameType, Listener {
    public final MapManager mapManager;
    public final TheBridgeMap map;
    public final Component[] teamNames = new Component[] {
            Component.translatable("general.red", NamedTextColor.RED),
            Component.translatable("general.blue", NamedTextColor.BLUE),
    };
    public final Cages cages;
    private final HashMap<UUID, HotbarItem.Editor> hotbarEditors = new HashMap<>();
    private final ArrayList<UUID> hotbarEditorsToRemove = new ArrayList<>();
    private final Minigames plugin;

    public TheBridge(Minigames plugin) {
        this.mapManager = new MapManager(
                Game.TheBridge,
                "maps/the_bridge",
                "map_editor/the_bridge",
                plugin
        );
        this.map = new TheBridgeMap(plugin);
        this.cages = new Cages(plugin);
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

    public void openHotbarEditor(Player player, Runnable onComplete) {
        UUID uuid = player.getUniqueId();
        hotbarEditors.put(uuid, new HotbarItem.Editor(player, plugin, () -> {
            hotbarEditorsToRemove.add(uuid);
            onComplete.run();
        }));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        hotbarEditors.forEach((uuid, editor) -> editor.onInventoryClick(event));
        hotbarEditorsToRemove.forEach(hotbarEditors::remove);
        hotbarEditorsToRemove.clear();
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        hotbarEditors.forEach((uuid, editor) -> editor.onPlayerDropItem(event));
        hotbarEditorsToRemove.forEach(hotbarEditors::remove);
        hotbarEditorsToRemove.clear();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        hotbarEditors.forEach((uuid, editor) -> editor.onInventoryClose(event));
        hotbarEditorsToRemove.forEach(hotbarEditors::remove);
        hotbarEditorsToRemove.clear();
    }
}
