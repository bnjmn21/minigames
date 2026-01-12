package bnjmn21.minigames.framework.ui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class Ui {
    public static final int BUTTON_WIDTH = 150;
    public static final int SMALL_BUTTON_WIDTH = 100;

    public static ButtonBuilder button(Component text) {
        return new ButtonBuilder(text);
    }

    public static <T> DropdownBuilder<T> dropdown(Component name) {
        return new DropdownBuilder<>(name);
    }

    public static Dialog multiAction(Player player, Component title, Stream<ActionButton> buttons, int columns, @Nullable Supplier<Dialog> previous) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(tx(player, title)).canCloseWithEscape(true).build())
                .type(DialogType.multiAction(buttons.map(b -> tx(player, b)).toList()).columns(columns).exitAction(backButton(player, previous)).build())
        );
    }

    public static ActionButton backButton(Player player, @Nullable Supplier<Dialog> previous) {
        return Ui.button(tx(player, Component.translatable("ui.back")))
                .width(SMALL_BUTTON_WIDTH)
                .onClick(audience -> {
                    if (previous != null) {
                        audience.showDialog(previous.get());
                    }
                })
                .build();
    }

    /**
     * A temporary workaround for <a href="https://github.com/PaperMC/Paper/issues/12971">#12971</a>
     */
    public static Component tx(Player player, Component comp) {
        return GlobalTranslator.render(comp, player.locale());
    }

    /**
     * A temporary workaround for <a href="https://github.com/PaperMC/Paper/issues/12971">#12971</a>
     */
    private static ActionButton tx(Player player, ActionButton btn) {
        return ActionButton.create(tx(player, btn.label()), btn.tooltip() != null ? tx(player, btn.tooltip()) : null, btn.width(), btn.action());
    }

    public static class ButtonBuilder {
        public Component text;
        public @Nullable Component tooltip;
        public int width = BUTTON_WIDTH;
        public DialogActionCallback onClick = (a, b) -> {};

        ButtonBuilder(Component text) {
            this.text = text;
        }

        public ButtonBuilder tooltip(Component tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public ButtonBuilder width(int width) {
            this.width = width;
            return this;
        }

        public ButtonBuilder onClick(DialogActionCallback onClick) {
            this.onClick = onClick;
            return this;
        }

        public ButtonBuilder onClick(Consumer<Audience> onClick) {
            return onClick((ignored, audience) -> onClick.accept(audience));
        }

        public ButtonBuilder onClick(Runnable onClick) {
            return onClick((ignored1, ignored2) -> onClick.run());
        }

        public ActionButton build() {
            return ActionButton.create(text, tooltip, width, DialogAction.customClick(onClick, ClickCallback.Options.builder().build()));
        }
    }

    public static class DropdownBuilder<T> {
        public Component title;
        public Stream<T> values;
        public Function<T, Component> makeName;
        public Supplier<T> getter;
        public Consumer<T> setter;
        @Nullable Supplier<Dialog> previous;

        public DropdownBuilder(Component title) {
            this.title = title;
        }

        public DropdownBuilder<T> values(Stream<T> values) {
            this.values = values;
            return this;
        }

        public DropdownBuilder<T> makeName(Function<T, Component> makeName) {
            this.makeName = makeName;
            return this;
        }

        public DropdownBuilder<T> getter(Supplier<T> getter) {
            this.getter = getter;
            return this;
        }

        public DropdownBuilder<T> setter(Consumer<T> setter) {
            this.setter = setter;
            return this;
        }

        public DropdownBuilder<T> previous(@Nullable Supplier<Dialog> previous) {
            this.previous = previous;
            return this;
        }

        public Dropdown<T> build(Player player) {
            return new Dropdown<>(player, title, values, makeName, getter, setter, previous);
        }
    }

    public static class Dropdown<T> {
        public final ActionButton button;

        Dropdown(
                Player player,
                Component title,
                Stream<T> values,
                Function<T, Component> name,
                Supplier<T> getter,
                Consumer<T> setter,
                @Nullable Supplier<Dialog> previous) {
            Stream<ActionButton> actions = values.map(entry -> ActionButton.create(
                    name.apply(entry),
                    null,
                    BUTTON_WIDTH,
                    DialogAction.customClick(
                            (response, audience) -> {
                                setter.accept(entry);
                                if (previous != null) {
                                    audience.showDialog(previous.get());
                                }
                            },
                            ClickCallback.Options.builder().build()
                    )
            ));
            Dialog dialog = multiAction(player, title, actions, 2, previous);

            button = ActionButton.create(
                    Component.translatable("ui.dropdown", title, name.apply(getter.get())),
                    null,
                    BUTTON_WIDTH,
                    DialogAction.customClick(
                            (response, audience) -> audience.showDialog(dialog),
                            ClickCallback.Options.builder().build()
                    )
            );
        }
    }
}
