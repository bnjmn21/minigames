package bnjmn21.minigames.util;

import net.kyori.adventure.text.Component;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class Scoreboards {
    private static final AtomicInteger teamId = new AtomicInteger();

    public static Team registerAnonymousTeam(Scoreboard board) {
        return board.registerNewTeam("t" + teamId.getAndIncrement());
    }

    public static @NotNull Objective registerAnonymousObjective(Scoreboard scoreboard, Criteria objective, Component displayName, RenderType renderType) {
        return scoreboard.registerNewObjective("o" + teamId.getAndIncrement(), objective, displayName, renderType);
    }
}
