package bnjmn21.minigames;

import bnjmn21.minigames.framework.PlayerDataManager;
import bnjmn21.minigames.util.Scoreboards;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.UUID;

public class Ranks {
    public Team noneTeam;
    public Team vipTeam;
    public Team vipPlusTeam;
    public Team mvpPlusTeam;
    public HashMap<UUID, Team> players = new HashMap<>();

    Ranks(PlayerDataManager data) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        noneTeam = Scoreboards.registerAnonymousTeam(scoreboard);
        noneTeam.color(NamedTextColor.GRAY);
        vipTeam = Scoreboards.registerAnonymousTeam(scoreboard);
        vipTeam.color(NamedTextColor.GREEN);
        vipTeam.prefix(Component.text("[VIP] "));
        vipPlusTeam = Scoreboards.registerAnonymousTeam(scoreboard);
        vipPlusTeam.color(NamedTextColor.GREEN);
        vipPlusTeam.prefix(Component.text("[VIP").append(Component.text("+", NamedTextColor.GOLD), Component.text("] ")));
        mvpPlusTeam = Scoreboards.registerAnonymousTeam(scoreboard);
        mvpPlusTeam.color(NamedTextColor.AQUA);
        mvpPlusTeam.prefix(Component.text("[MVP] "));
        for (var entry : data.getAll(Minigames.ns("rank"), new TypeToken<>() {}, () -> "none")) {
            switch (entry.getSecond()) {
                case "none" -> players.put(entry.getFirst(), noneTeam);
                case "vip" -> players.put(entry.getFirst(), vipTeam);
                case "vip+" -> players.put(entry.getFirst(), vipPlusTeam);
                case "mvp" -> players.put(entry.getFirst(), mvpPlusTeam);
            }
        }
    }

    public void addToTeam(Player player) {
        players.get(player.getUniqueId()).addPlayer(player);
    }
}
