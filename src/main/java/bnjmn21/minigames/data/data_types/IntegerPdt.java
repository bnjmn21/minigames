package bnjmn21.minigames.data.data_types;

import bnjmn21.minigames.data.MapDataField;
import bnjmn21.minigames.data.MapDataType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class IntegerPdt implements MapDataType<Integer> {
    public static IntegerPdt INSTANCE = new IntegerPdt();

    private IntegerPdt() {}

    @Override
    public Component view(Integer data) {
        return Component.text(data).color(NamedTextColor.AQUA);
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> setterSubcommand(
            ArgumentBuilder<CommandSourceStack, ?> command,
            MapDataField<Integer> field,
            BiConsumer<CommandContext<CommandSourceStack>, Integer> setter) {
        return command.then(Commands.argument("value", IntegerArgumentType.integer(0))
                .executes(ctx -> {
                    setter.accept(ctx, ctx.getArgument("value", int.class));
                    return 1;
        }));
    }

    @Override
    public int bytes(@NotNull Integer data) {
        return Integer.BYTES;
    }

    @Override
    public void toByteBuf(@NotNull Integer data, @NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        bb.putInt(data);
    }

    @Override
    public @NotNull Integer fromByteBuf(@NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        return bb.getInt();
    }

    @Override
    public @NotNull Class<Integer> getComplexType() {
        return Integer.class;
    }
}
