package bnjmn21.minigames.data.data_types;

import bnjmn21.minigames.data.ByteBufPdt;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class PosRotPdt implements ByteBufPdt<PosRot> {
    public static final PosRotPdt INSTANCE = new PosRotPdt();
    private PosRotPdt() {}

    @Override
    public @NotNull Class<PosRot> getComplexType() {
        return PosRot.class;
    }

    @Override
    public int bytes(@NotNull PosRot data) {
        return Vec3Pdt.INSTANCE.bytes(data.pos) + (Float.BYTES * 2);
    }

    @Override
    public void toByteBuf(@NotNull PosRot data, @NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        Vec3Pdt.INSTANCE.toByteBuf(data.pos, bb, context);
        bb.putFloat(data.pitch);
        bb.putFloat(data.yaw);
    }

    @Override
    public @NotNull PosRot fromByteBuf(@NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context) {
        Vec3 pos = Vec3Pdt.INSTANCE.fromByteBuf(bb, context);
        float pitch = bb.getFloat();
        float yaw = bb.getFloat();
        return new PosRot(pos, pitch, yaw);
    }
}
