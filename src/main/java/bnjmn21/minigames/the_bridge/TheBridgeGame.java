package bnjmn21.minigames.the_bridge;

import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.Ranks;
import bnjmn21.minigames.data.data_types.IVec3;
import bnjmn21.minigames.framework.Countdown;
import bnjmn21.minigames.framework.DeathSystem;
import bnjmn21.minigames.framework.GameInstance;
import bnjmn21.minigames.framework.Settings;
import bnjmn21.minigames.maps.GameMap;
import bnjmn21.minigames.util.HumanReadableList;
import bnjmn21.minigames.util.LeaveWorldListener;
import bnjmn21.minigames.util.Scoreboards;
import bnjmn21.minigames.util.WorldTools;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;
import org.bukkit.structure.Structure;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class TheBridgeGame implements GameInstance {
    private static final int POINTS_TO_WIN = 5;

    private World map;
    private final Component mapName;
    private Component gameName;
    public int redPoints = 0;
    public int bluePoints = 0;
    private final ArrayList<Entity> visibleToRed = new ArrayList<>();
    private final ArrayList<Entity> visibleToBlue = new ArrayList<>();
    private final ArrayList<Entity> visibleToSpectators = new ArrayList<>();
    private Structure redCage;
    private Structure blueCage;
    private Team redTeam;
    private Team blueTeam;
    private final ArrayList<UUID> redPlayers = new ArrayList<>();
    private final ArrayList<UUID> bluePlayers = new ArrayList<>();
    private Team spectatorTeam;
    private Sidebar sidebar;
    private ComponentSidebarLayout sidebarLayout;
    private TheBridgeMap.Data data;
    private State state = State.NotStarted;
    private final DeathSystem deathSystem = new DeathSystem();
    @Nullable private Countdown currentCountdown;
    private Objective hpObjective;
    private final HashMap<UUID, Integer> playerGoals = new HashMap<>();
    private final HashMap<UUID, Integer> kills = new HashMap<>();
    private final Minigames plugin;
    private final Set<UUID> hasSaidGG = new HashSet<>();
    private final HashMap<UUID, Integer> streaks = new HashMap<>();
    private final HashMap<UUID, Integer> longestStreaks = new HashMap<>();
    private final HashSet<IVec3> placedBlocks = new HashSet<>();

    enum State {
        NotStarted,
        Started,
        Ended
    }

    public TheBridgeGame(Settings settings, Minigames plugin) {
        this.plugin = plugin;
        GameMap map = settings.getMap();
        this.mapName = map.displayName;
        plugin.tempMaps.create(map, world -> {
            this.map = world;
            try {
                this.data = new TheBridgeMap.Data(world.getPersistentDataContainer(), plugin.theBridge.map);
            } catch (NullPointerException e) {
                Bukkit.getServer().sendMessage(Component.translatable("general.failed_to_load_map", NamedTextColor.RED, this.mapName));
                return;
            }
            buildMap();
            initTeams(settings);
            selectCages();
            currentCountdown = new Countdown(world, 5, () -> {
                currentCountdown = null;
                startGame();
            });
        });
    }

    private void startGame() {
        for (Player player : map.getPlayers()) {
            if (redTeam.hasPlayer(player)) {
                visibleToRed.forEach(e -> player.showEntity(plugin, e));
            } else if (blueTeam.hasPlayer(player)) {
                visibleToBlue.forEach(e -> player.showEntity(plugin, e));
            } else if (spectatorTeam.hasPlayer(player)) {
                visibleToSpectators.forEach(e -> player.showEntity(plugin, e));
            }
        }
        state = State.Started;
        startRound(Component.empty());
    }

    private void startRound(Component subtitle) {
        map.clearTitle();
        placeCage(redCage, data.redSpawn());
        placeCage(blueCage, data.blueSpawn());
        for (Player player : map.getPlayers()) {
            if (redTeam.hasPlayer(player)) {
                setupPlayer(player, false);
                player.teleport(spawnLocation(false));
            } else if (blueTeam.hasPlayer(player)) {
                setupPlayer(player, true);
                player.teleport(spawnLocation(true));
            }
        }
        currentCountdown = new Countdown(map, 5, subtitle, () -> {
            map.clearTitle();
            removeCage(data.redSpawn());
            removeCage(data.blueSpawn());
        });
    }

    private void setupPlayer(Player player, boolean blue) {
        Minigames.resetPlayer(player, GameMode.SURVIVAL);
        var inv = player.getInventory();
        plugin.playerData.get(player.getUniqueId(), HotbarItem.hotbarField).apply(inv, blue);
        inv.setItem(9, new ItemStack(Material.ARROW));
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        boots.editMeta(meta -> {
            meta.setUnbreakable(true);
            ((LeatherArmorMeta) meta).setColor(blue ? Color.BLUE : Color.RED);
        });
        inv.setItem(EquipmentSlot.FEET, boots);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        leggings.editMeta(meta -> {
            meta.setUnbreakable(true);
            ((LeatherArmorMeta) meta).setColor(blue ? Color.BLUE : Color.RED);
        });
        inv.setItem(EquipmentSlot.LEGS, leggings);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        chestplate.editMeta(meta -> {
            meta.setUnbreakable(true);
            ((LeatherArmorMeta) meta).setColor(blue ? Color.BLUE : Color.RED);
        });
        inv.setItem(EquipmentSlot.CHEST, chestplate);
    }

    private void buildMap() {
        buildGoal(data.redGoal(), false);
        buildGoal(data.blueGoal(), true);
        removeCage(data.redSpawn());
        removeCage(data.blueSpawn());
    }

    private void initTeams(Settings settings) {
        var playerTeams = settings.getPlayerTeams();
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        redTeam = Scoreboards.registerAnonymousTeam(board);
        redTeam.setAllowFriendlyFire(false);
        redTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        redTeam.color(NamedTextColor.RED);
        blueTeam = Scoreboards.registerAnonymousTeam(board);
        blueTeam.setAllowFriendlyFire(false);
        blueTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        blueTeam.color(NamedTextColor.BLUE);
        spectatorTeam = Scoreboards.registerAnonymousTeam(board);
        spectatorTeam.color(NamedTextColor.GRAY);
        hpObjective = Scoreboards.registerAnonymousObjective(board, Criteria.HEALTH, Component.text("❤", NamedTextColor.RED), RenderType.INTEGER);
        hpObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        for (Map.Entry<UUID, Optional<Integer>> playerTeam : playerTeams.entrySet()) {
            Player player = Bukkit.getPlayer(playerTeam.getKey());
            if (player == null) {
                continue;
            }
            player.teleport(map.getSpawnLocation());
            if (playerTeam.getValue().equals(Optional.of(0))) {
                redPlayers.add(player.getUniqueId());
                redTeam.addPlayer(player);
                Minigames.resetPlayer(player, GameMode.SURVIVAL);
            } else if (playerTeam.getValue().equals(Optional.of(1))) {
                bluePlayers.add(player.getUniqueId());
                blueTeam.addPlayer(player);
                Minigames.resetPlayer(player, GameMode.SURVIVAL);
            } else {
                spectatorTeam.addPlayer(player);
                Minigames.resetPlayer(player, GameMode.SPECTATOR);
            }
        }
        gameName = Component.translatable(
                "the_bridge.name_with_players",
                Component.text(redTeam.getSize()),
                Component.text(blueTeam.getSize())
        );
        initSidebars();
        for (UUID uuid : playerTeams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            sidebar.addPlayer(player);
        }
        sidebarLayout.apply(sidebar);
    }

    private void initSidebars() {
        this.sidebar = plugin.scoreboardLibrary.createSidebar(15);
        SidebarComponent sidebarComponent = SidebarComponent.builder()
                .addStaticLine(Component.translatable("the_bridge.sidebar.map", NamedTextColor.GRAY, this.mapName.color(NamedTextColor.WHITE)))
                .addBlankLine()
                .addDynamicLine(() -> Component.translatable("the_bridge.sidebar.red", NamedTextColor.RED,
                        Component.text("⬤".repeat(redPoints)).append(Component.text("⬤".repeat(POINTS_TO_WIN - redPoints), NamedTextColor.GRAY)))
                )
                .addDynamicLine(() -> Component.translatable("the_bridge.sidebar.blue", NamedTextColor.BLUE,
                        Component.text("⬤".repeat(bluePoints)).append(Component.text("⬤".repeat(POINTS_TO_WIN - bluePoints), NamedTextColor.GRAY)))
                )
                .build();
        this.sidebarLayout = new ComponentSidebarLayout(
                SidebarComponent.staticLine(gameName.color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)),
                sidebarComponent
        );
    }

    private void selectCages() {
        ArrayList<UUID> randomRedPlayers = new ArrayList<>(redPlayers);
        Collections.shuffle(randomRedPlayers);
        String redCageId = "default";
        for (UUID player : randomRedPlayers) {
            String cage = plugin.playerData.get(player, Cages.cageField);
            if (!Objects.equals(cage, "default")) {
                redCageId = cage;
            }
        }

        ArrayList<UUID> randomBluePlayers = new ArrayList<>(bluePlayers);
        Collections.shuffle(randomRedPlayers);
        String blueCageId = "default";
        for (UUID player : randomBluePlayers) {
            String cage = plugin.playerData.get(player, Cages.cageField);
            if (!Objects.equals(cage, "default")) {
                blueCageId = cage;
            }
        }

        this.redCage = plugin.theBridge.cages.load(redCageId, false);
        this.blueCage = plugin.theBridge.cages.load(blueCageId, true);
    }

    @Override
    public void stopGame() {
        sidebar.close();
        try {
            redTeam.unregister();
            blueTeam.unregister();
            spectatorTeam.unregister();
            hpObjective.unregister();
        } catch (IllegalStateException ignored) {}
        map.getPlayers().forEach(plugin.lobby::teleportToLobby);
        plugin.tempMaps.destroy(map);
        plugin.killCurrentGame();
    }

    @Override
    public void onPlayerLeaveWorld(LeaveWorldListener.LeaveWorldEvent event) {
        // really awful hack
        try {
            redTeam.removePlayer(event.player());
        } catch (IllegalStateException ignored) {}
        try {
            blueTeam.removePlayer(event.player());
        } catch (IllegalStateException ignored) {}
        try {
            spectatorTeam.removePlayer(event.player());
        } catch (IllegalStateException ignored) {}
        if (event.playersRemaining() == 0) {
            stopGame();
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        IVec3 pos = IVec3.of(event.getBlock().getLocation());
        boolean allowed = (isBridge(pos) || isInBuildableRegion(pos)) && placedBlocks.remove(pos);
        event.setCancelled(!allowed);
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        IVec3 pos = IVec3.of(event.getBlockPlaced().getLocation());

        if (!isInBuildableRegion(pos)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.translatable("general.cant_place_blocks_here", NamedTextColor.RED));
            return;
        }

        if (data.center().y + 7 == pos.y) {
            if (event.getBlockPlaced().getType() == Material.RED_TERRACOTTA) {
                event.getBlockPlaced().setType(Material.RED_CONCRETE);
            } else if (event.getBlockPlaced().getType() == Material.BLUE_TERRACOTTA) {
                event.getBlockPlaced().setType(Material.BLUE_CONCRETE);
            }
        }

        this.placedBlocks.add(pos);
        event.getItemInHand().setAmount(64);
    }

    @Override
    public World getWorld() {
        return map;
    }

    private void winGame(boolean blue) {
        state = State.Ended;
        for (Player player : map.getPlayers()) {
            Minigames.resetPlayer(player, GameMode.SPECTATOR);
            player.teleport(selectRespawnLocation(player));
        }
        if (blue) {
            map.showTitle(Title.title(Component.translatable("the_bridge.blue_won", NamedTextColor.BLUE), Component.empty(), 10, 100, 10));
        } else {
            map.showTitle(Title.title(Component.translatable("the_bridge.red_won", NamedTextColor.RED), Component.empty(), 10, 100, 10));
        }
        plugin.gameFinished();
        Location red_goal = data.redGoal().center(map);
        map.spawn(red_goal.add(0, 20, 0), Firework.class, entity -> {
            entity.setTicksToDetonate(20);
            FireworkMeta meta = entity.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(blue ? Color.BLUE : Color.RED).flicker(true).trail(true).build());
            entity.setFireworkMeta(meta);
        });
        Location blue_goal = data.blueGoal().center(map);
        map.spawn(blue_goal.add(0, 20, 0), Firework.class, entity -> {
            entity.setTicksToDetonate(20);
            FireworkMeta meta = entity.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(blue ? Color.BLUE : Color.RED).flicker(true).trail(true).build());
            entity.setFireworkMeta(meta);
        });
    }

    @Override
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(selectRespawnLocation(event.getPlayer()));
    }

    @Override
    public void onPlayerPostRespawn(PlayerPostRespawnEvent event) {
        postRespawn(event.getPlayer());
    }

    private Location selectRespawnLocation(Player player) {
        if (state != State.Started) {
            return map.getSpawnLocation();
        }

        if (redTeam.hasPlayer(player)) {
            return spawnLocation(false);
        } else if (blueTeam.hasPlayer(player)) {
            return spawnLocation(true);
        } else {
            return map.getSpawnLocation();
        }
    }

    private void postRespawn(Player player) {
        switch (state) {
            case NotStarted -> {
                if (spectatorTeam.hasPlayer(player)) {
                    Minigames.resetPlayer(player, GameMode.SPECTATOR);
                } else {
                    Minigames.resetPlayer(player, GameMode.ADVENTURE);
                }
            }
            case Started -> {
                if (redTeam.hasPlayer(player)) {
                    setupPlayer(player, false);
                } else if (blueTeam.hasPlayer(player)) {
                    setupPlayer(player, true);
                } else {
                    Minigames.resetPlayer(player, GameMode.SPECTATOR);
                }
            }
            case Ended -> Minigames.resetPlayer(player, GameMode.SPECTATOR);
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().getLocation().getBlockY() < data.center().y - 15) {
            event.getPlayer().damage(9999999, DamageSource.builder(DamageType.OUT_OF_WORLD).build());
            return;
        }

        if (state != State.Started || event.getTo().getBlock().getType() != Material.END_PORTAL) {
            return;
        }

        if (redTeam.hasPlayer(event.getPlayer()) && event.getTo().distance(data.blueGoal().center(map)) < 10) {
            redPoints++;
            playerGoals.put(event.getPlayer().getUniqueId(), playerGoals.computeIfAbsent(event.getPlayer().getUniqueId(), ignored -> 0) + 1);
            if (redPoints >= 5) {
                winMessage(event.getPlayer(), false);
                winGame(false);
            } else {
                goalMessage(event.getPlayer(), false);
                startRound(Component.translatable("the_bridge.goal_title", NamedTextColor.RED, event.getPlayer().teamDisplayName()));
            }
        } else if (blueTeam.hasPlayer(event.getPlayer()) && event.getFrom().distance(data.redGoal().center(map)) < 10) {
            bluePoints++;
            playerGoals.put(event.getPlayer().getUniqueId(), playerGoals.computeIfAbsent(event.getPlayer().getUniqueId(), ignored -> 0) + 1);
            if (bluePoints >= 5) {
                winMessage(event.getPlayer(), true);
                winGame(true);
            } else {
                goalMessage(event.getPlayer(), true);
                startRound(Component.translatable("the_bridge.goal_title", NamedTextColor.BLUE, event.getPlayer().teamDisplayName()));
            }
        }
        sidebarLayout.apply(sidebar);
    }

    private void goalMessage(Player player, boolean blue) {
        NamedTextColor color = blue ? NamedTextColor.BLUE : NamedTextColor.RED;
        sendHr(map);
        map.sendMessage(Component.translatable("the_bridge.goal_msg.by", color,
                player.teamDisplayName(),
                Component.translatable("general.paren_hp", NamedTextColor.GREEN, Component.text(String.format("%.1f", player.getHealth())))
        ));
        int goals = playerGoals.get(player.getUniqueId());
        map.sendMessage(Component.translatable("the_bridge.goal_msg.goal." + goals, NamedTextColor.GOLD));
        sendHr(map);
    }

    private void winMessage(Player player, boolean blue) {
        NamedTextColor color = blue ? NamedTextColor.BLUE : NamedTextColor.RED;
        map.sendMessage(Component.empty());
        sendHr(map);
        map.sendMessage(Component.translatable("the_bridge.goal_msg.by", color,
                player.teamDisplayName(),
                Component.translatable("general.paren_hp", NamedTextColor.GREEN, Component.text(String.format("%.1f", player.getHealth())))
        ));
        int goals = playerGoals.get(player.getUniqueId());
        map.sendMessage(Component.translatable("the_bridge.goal_msg.goal." + goals, NamedTextColor.GOLD));
        map.sendMessage(Component.empty());
        if (blue) {
            map.sendMessage(Component.translatable("the_bridge.blue_won", color).decorate(TextDecoration.BOLD));
        } else {
            map.sendMessage(Component.translatable("the_bridge.red_won", color).decorate(TextDecoration.BOLD));
        }
        var mostKills = kills.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue));
        if (mostKills.isPresent()) {
            Player mostKillsPlayer = Bukkit.getPlayer(mostKills.get().getKey());
            if (mostKillsPlayer != null) {
                map.sendMessage(Component.translatable("the_bridge.most_kills", NamedTextColor.GOLD,
                        mostKillsPlayer.teamDisplayName(),
                        Component.text(mostKills.get().getValue())
                ));
            }
        }
        var longestStreak = longestStreaks.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue));
        if (longestStreak.isPresent() && longestStreak.get().getValue() > 3) {
            Player longestStreakPlayer = Bukkit.getPlayer(longestStreak.get().getKey());
            if (longestStreakPlayer != null) {
                map.sendMessage(Component.translatable("the_bridge.longest_streak", NamedTextColor.GOLD,
                        longestStreakPlayer.teamDisplayName(),
                        Component.text(longestStreak.get().getValue())
                ));
            }
        }
        sendHr(map);
    }

    private static void sendHr(Audience aud) {
        aud.sendMessage(Component.text("▬".repeat(50), NamedTextColor.GREEN));
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getDamageSource().getCausingEntity() instanceof Player source) {
                deathSystem.hit(player, source);
                if (event.getDamageSource().getDamageType() == DamageType.ARROW && player.getHealth() - event.getFinalDamage() > 0) {
                    showHp(source, player, player.getHealth(), event.getFinalDamage());
                }
            }
            if (player.getHealth() - event.getFinalDamage() <= 0) {
                var assists = deathSystem.getAssists(player);
                event.setCancelled(true);
                postRespawn(player);
                player.teleport(selectRespawnLocation(player));
                for (Player assist : assists) {
                    kills.put(assist.getUniqueId(), kills.computeIfAbsent(assist.getUniqueId(), ignored -> 0) + 1);
                    int streak = streaks.computeIfAbsent(assist.getUniqueId(), ignored -> 0) + 1;
                    streaks.put(assist.getUniqueId(), streak);
                    if (longestStreaks.containsKey(assist.getUniqueId())) {
                        longestStreaks.put(assist.getUniqueId(), Math.max(streak, longestStreaks.get(assist.getUniqueId())));
                    } else {
                        longestStreaks.put(assist.getUniqueId(), streak);
                    }
                    assist.playSound(Sound.sound(SoundEventKeys.ENTITY_ARROW_HIT_PLAYER.key(), Sound.Source.MASTER, 1, 1));
                }
                if (redTeam.hasPlayer(player) || blueTeam.hasPlayer(player)) {
                    streaks.remove(player.getUniqueId());
                    map.sendMessage(deathMessage(player, event.getDamageSource().getDamageType(), assists));
                }
            }
        }
    }

    private void showHp(Player source, Player target, double hp, double damage) {
        Component hpComponent = fmtHp(Objects.requireNonNull(target.getAttribute(Attribute.MAX_HEALTH)).getValue(), hp, damage);
        source.showTitle(Title.title(Component.empty(), Component.empty(), 0, 20, 20));
        source.sendActionBar(target.teamDisplayName().append(Component.text(" - ", NamedTextColor.GRAY), hpComponent));
    }

    private Component fmtHp(double maxHp, double hp, double damage) {
        double endHp = hp - damage;
        int endHearts = (int) (endHp / 2);
        int damageHearts = (int) (hp / 2) - endHearts;
        int emptyHearts = ((int) (maxHp / 2)) - (endHearts + damageHearts);
        return Component.text("❤".repeat(endHearts), NamedTextColor.DARK_RED).append(
                Component.text("❤".repeat(damageHearts), NamedTextColor.RED),
                Component.text("❤".repeat(emptyHearts), NamedTextColor.GRAY),
                Component.text(" "),
                Component.translatable("general.paren_hp", NamedTextColor.GREEN, Component.text(String.format("%.1f", endHp)))
        );
    }

    @Override
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.GOLDEN_APPLE) {
            event.setCancelled(true);
            event.getPlayer().addPotionEffect(PotionEffectType.ABSORPTION.createEffect(30 * 20, 0));
            event.getPlayer().setHealth(Optional.ofNullable(event.getPlayer().getAttribute(Attribute.MAX_HEALTH)).map(AttributeInstance::getValue).orElse(20.0));
        }
    }

    @Override
    public void onAsyncChat(AsyncChatEvent event) {
        if (state == State.Ended
                && PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).strip().equalsIgnoreCase("gg")
                && !hasSaidGG.contains(event.getPlayer().getUniqueId())) {
            String rank = plugin.playerData.get(event.getPlayer().getUniqueId(), Ranks.field);
            switch (rank) {
                case "vip" -> Bukkit.getScheduler().runTask(plugin, () -> event.getPlayer().sendMessage(Component.text("+10 Karma!", NamedTextColor.LIGHT_PURPLE)));
                case "vip+" -> Bukkit.getScheduler().runTask(plugin, () -> event.getPlayer().sendMessage(Component.text("+15 Karma!", NamedTextColor.LIGHT_PURPLE)));
                case "mvp" -> Bukkit.getScheduler().runTask(plugin, () -> event.getPlayer().sendMessage(Component.text("+20 Karma!", NamedTextColor.LIGHT_PURPLE)));
                default -> Bukkit.getScheduler().runTask(plugin, () -> event.getPlayer().sendMessage(Component.text("+5 Karma!", NamedTextColor.LIGHT_PURPLE)));
            }

            hasSaidGG.add(event.getPlayer().getUniqueId());
        }
    }

    private Component deathMessage(Player player, DamageType damageType, List<Player> assists) {
        Component killer = HumanReadableList.of(assists.stream().map(this::fmtPlayerWithStreak).toList()).color(NamedTextColor.GRAY);

        if (!assists.isEmpty()) {
            return Component.translatable(deathMessageId(damageType, true), NamedTextColor.GRAY, player.teamDisplayName(), killer);
        } else {
            return Component.translatable(deathMessageId(damageType, false), NamedTextColor.GRAY, player.teamDisplayName());
        }
    }

    private static @NotNull String deathMessageId(DamageType damageType, boolean player) {
        String message = "the_bridge.death.other";
        if (damageType == DamageType.OUT_OF_WORLD) {
            if (player) {
                message = "the_bridge.death.out_of_world.player";
            } else {
                message = "the_bridge.death.out_of_world.self";
            }
        } else if (damageType == DamageType.PLAYER_ATTACK) {
            message = "the_bridge.death.attack.player";
        } else if (damageType == DamageType.ARROW) {
            if (player) {
                message = "the_bridge.death.arrow.player";
            } else {
                message = "the_bridge.death.arrow.self";
            }
        } else if (damageType == DamageType.MACE_SMASH) {
            message = "the_bridge.death.mace_smash.player";
        }
        return message;
    }

    private Component fmtPlayerWithStreak(Player player) {
        int streak = streaks.getOrDefault(player.getUniqueId(), 0);
        if (streak > 3) {
            return player.teamDisplayName().append(Component.translatable("the_bridge.kill_streak", NamedTextColor.YELLOW, Component.text(streak)).decorate(TextDecoration.BOLD));
        } else {
            return player.teamDisplayName();
        }
    }

    @Override
    public void tick() {
        if (currentCountdown != null) {
            currentCountdown.tick();
        }
    }

    private Location spawnLocation(boolean blueTeam) {
        if (blueTeam) {
            return facing(data.blueSpawn().center(map), data.center().center(map));
        } else {
            return facing(data.redSpawn().center(map), data.center().center(map));
        }
    }

    private Location facing(Location location, Location towards) {
        Location diff = location.clone().subtract(towards);
        if (diff.x() > 0 && Math.abs(diff.x()) > Math.abs(diff.z())) {
            location.setYaw(90);
        } else if (diff.z() > 0 && Math.abs(diff.z()) > Math.abs(diff.x())) {
            location.setYaw(180);
        } else if (diff.x() < 0 && Math.abs(diff.x()) > Math.abs(diff.z())) {
            location.setYaw(-90);
        } else if (diff.z() < 0 && Math.abs(diff.z()) > Math.abs(diff.x())) {
            location.setYaw(0);
        }
        return location;
    }

    private boolean isInBuildableRegion(IVec3 loc) {
        IVec3 center = data.center();
        int limit = data.buildLimit();
        IVec3 start = new IVec3(center.x - limit, center.y - 11, center.z - limit);
        IVec3 end = new IVec3(center.x + limit, center.y + 7, center.z + limit);
        return loc.x >= start.x && loc.x <= end.x && loc.y >= start.y && loc.y <= end.y && loc.z >= start.z && loc.z <= end.z;
    }

    private boolean isBridge(IVec3 pos) {
        IVec3 center = data.center();
        int manhattanDist = Math.abs(pos.x - center.x) + Math.abs(pos.z - center.z);
        return manhattanDist <= 20 && (pos.x == center.x || pos.z == center.z);
    }

    private void removeCage(IVec3 spawnPos) {
        WorldTools.clear(map,
                spawnPos.x - 4, spawnPos.y - 2, spawnPos.z - 4,
                9, 6, 9
        );
    }

    private void placeCage(Structure cage, IVec3 spawnPos) {
        cage.place(
                new Location(map, spawnPos.x - 4, spawnPos.y - 2, spawnPos.z - 4),
                false,
                StructureRotation.NONE,
                Mirror.NONE,
                0,
                1.0f,
                new Random()
        );
    }

    private void buildGoal(IVec3 pos, boolean blueTeam) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                Block block = map.getBlockAt(pos.x + dx, pos.y - 1, pos.z + dz);
                Material type = block.getType();
                if (type == Material.SPONGE) {
                    block.setType(Material.END_PORTAL, false);
                }
            }
        }

        Location goalTextPos = pos.center(map);
        goalTextPos.add(0, 3, 0);
        NamedTextColor color = blueTeam ? NamedTextColor.BLUE : NamedTextColor.RED;
        TextDisplay ownGoalText = map.spawn(goalTextPos, TextDisplay.class, entity -> {
            entity.text(Component.translatable("the_bridge.goal_display.own.1", color).append(
                    Component.newline(),
                    Component.translatable("the_bridge.goal_display.own.2").decorate(TextDecoration.ITALIC)));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setVisibleByDefault(false);
        });
        (blueTeam ? visibleToBlue : visibleToRed).add(ownGoalText);
        TextDisplay oppGoalText = map.spawn(goalTextPos, TextDisplay.class, entity -> {
            entity.text(Component.translatable("the_bridge.goal_display.opp.1", color).append(
                    Component.newline(),
                    Component.translatable("the_bridge.goal_display.opp.2").decorate(TextDecoration.ITALIC)));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setVisibleByDefault(false);
        });
        (blueTeam ? visibleToRed : visibleToBlue).add(oppGoalText);
        TextDisplay spectatorGoalText = map.spawn(goalTextPos, TextDisplay.class, entity -> {
            if (blueTeam) {
                entity.text(Component.translatable("the_bridge.goal_display.spectator.blue", color));
            } else {
                entity.text(Component.translatable("the_bridge.goal_display.spectator.red", color));
            }
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setVisibleByDefault(false);
        });
        visibleToSpectators.add(spectatorGoalText);
    }
}
