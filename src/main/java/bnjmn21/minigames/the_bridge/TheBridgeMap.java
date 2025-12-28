package bnjmn21.minigames.the_bridge;

import bnjmn21.minigames.Game;
import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.data.MapDataBuilder;
import bnjmn21.minigames.data.MapDataField;
import bnjmn21.minigames.data.data_types.IVec3;
import bnjmn21.minigames.data.data_types.IVec3Pdt;
import bnjmn21.minigames.data.data_types.IntegerPdt;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;

public class TheBridgeMap {
    public final MapDataField<IVec3> center;
    public final MapDataField<Integer> buildLimit;
    public final MapDataField<IVec3> redSpawn;
    public final MapDataField<IVec3> redGoal;
    public final MapDataField<IVec3> blueSpawn;
    public final MapDataField<IVec3> blueGoal;
    public final LiteralCommandNode<CommandSourceStack> command;
    public final MapDataBuilder data;

    public TheBridgeMap(Minigames plugin) {
        MapDataBuilder builder = new MapDataBuilder(Game.TheBridge, plugin);
        center = builder.add("center", IVec3Pdt.INSTANCE);
        buildLimit = builder.add("buildLimit", IntegerPdt.INSTANCE);
        redSpawn = builder.add("redSpawn", IVec3Pdt.INSTANCE);
        redGoal = builder.add("redGoal", IVec3Pdt.INSTANCE);
        blueSpawn = builder.add("blueSpawn", IVec3Pdt.INSTANCE);
        blueGoal = builder.add("blueGoal", IVec3Pdt.INSTANCE);
        command = builder.buildCommand("the_bridge_edit", Component.text(
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
