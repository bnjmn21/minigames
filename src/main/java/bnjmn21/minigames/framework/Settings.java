package bnjmn21.minigames.framework;

import bnjmn21.minigames.Game;
import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.maps.GameMap;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class Settings {
    static final int BUTTON_WIDTH = 150;
    static final int SMALL_BUTTON_WIDTH = 100;

    public Game game;
    @Nullable
    public GameMap selectedMap;
    public HashMap<UUID, Team> teams;
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
        // 3. assign each player to whatever team has the least amount of players

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
        Dropdown<Game> gameDropdown = new Dropdown<>(
                Component.text("Game"),
                Arrays.stream(Game.values()),
                game -> Component.text(game.friendlyName),
                () -> this.game,
                res -> this.game = res,
                this::settingsDialog
        );
        Dropdown<Optional<GameMap>> mapDropdown = new Dropdown<>(
                Component.text("Map"),
                Stream.concat(
                        Stream.of(Optional.empty()),
                        plugin.getGameType(game).getMapManager().maps.values().stream().map(Optional::of)
                ),
                map -> map.map(m -> m.displayName).orElse(Component.text("Random").color(NamedTextColor.YELLOW)),
                () -> Optional.ofNullable(selectedMap),
                map -> selectedMap = map.orElse(null),
                this::settingsDialog
        );
        ActionButton teamsDialogButton = ActionButton.create(
                Component.text("Teams"),
                null,
                BUTTON_WIDTH,
                DialogAction.customClick(
                        (response, audience) -> audience.showDialog(teamsDialog()),
                        ClickCallback.Options.builder().build()
                )
        );
        ActionButton startGameButton = ActionButton.create(
                Component.text("Start Game", NamedTextColor.GREEN),
                null,
                BUTTON_WIDTH,
                DialogAction.customClick(
                        (response, audience) -> plugin.startGame(),
                        ClickCallback.Options.builder().build()
                )
        );

        return defaultMultiAction(
                Component.text("Settings"),
                Stream.of(
                        gameDropdown.button,
                        mapDropdown.button,
                        teamsDialogButton,
                        startGameButton
                ),
                1, null
        );
    }

    private Dialog teamsDialog() {
        List<SingleOptionDialogInput> inputs = plugin.lobby.world.getPlayers().stream()
                .map(p -> DialogInput.singleOption(
                        p.getUniqueId().toString().replace('-', '_'),
                        BUTTON_WIDTH,
                        playerTeamOptions(p),
                        p.displayName(),
                        true
                )).toList();

        ActionButton confirm = ActionButton.create(
                Component.text("Confirm"),
                null,
                SMALL_BUTTON_WIDTH,
                DialogAction.customClick((response, audience) -> {
                    teams.clear();
                    for (Player p : plugin.lobby.world.getPlayers()) {
                        @Nullable String teamId = response.getText(p.getUniqueId().toString().replace('-', '_'));
                        if (teamId != null) {
                            teams.put(p.getUniqueId(), Team.fromId(teamId));
                        }
                    }
                    audience.showDialog(this.settingsDialog());
                }, ClickCallback.Options.builder().build())
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Teams")).inputs(inputs).canCloseWithEscape(true).build())
                .type(DialogType.confirmation(confirm, back(this::settingsDialog)))
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

    private static ActionButton back(@Nullable Supplier<Dialog> previous) {
        return ActionButton.create(
                Component.text("Back"),
                null,
                SMALL_BUTTON_WIDTH,
                DialogAction.customClick((response, audience) -> {
                    if (previous != null) {
                        audience.showDialog(previous.get());
                    }
                }, ClickCallback.Options.builder().build())
        );
    }

    private static Dialog defaultMultiAction(Component title, Stream<ActionButton> buttons, int columns, @Nullable Supplier<Dialog> previous) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title).canCloseWithEscape(true).build())
                .type(DialogType.multiAction(buttons.toList()).columns(columns).exitAction(back(previous)).build())
        );
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

    private static class Dropdown<T> {
        public final ActionButton button;

        Dropdown(
                Component title,
                Stream<T> values,
                Function<T, Component> name,
                Supplier<T> getter,
                Consumer<T> setter,
                @Nullable Supplier<Dialog> previous) {
            Stream<ActionButton> actions = values.map(entry -> ActionButton.create(
                            name.apply(entry),
                            null,
                            BUTTON_WIDTH,
                            DialogAction.customClick(
                                    (response, audience) -> {
                                        setter.accept(entry);
                                        if (previous != null) {
                                            audience.showDialog(previous.get());
                                        }
                                    },
                                    ClickCallback.Options.builder().build()
                            )
                    ));
            Dialog dialog = defaultMultiAction(title, actions, 2, previous);

            button = ActionButton.create(
                    title.append(Component.text(": "), name.apply(getter.get())),
                    null,
                    BUTTON_WIDTH,
                    DialogAction.customClick(
                            (response, audience) -> audience.showDialog(dialog),
                            ClickCallback.Options.builder().build()
                    )
            );
        }
    }
}
