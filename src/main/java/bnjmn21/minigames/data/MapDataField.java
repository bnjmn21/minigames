package bnjmn21.minigames.data;

import bnjmn21.minigames.Minigames;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class MapDataField<T> {
    public final NamespacedKey key;
    public final MapDataType<T> dataType;

    public MapDataField(String name, MapDataType<T> dataType, Minigames plugin) {
        this.key = new NamespacedKey(plugin, name);
        this.dataType = dataType;
    }

    public void set(PersistentDataContainer container, T value) {
        container.set(key, dataType, value);
    }

    public void set(CommandContext<CommandSourceStack> ctx, T value) {
        ctx.getSource().getLocation().getWorld().getPersistentDataContainer().set(key, dataType, value);
    }

    public T get(PersistentDataContainer container) {
        return container.get(key, dataType);
    }
}
