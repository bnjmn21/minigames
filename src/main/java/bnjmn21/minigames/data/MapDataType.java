package bnjmn21.minigames.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;

import java.util.function.BiConsumer;

public interface MapDataType<T> extends ByteBufPdt<T> {
    Component view(T data);
    ArgumentBuilder<CommandSourceStack, ?> setterSubcommand(
            ArgumentBuilder<CommandSourceStack, ?> command,
            MapDataField<T> field,
            BiConsumer<CommandContext<CommandSourceStack>, T> setter);
}
