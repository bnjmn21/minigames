package bnjmn21.minigames.maps;

import bnjmn21.minigames.Game;
import bnjmn21.minigames.Minigames;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static bnjmn21.minigames.util.Paths.path;

public class MapManager implements LeaveWorldListener {
    private final Map<String, GameMap> allMaps;
    private final HashMap<String, Optional<Editor>> editorMaps = new HashMap<>();
    private final Minigames plugin;
    private static final String editorMapPath = "map_editor";
    private final HashMap<String, ArrayList<Player>> teleportOnceLoaded = new HashMap<>();

    public MapManager(Minigames plugin) {
        this.plugin = plugin;
        this.allMaps = new HashMap<>();
        for (String map : discoverMaps(path(editorMapPath))) {
            plugin.getLogger().warning("The editor for `" + map + "` wasn't closed correctly. Saving...");
            String originalMap = Paths.toString(path(GameMap.path).resolve(path(editorMapPath).relativize(path(map))));
            GameMap.Writeable.recover(map, originalMap);
            plugin.getLogger().info("Saved " + map);
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public boolean isEditor(World world) {
        return editorMaps.containsKey(world.getName());
    }

    public boolean isEditorOfGame(World world, Game game) {
        if (isEditor(world)) {
            var maybeEditorMap = editorMaps.get(world.getName());
            if (maybeEditorMap.isPresent()) {
                return maybeEditorMap.get().map.original.game == game;
            }
        }
        return false;
    }

    public @Nullable Editor getEditor(World world) {
        var maybeEditor = editorMaps.get(world.getName());
        if (maybeEditor == null) {
            return null;
        }
        return editorMaps.get(world.getName()).orElse(null);
    }

    public void loadGameMaps(Game game, String mapDirectory) {
        var gameMaps = MapManager.discoverMaps(path(GameMap.path).resolve(mapDirectory)).stream()
                .collect(Collectors.toMap(k -> k, name -> new GameMap(name, game, plugin)));
        this.allMaps.putAll(gameMaps);

        plugin.getLogger().info("Found the following " + game.friendlyName + " maps:");
        for (String map : this.allMaps.keySet()) {
            plugin.getLogger().info("- " + map);
        }
    }

    private static String editorMapNameOf(String map) {
        return Paths.toString(path(editorMapPath).resolve(path(GameMap.path).relativize(path(map))));
    }

    /**
     * Teleports the player to a temporary editor for the given map.
     * The editor is closed and saved when all players leave the editor.
     */
    public void editMap(String map, Player player) {
        String editorMapName = editorMapNameOf(map);
        if (editorMaps.containsKey(editorMapName) && editorMaps.get(editorMapName).isPresent()) {
            tpToEditor(editorMaps.get(editorMapName).get(), player);
            return;
        }

        player.sendMessage(Component.text("Loading map...").color(NamedTextColor.GRAY));
        teleportPlayerOnceLoaded(editorMapName, player);

        if (!editorMaps.containsKey(editorMapName)) {
            editorMaps.put(editorMapName, Optional.empty());
            allMaps.get(map).createWritableCopy(editorMapName, world -> {
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
        player.teleport(editor.map.world.getSpawnLocation());
        Minigames.resetPlayer(player, GameMode.CREATIVE);
        player.sendMessage(Component.text("You are now in the map editor. Use /l to return to the lobby."));
    }

    public LiteralCommandNode<CommandSourceStack> editCommand() {
        return Commands.literal("edit")
            .requires(ctx -> ctx.getSender().hasPermission("minigames.edit"))
            .then(Commands.argument("world", StringArgumentType.string())
                .suggests((context, builder) -> {
                    allMaps.keySet().forEach(name -> {
                        builder.suggest("\"" +
                                Paths.toString(path(GameMap.path).relativize(path(name)))
                        + "\"");
                    });
                    return builder.buildFuture();
                })
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("Only players can use this command!")
                                .color(NamedTextColor.RED));
                        return 0;
                    }

                    String mapName = StringArgumentType.getString(context, "world");
                    String map = Paths.toString(path(GameMap.path).resolve(mapName));

                    if (!allMaps.containsKey(map)) {
                        sender.sendMessage(Component.text("Couldn't find map" + map + "!")
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

                    String mapName = StringArgumentType.getString(ctx, "world");
                    String map = Paths.toString(path(GameMap.path).resolve(mapName));
                    if (!allMaps.containsKey(map)) {
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
                    plugin.getLogger().info("Saving " + editor.map.copyName + ", no players left");
                    editorMaps.remove(event.world().getName());
                    editor.sidebar.close();
                    editor.map.save(() -> {
                        plugin.getLogger().info("Saved " + editor.map.copyName);
                    });
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
