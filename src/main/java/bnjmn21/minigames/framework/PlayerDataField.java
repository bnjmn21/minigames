package bnjmn21.minigames.framework;

import com.google.gson.reflect.TypeToken;
import org.bukkit.NamespacedKey;

import java.util.function.Supplier;

public class PlayerDataField<T> {
    final NamespacedKey name;
    final TypeToken<T> type;
    final Supplier<T> defaultSupplier;

    public PlayerDataField(NamespacedKey name, TypeToken<T> type, Supplier<T> defaultSupplier) {
        this.name = name;
        this.type = type;
        this.defaultSupplier = defaultSupplier;
    }
}
