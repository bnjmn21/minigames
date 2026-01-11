package bnjmn21.minigames.framework;

import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.framework.ui.Ui;
import bnjmn21.minigames.the_bridge.Cages;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.stream.Stream;

public class PlayerSettings {
    Player player;
    Minigames plugin;

    public PlayerSettings(Player player, Minigames plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public Dialog playerSettings() {
        var theBridgeEditHotbar = Ui.button("Edit The Bridge Hotbar")
                .onClick(() -> plugin.theBridge.openHotbarEditor(player, () -> player.showDialog(playerSettings())))
                .build();
        var theBridgeChangeCage = Ui.<String>dropdown("The Bridge Cage")
                .values(plugin.theBridge.cages.cages.keySet().stream())
                .makeName(id -> Component.text(plugin.theBridge.cages.cages.get(id)))
                .getter(() -> plugin.playerData.get(player.getUniqueId(), Cages.cageField))
                .setter(res -> plugin.playerData.set(player.getUniqueId(), Cages.cageField, res))
                .previous(this::playerSettings)
                .build();

        return Ui.multiAction(
                Component.text("Player Settings"),
                Stream.of(theBridgeEditHotbar, theBridgeChangeCage.button),
                1,
                null
        );
    }

    public static LiteralCommandNode<CommandSourceStack> playerSettingsCommand(String name, Minigames plugin) {
        return Commands.literal(name)
                .requires(ctx -> ctx.getSender() instanceof Player)
                .executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player p)) {
                        throw new RuntimeException("unreachable");
                    }
                    p.showDialog(new PlayerSettings(p, plugin).playerSettings());
                    return 1;
                }).build();
    }
}
