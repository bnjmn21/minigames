package bnjmn21.minigames.framework;

import bnjmn21.minigames.Game;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class Settings {
    public Game game;
    public MapMode mapMode;
    public String selectedMap;
    public HashMap<UUID, Optional<Integer>> team;

    public enum MapMode {
        Random,
        Selected
    }

    public void showSettingsDialog(Player player) {
//        Dialog dialog = Dialog.create(builder -> builder.empty()
//                .base(DialogBase.builder(Component.text("Settings")).canCloseWithEscape(true).build())
//                .type(DialogType.multiAction(
//                        List.of(ActionButton.builder(Component.text("")).build())
//                ))
//        );
    }

    private static class Dropdown<T> {
        public final Dialog dialog;

        Dropdown(Component title, Map<T, Component> values, Consumer<T> setter) {
            List<ActionButton> actions = values.entrySet().stream()
                    .map(entry -> ActionButton.create(
                            entry.getValue(),
                            null,
                            100,
                            DialogAction.customClick((response, audience) -> {
                                setter.accept(entry.getKey());
                            }, ClickCallback.Options.builder().build())
                    ))
                    .toList();

            dialog = Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(title).canCloseWithEscape(true).afterAction(DialogBase.DialogAfterAction.CLOSE).build())
                    .type(DialogType.multiAction(actions).columns(2).build())
            );
        }
    }
}
