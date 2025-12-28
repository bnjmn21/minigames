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
import org.bukkit.entity.Player;

public class Editor {
    public final GameMap.Writeable map;
    public final Sidebar sidebar;
    public final SidebarComponent sidebarComponent;
    public final ComponentSidebarLayout sidebarLayout;
    public final MapDataBuilder mapData;

    public Editor(GameMap.Writeable map, Minigames plugin) {
        this.map = map;
        this.mapData = plugin.getGameMapData(map.original.game);
        this.sidebar = plugin.scoreboardLibrary.createSidebar();
        this.sidebarComponent = SidebarComponent.builder()
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
    }

    public Issues validate() {
        Issues issues = new Issues();
        var pdc = map.world.getPersistentDataContainer();
        for (MapDataField<?> field : mapData.fields) {
            if (!pdc.has(field.key)) {
                issues.addError(IssueCollection.UnsetField.INSTANCE, field.key.getKey());
            }
        }
        return issues;
    }

    public void onMapChange() {
        sidebarLayout.apply(sidebar);
    }

    public void onJoin(Player player) {
        sidebar.addPlayer(player);
    }

    public void onLeave(Player player) {
        sidebar.removePlayer(player);
    }
}
