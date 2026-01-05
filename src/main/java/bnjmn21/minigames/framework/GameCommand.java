package bnjmn21.minigames.framework;

import bnjmn21.minigames.data.MapDataField;
import bnjmn21.minigames.maps.Editor;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

public record GameCommand(GameType gameType) {
    public LiteralCommandNode<CommandSourceStack> buildCommand(String commandName) {
        var command = Commands.literal(commandName).requires(GameCommand::hasEditorPerm);

        var setterCommand = Commands.literal("set").requires(this::isInEditor);
        var unsetCommand = Commands.literal("unset").requires(this::isInEditor);
        var getterCommand = Commands.literal("get").requires(this::isInEditor);

        for (MapDataField<?> field : gameType.getMapData().fields) {
            setterCommand = addFieldToSetter(setterCommand, field);
            unsetCommand = unsetCommand.then(Commands.literal(field.key.getKey()).executes(ctx -> {
                World world = ctx.getSource().getLocation().getWorld();
                var pdc = world.getPersistentDataContainer();
                if (pdc.has(field.key)) {
                    pdc.remove(field.key);
                    @Nullable Editor editor = gameType.getMapManager().getEditor(world);
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
                .then(gameType.getMapManager().openEditorCommand())
                .then(Commands.literal("help").requires(this::isInEditor).executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(gameType.getMapData().help);
                    return 1;
                }))
                .then(Commands.literal("set_default_game_rules").requires(this::isInEditor).executes(ctx -> {
                    World world = ctx.getSource().getLocation().getWorld();
                    @Nullable Editor editor = gameType.getMapManager().getEditor(world);
                    if (editor != null) {
                        editor.disableUpdates = true; // prevent update spam due to mass-setting gamerules
                        gameType.getMapData().gameRules.applyAndResetOthers(world);
                        ctx.getSource().getSender().sendMessage(Component.text("All gamerules set."));
                        editor.disableUpdates = false;
                        editor.onMapChange();
                    }
                    return 1;
                }))
                .build();
    }

    public static boolean hasEditorPerm(CommandSourceStack ctx) {
        return ctx.getSender().hasPermission("minigames.edit");
    }

    private boolean isInEditor(CommandSourceStack ctx) {
        try {
            return gameType.getMapManager().isEditor(ctx.getLocation().getWorld());
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    private <T> LiteralArgumentBuilder<CommandSourceStack> addFieldToSetter(LiteralArgumentBuilder<CommandSourceStack> setter, MapDataField<T> field) {
        return setter.then(field.dataType.setterSubcommand(
                Commands.literal(field.key.getKey()),
                field,
                (ctx, value) -> {
                    field.set(ctx, value);
                    ctx.getSource().getSender().sendMessage(Component.text("Field '" + field.key.getKey() + "' set to ").append(field.dataType.view(value)));
                    @Nullable Editor editor = gameType.getMapManager().getEditor(ctx.getSource().getLocation().getWorld());
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
