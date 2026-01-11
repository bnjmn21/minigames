package bnjmn21.minigames.framework;

import bnjmn21.minigames.Game;
import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.framework.ui.Ui;
import bnjmn21.minigames.maps.GameMap;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class Settings {
    public Game game;
    @Nullable
    public GameMap selectedMap;
    public HashMap<UUID, Team> teams;
    public boolean autoStart = true;
    private final Minigames plugin;

    public Settings(Game game, Minigames plugin) {
        this.game = game;
        this.teams = new HashMap<>();
        this.plugin = plugin;
    }

    public GameMap getMap() {
        return Objects.requireNonNullElseGet(selectedMap, () -> {
            var maps = plugin.getGameType(game).getMapManager().maps;
            return maps.values().stream().skip(new Random().nextInt(maps.size())).findFirst().orElseThrow();
        });
    }

    public HashMap<UUID, Optional<Integer>> getPlayerTeams() {
        // 1. first get all players that have a specified team and assign them
        // 2. then shuffle all random players
        // 3. assign each player to whatever team has the lowest number of players

        HashMap<Integer, Integer> playersPerTeam = new HashMap<>();
        int numTeams = plugin.getGameType(game).getTeamNames().length;
        for (int i = 0; i < numTeams; i++) {
            playersPerTeam.put(i, 0);
        }

        // 1.
        HashMap<UUID, Optional<Integer>> playerTeams = new HashMap<>();
        ArrayList<? extends Player> unsetPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Map.Entry<UUID, Team> team : teams.entrySet()) {
            int idx = unsetPlayers.indexOf(Bukkit.getPlayer(team.getKey()));
            if (idx == -1) {
                continue;
            }

            if (team.getValue() instanceof Team.GameTeam(int i)) {
                playerTeams.put(team.getKey(), Optional.of(i));
                unsetPlayers.remove(idx);
            } else if (team.getValue() instanceof Team.Spectator) {
                playerTeams.put(team.getKey(), Optional.empty());
                unsetPlayers.remove(idx);
            }
        }

        // 2.
        ArrayList<UUID> randomTeamPlayers = new ArrayList<>(unsetPlayers.stream().map(Player::getUniqueId).toList());
        Collections.shuffle(randomTeamPlayers);

        // 3.
        for (UUID player : randomTeamPlayers) {
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            var smallestTeam = playersPerTeam.entrySet().stream()
                    .min(Comparator.comparingInt(Map.Entry::getValue)).get();
            playerTeams.put(player, Optional.of(smallestTeam.getKey()));
            playersPerTeam.put(smallestTeam.getKey(), smallestTeam.getValue() + 1);
        }

        return playerTeams;
    }

    public LiteralCommandNode<CommandSourceStack> settingsCommand(String name) {
        return Commands.literal(name).executes(ctx -> {
            ctx.getSource().getSender().showDialog(settingsDialog());
            return 1;
        }).build();
    }

    private Dialog settingsDialog() {
        var gameDropdown = Ui.<Game>dropdown("Game")
                .values(Arrays.stream(Game.values()))
                .makeName(game -> Component.text(game.friendlyName))
                .getter(() -> this.game)
                .setter(res -> this.game = res)
                .previous(this::settingsDialog)
                .build();
        var mapDropdown = Ui.<Optional<GameMap>>dropdown("Map")
                .values(Stream.concat(
                        Stream.of(Optional.empty()),
                        plugin.getGameType(game).getMapManager().maps.values().stream().map(Optional::of)
                ))
                .makeName(map -> map.map(m -> m.displayName).orElse(Component.text("Random", NamedTextColor.YELLOW)))
                .getter(() -> Optional.ofNullable(selectedMap))
                .setter(map -> selectedMap = map.orElse(null))
                .previous(this::settingsDialog)
                .build();
        ActionButton teamsButton = Ui.button("Teams").onClick(aud -> aud.showDialog(teamsDialog())).build();
        ActionButton autoStartButton = Ui.button(
                Component.text("Autostart: ", NamedTextColor.WHITE).append(autoStart ?
                        Component.text("Enabled", NamedTextColor.GREEN)
                        : Component.text("Disabled", NamedTextColor.RED)
                        ))
                .onClick(aud -> {
                    autoStart = !autoStart;
                    aud.showDialog(settingsDialog());
                })
                .build();
        ActionButton startGameButton = Ui.button(Component.text("Start Game", NamedTextColor.GREEN))
                .onClick(plugin::startGame)
                .build();

        return Ui.multiAction(
                Component.text("Settings"),
                Stream.of(
                        gameDropdown.button,
                        mapDropdown.button,
                        teamsButton,
                        autoStartButton,
                        startGameButton
                ),
                1, null
        );
    }

    private Dialog teamsDialog() {
        List<SingleOptionDialogInput> inputs = plugin.lobby.world.getPlayers().stream()
                .map(p -> DialogInput.singleOption(
                        p.getUniqueId().toString().replace('-', '_'),
                        Ui.BUTTON_WIDTH,
                        playerTeamOptions(p),
                        p.displayName(),
                        true
                )).toList();

        ActionButton confirm = Ui.button("Confirm")
                .width(Ui.SMALL_BUTTON_WIDTH)
                .onClick((response, audience) -> {
                    teams.clear();
                    for (Player p : plugin.lobby.world.getPlayers()) {
                        @Nullable String teamId = response.getText(p.getUniqueId().toString().replace('-', '_'));
                        if (teamId != null) {
                            teams.put(p.getUniqueId(), Team.fromId(teamId));
                        }
                    }
                    audience.showDialog(this.settingsDialog());
                })
                .build();

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Teams")).inputs(inputs).canCloseWithEscape(true).build())
                .type(DialogType.confirmation(confirm, Ui.backButton(this::settingsDialog)))
        );
    }

    private List<SingleOptionDialogInput.OptionEntry> playerTeamOptions(Player player) {
        Component[] gameTeamNames = plugin.getGameType(game).getTeamNames();
        List<Team> gameTeams = Team.values(gameTeamNames.length);
        Team playerTeam = Objects.requireNonNullElse(teams.get(player.getUniqueId()), new Team.Random());

        return gameTeams.stream().map(team ->
                SingleOptionDialogInput.OptionEntry.create(team.id(), team.displayName(gameTeamNames), playerTeam.equals(team))
        ).toList();
    }

    public interface Team {
        Component displayName(Component[] gameTeams);
        String id();

        static Team fromId(String id) {
            return switch (id) {
                case "random" -> new Team.Random();
                case "spectator" -> new Team.Spectator();
                default -> new Team.GameTeam(Integer.parseInt(id));
            };
        }

        static List<Team> values(int gameTeams) {
            ArrayList<Team> teams = new ArrayList<>();
            teams.add(new Team.Random());
            for (int i = 0; i < gameTeams; i++) {
                teams.add(new GameTeam(i));
            }
            teams.add(new Team.Spectator());
            return teams;
        }

        class Random implements Team {
            @Override
            public Component displayName(Component[] gameTeams) {
                return Component.text("Random");
            }

            @Override
            public String id() {
                return "random";
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Random;
            }
        }

        class Spectator implements Team {
            @Override
            public Component displayName(Component[] gameTeams) {
                return Component.text("Spectator").color(NamedTextColor.GRAY);
            }

            @Override
            public String id() {
                return "spectator";
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Spectator;
            }
        }

        record GameTeam(int team) implements Team {
            @Override
            public Component displayName(Component[] gameTeams) {
                return gameTeams[team];
            }

            @Override
            public String id() {
                return String.valueOf(team);
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof GameTeam(int t) && team == t;
            }
        }
    }
}
