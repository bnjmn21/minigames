package bnjmn21.minigames.data;

import bnjmn21.minigames.Game;
import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.maps.Editor;
import bnjmn21.minigames.maps.GameRuleSet;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class MapDataBuilder {
    public final ArrayList<MapDataField<?>> fields = new ArrayList<>();
    public final GameRuleSet gameRules = new GameRuleSet();
    private final Game game;
    private final Minigames plugin;
    @Nullable
    public String commandName;

    public MapDataBuilder(Game game, Minigames plugin) {
        this.game = game;
        this.plugin = plugin;
    }

    public <T> MapDataField<T> add(String name, MapDataType<T> dataType) {
        MapDataField<T> field = new MapDataField<>(name, dataType, plugin);
        fields.add(field);
        return field;
    }

    public MapDataBuilder addGameRule(GameRule<Boolean> gameRule, boolean value) {
        gameRules.with(gameRule, value);
        return this;
    }

    public MapDataBuilder addGameRule(GameRule<Integer> gameRule, int value) {
        gameRules.with(gameRule, value);
        return this;
    }

    public LiteralCommandNode<CommandSourceStack> buildCommand(String commandName, Component help) {
        this.commandName = commandName;

        var command = Commands
            .literal(commandName)
            .requires(ctx -> ctx.getSender().hasPermission("minigames.edit"))
            .requires(ctx -> plugin.mapManager.isEditorOfGame(ctx.getLocation().getWorld(), game));

        var setterCommand = Commands.literal("set");
        var unsetCommand = Commands.literal("unset");
        var getterCommand = Commands.literal("get");

        for (MapDataField<?> field : fields) {
            setterCommand = addFieldToSetter(setterCommand, field);
            unsetCommand = unsetCommand.then(Commands.literal(field.key.getKey()).executes(ctx -> {
                World world = ctx.getSource().getLocation().getWorld();
                var pdc = world.getPersistentDataContainer();
                if (pdc.has(field.key)) {
                    pdc.remove(field.key);
                    @Nullable Editor editor = plugin.mapManager.getEditor(world);
                    if (editor != null) {
                        editor.onMapChange();
                    }
                }
                return 1;
            }));
            getterCommand = getterCommand.then(Commands.literal(field.key.getKey()).executes(ctx -> {
                var pdc = ctx.getSource().getLocation().getWorld().getPersistentDataContainer();
                if (pdc.has(field.key)) {
                    ctx.getSource().getSender().sendMessage(
                            Component.text(field.key.getKey() + " is ").append(getAndView(pdc, field))
                    );
                } else {
                    ctx.getSource().getSender().sendMessage(Component.text(field.key.getKey() + " is unset"));
                }
                return 1;
            }));
        }

        return command
                .then(setterCommand)
                .then(unsetCommand)
                .then(getterCommand)
                .then(Commands.literal("help").executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(help);
                    return 1;
                }))
                .then(Commands.literal("set_default_game_rules").executes(ctx -> {
                    @Nullable Editor editor = plugin.mapManager.getEditor(ctx.getSource().getLocation().getWorld());
                    if (editor != null) {
                        editor.disableUpdates = true; // prevent update spam due to mass-setting gamerules
                        gameRules.applyAndResetOthers(ctx.getSource().getLocation().getWorld());
                        ctx.getSource().getSender().sendMessage(Component.text("All gamerules set."));
                        editor.disableUpdates = false;
                        editor.onMapChange();
                    }
                    return 1;
                }))
                .build();
    }

    private <T> LiteralArgumentBuilder<CommandSourceStack> addFieldToSetter(LiteralArgumentBuilder<CommandSourceStack> setter, MapDataField<T> field) {
        return setter.then(field.dataType.setterSubcommand(
                Commands.literal(field.key.getKey()),
                field,
                (ctx, value) -> {
                    field.set(ctx, value);
                    ctx.getSource().getSender().sendMessage(Component.text("Field '" + field.key.getKey() + "' set to ").append(field.dataType.view(value)));
                    @Nullable Editor editor = plugin.mapManager.getEditor(ctx.getSource().getLocation().getWorld());
                    if (editor != null) {
                        editor.onMapChange();
                    }
                }
        ));
    }

    private static <T> Component getAndView(PersistentDataContainer pdc, MapDataField<T> field) {
        return field.dataType.view(field.get(pdc));
    }
}
