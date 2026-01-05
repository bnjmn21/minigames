package bnjmn21.minigames.maps;

import bnjmn21.minigames.Game;
import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.framework.GameCommand;
import bnjmn21.minigames.util.LeaveWorldListener;
import bnjmn21.minigames.util.Paths;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.event.world.WorldGameRuleChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static bnjmn21.minigames.util.Paths.path;

public class MapManager implements LeaveWorldListener {
    public HashMap<String, GameMap> maps = new HashMap<>();
    public final Game game;
    public final Path path;
    public final Path editorPath;
    private final HashMap<String, Optional<Editor>> editorMaps = new HashMap<>();
    private final Minigames plugin;
    private final HashMap<String, ArrayList<Player>> teleportOnceLoaded = new HashMap<>();

    public MapManager(Game game, String mapsPath, String editorPath, Minigames plugin) {
        this.plugin = plugin;
        this.game = game;
        this.path = path(mapsPath);
        this.editorPath = path(editorPath);

        for (String map : discoverMaps(path(editorPath))) {
            plugin.getLogger().warning("The editor for `" + map + "` wasn't closed correctly. Saving...");
            String originalMap = Paths.toString(this.path.resolve(this.editorPath.relativize(path(map))));
            GameMap.Writeable.recover(map, originalMap);
            plugin.getLogger().info("Saved " + map);
        }
        loadGameMaps();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public boolean isEditor(World world) {
        return editorMaps.containsKey(world.getName());
    }

    public @Nullable Editor getEditor(World world) {
        var maybeEditor = editorMaps.get(world.getName());
        if (maybeEditor == null) {
            return null;
        }
        return maybeEditor.orElse(null);
    }

    private void loadGameMaps() {
        var gameMaps = MapManager.discoverMaps(path).stream()
                .collect(Collectors.toMap(k -> k, name -> {
                    Component displayName = Component.text(shortName(name));
                    return new GameMap(name, game, displayName, plugin);
                }));
        this.maps.putAll(gameMaps);

        plugin.getLogger().info("Found the following " + game.friendlyName + " maps:");
        for (String map : this.maps.keySet()) {
            plugin.getLogger().info("- " + map);
        }
    }

    private String shortName(String fullName) {
        return fullName.substring(Paths.toString(path).length() + 1);
    }

    private String fullName(String shortName) {
        return Paths.toString(this.path.resolve(shortName));
    }

    private String editorName(String shortName) {
        return Paths.toString(this.editorPath.resolve(shortName));
    }

    /**
     * Teleports the player to a temporary editor for the given map.
     * The editor is closed and saved when all players leave the editor.
     */
    public void editMap(String shortName, Player player) {
        String editorMapName = editorName(shortName);
        if (editorMaps.containsKey(editorMapName) && editorMaps.get(editorMapName).isPresent()) {
            tpToEditor(editorMaps.get(editorMapName).get(), player);
            return;
        }

        player.sendMessage(Component.text("Loading map...").color(NamedTextColor.GRAY));
        teleportPlayerOnceLoaded(editorMapName, player);

        if (!editorMaps.containsKey(editorMapName)) {
            editorMaps.put(editorMapName, Optional.empty());
            maps.get(fullName(shortName)).createWritableCopy(editorMapName, world -> {
                Editor editor = new Editor(world, plugin);
                editorMaps.put(editorMapName, Optional.of(editor));
                for (Player p : getPlayersToTeleport(editorMapName)) {
                    tpToEditor(editor, p);
                }
            });
        }
    }

    private void teleportPlayerOnceLoaded(String map, Player player) {
        if (teleportOnceLoaded.containsKey(map)) {
            teleportOnceLoaded.get(map).add(player);
        } else {
            teleportOnceLoaded.put(map, new ArrayList<>(List.of(new Player[]{player})));
        }
    }

    private List<Player> getPlayersToTeleport(String map) {
        if (teleportOnceLoaded.containsKey(map)) {
            return teleportOnceLoaded.remove(map);
        } else {
            return List.of();
        }
    }

    private void tpToEditor(Editor editor, Player player) {
        editor.onJoin(player);
        player.teleport(editor.map.world().getSpawnLocation());
        Minigames.resetPlayer(player, GameMode.CREATIVE);
        player.sendMessage(Component.text("You are now in the map editor. Use /l to return to the lobby."));
    }

    public LiteralCommandNode<CommandSourceStack> openEditorCommand() {
        return Commands.literal("open").requires(GameCommand::hasEditorPerm)
            .then(Commands.argument("world", StringArgumentType.string())
                .suggests((ctx, builder) -> {
                    maps.keySet().forEach(name -> builder.suggest("\"" + shortName(name) + "\""));
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("Only players can use this command!")
                                .color(NamedTextColor.RED));
                        return 0;
                    }

                    String map = StringArgumentType.getString(ctx, "world");
                    if (!maps.containsKey(fullName(map))) {
                        sender.sendMessage(Component.text("Couldn't find map " + map + "!")
                                .color(NamedTextColor.RED));
                        return 0;
                    }

                    editMap(map, player);

                    return 1;
                })
            .then(Commands.argument("players", ArgumentTypes.players())
                .requires(ctx -> ctx.getSender().hasPermission("minecraft.command.selector"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();

                    String map = StringArgumentType.getString(ctx, "world");
                    if (!maps.containsKey(fullName(map))) {
                        sender.sendMessage(Component.text("Couldn't find map " + map + "!")
                                .color(NamedTextColor.RED));
                        return 0;
                    }

                    final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
                    for (Player target : targetResolver.resolve(ctx.getSource())) {
                        editMap(map, target);
                    }

                    sender.sendMessage(Component.text("Teleported players to " + map + "!"));
                    return 1;
                })))
            .build();
    }

    @Override
    public void onLeaveWorld(LeaveWorldEvent event) {
        if (editorMaps.containsKey(event.world().getName())) {
            var maybeMap = editorMaps.get(event.world().getName());
            maybeMap.ifPresent(editor -> {
                editor.onLeave(event.player());
                if (event.playersRemaining() == 0) {
                    plugin.getLogger().info("Saving " + editor.map.copyName() + ", no players left");
                    editorMaps.remove(event.world().getName());
                    editor.sidebar.close();
                    editor.map.save(() -> plugin.getLogger().info("Saved " + editor.map.copyName()));
                }
            });
        }
    }

    @EventHandler
    public void onGameRuleChange(WorldGameRuleChangeEvent event) {
        @Nullable Editor maybeEditor = getEditor(event.getWorld());
        if (maybeEditor == null) {
            return;
        }

        maybeEditor.onMapChange();
    }

    private static HashSet<String> discoverMaps(Path root) {
        HashSet<String> result = new HashSet<>();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                    if (Files.isRegularFile(dir.resolve("level.dat"))) {
                        result.add(Paths.toString(dir));
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
