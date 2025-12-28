package bnjmn21.minigames.data;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface ByteBufPdt<T> extends PersistentDataType<byte[], T> {
    int bytes(@NotNull T data);
    void toByteBuf(@NotNull T data, @NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context);
    @NotNull T fromByteBuf(@NotNull ByteBuffer bb, @NotNull PersistentDataAdapterContext context);

    @Override
    default @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    default byte @NotNull [] toPrimitive(@NotNull T complex, @NotNull PersistentDataAdapterContext context) {
        ByteBuffer bb = ByteBuffer.allocate(bytes(complex));
        toByteBuf(complex, bb, context);
        return bb.array();
    }

    @Override
    default @NotNull T fromPrimitive(byte @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
        ByteBuffer bb = ByteBuffer.wrap(primitive);
        return fromByteBuf(bb, context);
    }
}
