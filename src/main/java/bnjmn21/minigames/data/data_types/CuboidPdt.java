package bnjmn21.minigames.data.data_types;

import bnjmn21.minigames.data.ByteBufPdt;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class CuboidPdt implements ByteBufPdt<Cuboid> {
    public static final CuboidPdt INSTANCE = new CuboidPdt();
    private CuboidPdt() {}

    @Override
    public @NotNull Class<Cuboid> getComplexType() {
        return Cuboid.class;
    }

    @Override
    public int bytes(@NotNull Cuboid data) {
        return IVec3Pdt.INSTANCE.bytes(data.start) + IVec3Pdt.INSTANCE.bytes(data.size);
    }

    @Override
    public void toByteBuf(@NotNull Cuboid data, @NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        IVec3Pdt.INSTANCE.toByteBuf(data.start, bb, context);
        IVec3Pdt.INSTANCE.toByteBuf(data.size, bb, context);
    }

    @Override
    public @NotNull Cuboid fromByteBuf(@NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        IVec3 start = IVec3Pdt.INSTANCE.fromByteBuf(bb, context);
        IVec3 size = IVec3Pdt.INSTANCE.fromByteBuf(bb, context);
        return new Cuboid(start, size);
    }
}
