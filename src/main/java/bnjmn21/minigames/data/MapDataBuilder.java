package bnjmn21.minigames.data;

import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.maps.GameRuleSet;
import net.kyori.adventure.text.Component;
import org.bukkit.GameRule;

import java.util.ArrayList;

public class MapDataBuilder {
    public final ArrayList<MapDataField<?>> fields = new ArrayList<>();
    public final GameRuleSet gameRules = new GameRuleSet();
    private final Minigames plugin;
    public Component help;

    public MapDataBuilder(Minigames plugin) {
        this.plugin = plugin;
    }

    public <T> MapDataField<T> add(String name, MapDataType<T> dataType) {
        MapDataField<T> field = new MapDataField<>(name, dataType, plugin);
        fields.add(field);
        return field;
    }

    public MapDataBuilder addGameRule(GameRule<Boolean> gameRule, boolean value) {
        gameRules.with(gameRule, value);
        return this;
    }

    public MapDataBuilder addGameRule(GameRule<Integer> gameRule, int value) {
        gameRules.with(gameRule, value);
        return this;
    }

    public MapDataBuilder setHelp(Component help) {
        this.help = help;
        return this;
    }
}
