package bnjmn21.minigames.maps;

import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.data.MapDataBuilder;
import bnjmn21.minigames.data.MapDataField;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Editor {
    public final GameMap.Writeable map;
    public final Sidebar sidebar;
    public final SidebarComponent sidebarComponent;
    public final ComponentSidebarLayout sidebarLayout;
    public final MapDataBuilder mapData;
    public boolean disableUpdates = false;
    private final Minigames plugin;

    public Editor(GameMap.Writeable map, Minigames plugin) {
        this.map = map;
        this.mapData = plugin.getGameType(map.original().game).getMapData();
        this.sidebar = plugin.scoreboardLibrary.createSidebar(15);
        SidebarComponent.Builder sidebarComponent = SidebarComponent.builder();
        sidebarComponent = sidebarComponent.addStaticLine(
                Component.text("Editing: ").color(NamedTextColor.GRAY)
                        .append(this.map.original().displayName.color(NamedTextColor.WHITE))
        ).addBlankLine();
        this.sidebarComponent = sidebarComponent
                .addComponent(new IssuesSidebarComponent(this))
                .build();
        this.sidebarLayout = new ComponentSidebarLayout(
                SidebarComponent.staticLine(
                        Component.text("The Bridge").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).append(
                                Component.text(" (Editor)").color(NamedTextColor.GRAY)
                        )),
                this.sidebarComponent
        );
        sidebarLayout.apply(sidebar);
        this.plugin = plugin;
    }

    public Issues validate() {
        Issues issues = new Issues();
        var pdc = map.world().getPersistentDataContainer();
        for (MapDataField<?> field : mapData.fields) {
            if (!pdc.has(field.key)) {
                issues.addError(IssueCollection.UnsetField.INSTANCE, field.key.getKey());
            }
        }
        mapData.gameRules.validate(map.world(), issues);
        return issues;
    }

    public void onMapChange() {
        if (!disableUpdates) {
            Bukkit.getScheduler().runTask(plugin, () -> sidebarLayout.apply(sidebar));
        }
    }

    public void onJoin(Player player) {
        sidebar.addPlayer(player);
    }

    public void onLeave(Player player) {
        sidebar.removePlayer(player);
    }
}
