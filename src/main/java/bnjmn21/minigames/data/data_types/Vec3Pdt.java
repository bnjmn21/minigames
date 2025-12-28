package bnjmn21.minigames.data.data_types;

import bnjmn21.minigames.data.ByteBufPdt;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class Vec3Pdt implements ByteBufPdt<Vec3> {
    public static final Vec3Pdt INSTANCE = new Vec3Pdt();
    private Vec3Pdt() {}

    @Override
    public int bytes(@NotNull Vec3 data) {
        return Float.BYTES * 3;
    }

    @Override
    public void toByteBuf(@NotNull Vec3 data, @NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        bb.putFloat(data.x);
        bb.putFloat(data.y);
        bb.putFloat(data.z);
    }

    @Override
    public @NotNull Vec3 fromByteBuf(@NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        float x = bb.getFloat();
        float y = bb.getFloat();
        float z = bb.getFloat();
        return new Vec3(x, y, z);
    }

    @Override
    public @NotNull Class<Vec3> getComplexType() {
        return Vec3.class;
    }
}