package bnjmn21.minigames.util;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.concurrent.atomic.AtomicInteger;

public class Scoreboards {
    private static final AtomicInteger teamId = new AtomicInteger();

    public static Team registerAnonymousTeam(Scoreboard board) {
        return board.registerNewTeam("t" + teamId.getAndIncrement());
    }
}
