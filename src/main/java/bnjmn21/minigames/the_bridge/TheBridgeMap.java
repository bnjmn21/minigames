package bnjmn21.minigames.the_bridge;

import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.data.MapDataBuilder;
import bnjmn21.minigames.data.MapDataField;
import bnjmn21.minigames.data.data_types.IVec3;
import bnjmn21.minigames.data.data_types.IVec3Pdt;
import bnjmn21.minigames.data.data_types.IntegerPdt;
import net.kyori.adventure.text.Component;
import org.bukkit.GameRule;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Objects;

public class TheBridgeMap {
    public final MapDataField<IVec3> center;
    public final MapDataField<Integer> buildLimit;
    public final MapDataField<IVec3> redSpawn;
    public final MapDataField<IVec3> redGoal;
    public final MapDataField<IVec3> blueSpawn;
    public final MapDataField<IVec3> blueGoal;
    public final MapDataBuilder data;

    record Data(
            IVec3 center, Integer buildLimit,
            IVec3 redSpawn, IVec3 redGoal,
            IVec3 blueSpawn, IVec3 blueGoal
            ) {
        public Data(PersistentDataContainer pdc, TheBridgeMap fields) {
            this(
                    Objects.requireNonNull(fields.center.get(pdc)),
                    Objects.requireNonNull(fields.buildLimit.get(pdc)),
                    Objects.requireNonNull(fields.redSpawn.get(pdc)),
                    Objects.requireNonNull(fields.redGoal.get(pdc)),
                    Objects.requireNonNull(fields.blueSpawn.get(pdc)),
                    Objects.requireNonNull(fields.blueGoal.get(pdc))
            );
        }
    }

    public TheBridgeMap(Minigames plugin) {
        MapDataBuilder builder = new MapDataBuilder(plugin);
        center = builder.add("center", IVec3Pdt.INSTANCE);
        buildLimit = builder.add("buildLimit", IntegerPdt.INSTANCE);
        redSpawn = builder.add("redSpawn", IVec3Pdt.INSTANCE);
        redGoal = builder.add("redGoal", IVec3Pdt.INSTANCE);
        blueSpawn = builder.add("blueSpawn", IVec3Pdt.INSTANCE);
        blueGoal = builder.add("blueGoal", IVec3Pdt.INSTANCE);

        builder.addGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        builder.addGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);
        builder.addGameRule(GameRule.DISABLE_RAIDS, true);
        builder.addGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        builder.addGameRule(GameRule.DO_ENTITY_DROPS, false);
        builder.addGameRule(GameRule.DO_FIRE_TICK, false);
        builder.addGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        builder.addGameRule(GameRule.DO_INSOMNIA, false);
        builder.addGameRule(GameRule.DO_MOB_LOOT, false);
        builder.addGameRule(GameRule.DO_MOB_SPAWNING, false);
        builder.addGameRule(GameRule.DO_PATROL_SPAWNING, false);
        builder.addGameRule(GameRule.DO_TILE_DROPS, false);
        builder.addGameRule(GameRule.DO_TRADER_SPAWNING, false);
        builder.addGameRule(GameRule.DO_VINES_SPREAD, false);
        builder.addGameRule(GameRule.DO_WARDEN_SPAWNING, false);
        builder.addGameRule(GameRule.DO_WEATHER_CYCLE, false);
        builder.addGameRule(GameRule.DROWNING_DAMAGE, false);
        builder.addGameRule(GameRule.FIRE_DAMAGE, false);
        builder.addGameRule(GameRule.FREEZE_DAMAGE, false);
        builder.addGameRule(GameRule.FALL_DAMAGE, false);
        builder.addGameRule(GameRule.KEEP_INVENTORY, false);
        builder.addGameRule(GameRule.LOCATOR_BAR, false);
        builder.addGameRule(GameRule.NATURAL_REGENERATION, false);
        builder.addGameRule(GameRule.ALLOW_ENTERING_NETHER_USING_PORTALS, false);
        builder.addGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        builder.addGameRule(GameRule.SPAWN_MONSTERS, false);
        builder.addGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        builder.addGameRule(GameRule.WATER_SOURCE_CONVERSION, false);
        builder.addGameRule(GameRule.TNT_EXPLODES, false);
        builder.setHelp(Component.text(
        """
                
                --- Required fields ---
                - 'center': The center of the bridge. Should be set to the block above the white terracotta column.
                - 'buildLimit': How far you can build from the center.
                        If the center is at [0, 64, 0], then a block at [buildLimit, 64, 0] would be just inside the limit.
                - 'redSpawn': The spawn point of the red team. Must be in the center of the cage.
                - 'redGoal': The center of the red goal. See below for more info.
                - 'blueSpawn' and 'blueGoal' similarly.
                
                --- Goals ---
                The goals should be built from sponge blocks.
                The 'redGoal' and 'blueGoal' positions should be placed one block above the center.
                """
        ));
        data = builder;
    }
}
