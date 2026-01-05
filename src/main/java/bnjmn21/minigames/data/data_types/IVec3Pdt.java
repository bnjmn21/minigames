package bnjmn21.minigames.data.data_types;

import bnjmn21.minigames.data.MapDataField;
import bnjmn21.minigames.data.MapDataType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public class IVec3Pdt implements MapDataType<IVec3> {
    public static final IVec3Pdt INSTANCE = new IVec3Pdt();
    private IVec3Pdt() {}

    @Override
    public int bytes(@NotNull IVec3 data) {
        return Integer.BYTES * 3;
    }

    @Override
    public void toByteBuf(@NotNull IVec3 data, @NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        bb.putInt(data.x);
        bb.putInt(data.y);
        bb.putInt(data.z);
    }

    @Override
    public @NotNull IVec3 fromByteBuf(@NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        return new IVec3(x, y, z);
    }

    @Override
    public @NotNull Class<IVec3> getComplexType() {
        return IVec3.class;
    }

    @Override
    public Component view(IVec3 data) {
        return Component.text("[").color(NamedTextColor.GRAY).append(
                Component.text(data.x).color(NamedTextColor.AQUA),
                Component.text(", ").color(NamedTextColor.GRAY),
                Component.text(data.y).color(NamedTextColor.AQUA),
                Component.text(", ").color(NamedTextColor.GRAY),
                Component.text(data.z).color(NamedTextColor.AQUA),
                Component.text("]").color(NamedTextColor.GRAY)
        );
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ArgumentBuilder<CommandSourceStack, ?> setterSubcommand(
            ArgumentBuilder<CommandSourceStack, ?> command,
            MapDataField<IVec3> field,
            BiConsumer<CommandContext<CommandSourceStack>, IVec3> setter
    ) {
        return command.executes(ctx -> {
            Location loc = ctx.getSource().getLocation();
            IVec3 value = new IVec3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            setter.accept(ctx, value);
            return 1;
        }).then(Commands.argument("position", ArgumentTypes.blockPosition()).executes(ctx -> {
            final BlockPositionResolver blockPositionResolver = ctx.getArgument("position", BlockPositionResolver.class);
            final BlockPosition blockPosition = blockPositionResolver.resolve(ctx.getSource());
            setter.accept(ctx, new IVec3(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ()));
            return 1;
        }));
    }
}
