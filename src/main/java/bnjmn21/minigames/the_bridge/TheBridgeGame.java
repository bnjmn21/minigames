package bnjmn21.minigames.the_bridge;

import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.data.data_types.IVec3;
import bnjmn21.minigames.framework.Countdown;
import bnjmn21.minigames.framework.DeathSystem;
import bnjmn21.minigames.framework.GameInstance;
import bnjmn21.minigames.framework.Settings;
import bnjmn21.minigames.maps.BlockSnapshot;
import bnjmn21.minigames.maps.GameMap;
import bnjmn21.minigames.util.LeaveWorldListener;
import bnjmn21.minigames.util.Scoreboards;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.SoundEventKeys;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class TheBridgeGame implements GameInstance {
    private static final int POINTS_TO_WIN = 5;

    private World map;
    private final Component mapName;
    private String gameName;
    private int redPoints = 0;
    private int bluePoints = 0;
    private final ArrayList<Entity> visibleToRed = new ArrayList<>();
    private final ArrayList<Entity> visibleToBlue = new ArrayList<>();
    private final ArrayList<Entity> visibleToSpectators = new ArrayList<>();
    private List<BlockSnapshot> redCage;
    private List<BlockSnapshot> blueCage;
    private Team redTeam;
    private Team blueTeam;
    private Team spectatorTeam;
    private Sidebar sidebar;
    private ComponentSidebarLayout sidebarLayout;
    private TheBridgeMap.Data data;
    private boolean gameStarted = false;
    private boolean gameEnded = false;
    private final DeathSystem deathSystem = new DeathSystem();
    @Nullable private Countdown currentCountdown;
    private Objective hpObjective;
    private final Minigames plugin;

    public TheBridgeGame(Settings settings, Minigames plugin) {
        this.plugin = plugin;
        GameMap map = settings.getMap();
        this.mapName = map.displayName;
        plugin.tempMaps.create(map, world -> {
            this.map = world;
            try {
                this.data = new TheBridgeMap.Data(world.getPersistentDataContainer(), plugin.theBridge.map);
            } catch (NullPointerException e) {
                Bukkit.getServer().sendMessage(Component.text("Failed to load map '", TextColor.color(NamedTextColor.RED)).append(
                        this.mapName,
                        Component.text("'.")
                ));
            }
            buildMap();
            initTeams(settings);
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
        gameStarted = true;
        startRound(Component.empty());
    }

    private void startRound(Component subtitle) {
        map.clearTitle();
        BlockSnapshot.restore(map, redCage);
        BlockSnapshot.restore(map, blueCage);
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
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        sword.editMeta(meta -> meta.setUnbreakable(true));
        sword.setData(DataComponentTypes.BLOCKS_ATTACKS, BlocksAttacks.blocksAttacks()
                .addDamageReduction(DamageReduction.damageReduction().base(0).factor(0.5f).type(RegistrySet.keySet(RegistryKey.DAMAGE_TYPE,
                        RegistryKey.DAMAGE_TYPE.typedKey(DamageType.PLAYER_ATTACK.key())
                )).build()).build());
        sword.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes()
                .addModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(NamespacedKey.minecraft("the_bridge_atk_dmg"), 5.0f, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND))
                .addModifier(Attribute.ATTACK_SPEED, new AttributeModifier(NamespacedKey.minecraft("the_bridge_atk_spd"), 100.0f, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND))
                .build());
        inv.setItem(0, sword);
        inv.setItem(1, new ItemStack(blue ? Material.BLUE_TERRACOTTA : Material.RED_TERRACOTTA, 64));
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.INFINITY, 1);
        bow.editMeta(meta -> meta.setUnbreakable(true));
        bow.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(2.5f).cooldownGroup(Key.key("bow")).build());
        inv.setItem(2, bow);
        inv.setItem(3, new ItemStack(Material.GOLDEN_APPLE, 8));
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        pickaxe.addEnchantment(Enchantment.EFFICIENCY, 2);
        pickaxe.editMeta(meta -> meta.setUnbreakable(true));
        inv.setItem(4, pickaxe);
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
        redCage = removeCage(data.redSpawn());
        blueCage = removeCage(data.blueSpawn());
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
                redTeam.addPlayer(player);
                Minigames.resetPlayer(player, GameMode.SURVIVAL);
            } else if (playerTeam.getValue().equals(Optional.of(1))) {
                blueTeam.addPlayer(player);
                Minigames.resetPlayer(player, GameMode.SURVIVAL);
            } else {
                spectatorTeam.addPlayer(player);
                Minigames.resetPlayer(player, GameMode.SPECTATOR);
            }
        }
        gameName = "The Bridge " + redTeam.getSize() + "v" + blueTeam.getSize();
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
                .addStaticLine(Component.text("Map: ", NamedTextColor.GRAY).append(this.mapName.color(NamedTextColor.WHITE)))
                .addBlankLine()
                .addDynamicLine(() -> Component.text("Red: " + "⬤".repeat(redPoints), NamedTextColor.RED).append(Component.text("⬤".repeat(POINTS_TO_WIN - redPoints), NamedTextColor.GRAY)))
                .addDynamicLine(() -> Component.text("Blue: " + "⬤".repeat(bluePoints), NamedTextColor.BLUE).append(Component.text("⬤".repeat(POINTS_TO_WIN - bluePoints), NamedTextColor.GRAY)))
                .build();
        this.sidebarLayout = new ComponentSidebarLayout(
                SidebarComponent.staticLine(Component.text(gameName).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)),
                sidebarComponent
        );
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
        Block block = event.getBlock();
        boolean allowed = isInBuildableRegion(block.getLocation()) && (
                block.getType() == Material.RED_TERRACOTTA
                || block.getType() == Material.RED_CONCRETE
                || block.getType() == Material.WHITE_TERRACOTTA
                || block.getType() == Material.BLUE_TERRACOTTA
                || block.getType() == Material.BLUE_CONCRETE
        );
        event.setCancelled(!allowed);
    }

    @Override
    public World getWorld() {
        return map;
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isInBuildableRegion(event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("You cannot place blocks here", NamedTextColor.RED));
            return;
        }

        if (data.center().y + 7 == event.getBlockPlaced().getLocation().getBlockY()) {
            if (event.getBlockPlaced().getType() == Material.RED_TERRACOTTA) {
                event.getBlockPlaced().setType(Material.RED_CONCRETE);
            } else if (event.getBlockPlaced().getType() == Material.BLUE_TERRACOTTA) {
                event.getBlockPlaced().setType(Material.BLUE_CONCRETE);
            }
        }

        event.getItemInHand().setAmount(64);
    }

    private void winGame(boolean blue) {
        gameEnded = true;
        for (Player player : map.getPlayers()) {
            Minigames.resetPlayer(player, GameMode.SPECTATOR);
            player.teleport(selectRespawnLocation(player));
        }
        if (blue) {
            map.showTitle(Title.title(Component.text("Blue won the game!", NamedTextColor.BLUE), Component.empty(), 10, 100, 10));
        } else {
            map.showTitle(Title.title(Component.text("Red won the game!", NamedTextColor.RED), Component.empty(), 10, 100, 10));
        }
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
        if (!gameStarted || gameEnded) {
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
        if (gameEnded) {
            Minigames.resetPlayer(player, GameMode.SPECTATOR);
            return;
        }

        if (!gameStarted) {
            if (spectatorTeam.hasPlayer(player)) {
                Minigames.resetPlayer(player, GameMode.SPECTATOR);
            } else {
                Minigames.resetPlayer(player, GameMode.ADVENTURE);
            }
            return;
        }

        if (redTeam.hasPlayer(player)) {
            setupPlayer(player, false);
        } else if (blueTeam.hasPlayer(player)) {
            setupPlayer(player, true);
        } else {
            Minigames.resetPlayer(player, GameMode.SPECTATOR);
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().getLocation().getBlockY() < data.center().y - 15) {
            event.getPlayer().damage(9999999, DamageSource.builder(DamageType.OUT_OF_WORLD).build());
            return;
        }

        if (event.getTo().getBlock().getType() != Material.END_PORTAL) {
            return;
        }

        if (redTeam.hasPlayer(event.getPlayer()) && event.getTo().distance(data.blueGoal().center(map)) < 10) {
            redPoints++;
            if (redPoints >= 5) {
                winGame(false);
            } else {
                startRound(event.getPlayer().teamDisplayName().color(NamedTextColor.RED).append(Component.text(" scored!")));
            }
        } else if (blueTeam.hasPlayer(event.getPlayer()) && event.getFrom().distance(data.redGoal().center(map)) < 10) {
            bluePoints++;
            if (bluePoints >= 5) {
                winGame(true);
            } else {
                startRound(event.getPlayer().teamDisplayName().color(NamedTextColor.BLUE).append(Component.text(" scored!")));
            }
        }
        sidebarLayout.apply(sidebar);
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
                if (redTeam.hasPlayer(player) || blueTeam.hasPlayer(player)) {
                    map.sendMessage(deathMessage(player, event.getDamageSource().getDamageType(), assists));
                }
                for (Player assist : assists) {
                    assist.playSound(Sound.sound(SoundEventKeys.ENTITY_ARROW_HIT_PLAYER.key(), Sound.Source.MASTER, 1, 1));
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
                Component.text(String.format(" (%.1f HP)", endHp), NamedTextColor.GREEN)
        );
    }

    @Override
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.GOLDEN_APPLE) {
            event.setCancelled(true);
            event.getPlayer().addPotionEffect(PotionEffectType.ABSORPTION.createEffect(60, 0));
            event.getPlayer().setHealth(Optional.ofNullable(event.getPlayer().getAttribute(Attribute.MAX_HEALTH)).map(AttributeInstance::getValue).orElse(20.0));
        }
    }

    private Component deathMessage(Player player, DamageType damageType, List<Player> assists) {
        Component killer = null;
        if (assists.size() == 1) {
            killer = assists.getFirst().teamDisplayName();
        } else if (assists.size() == 2) {
            killer = assists.getFirst().teamDisplayName().append(gray(" and "), assists.getLast().teamDisplayName());
        } else if (assists.size() > 2) {
            var firstKillers = assists.subList(1, assists.size() - 1).stream()
                    .map(Player::teamDisplayName).toList();
            Component res = assists.getFirst().teamDisplayName();
            for (Component k : firstKillers) {
                res = res.append(gray(", "), k);
            }
            killer = res.append(gray(" and "), assists.getLast().teamDisplayName());
        }

        if (damageType == DamageType.OUT_OF_WORLD) {
            if (killer != null) {
                return player.teamDisplayName().append(gray(" was knocked into the void by "), killer, gray("."));
            } else {
                return player.teamDisplayName().append(gray(" fell into the void."));
            }
        } else if (damageType == DamageType.PLAYER_ATTACK) {
            //noinspection DataFlowIssue
            return player.teamDisplayName().append(gray(" was killed by "), killer, gray("."));
        } else if (damageType == DamageType.ARROW) {
            //noinspection DataFlowIssue
            return player.teamDisplayName().append(gray(" was shot by "), killer, gray("."));
        } else {
            return player.teamDisplayName().append(gray(" died."));
        }
    }

    private Component gray(String text) {
        return Component.text(text, NamedTextColor.GRAY);
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

    private boolean isInBuildableRegion(Location loc) {
        TheBridgeMap fields = plugin.theBridge.map;
        PersistentDataContainer pdc = map.getPersistentDataContainer();
        IVec3 center = fields.center.get(pdc);
        int limit = fields.buildLimit.get(pdc);
        IVec3 start = new IVec3(center.x - limit, center.y - 11, center.z - limit);
        IVec3 end = new IVec3(center.x + limit, center.y + 7, center.z + limit);
        return loc.getBlockX() >= start.x && loc.getBlockX() <= end.x
                && loc.getBlockY() >= start.y && loc.getBlockY() <= end.y
                && loc.getBlockZ() >= start.z && loc.getBlockZ() <= end.z;
    }

    private List<BlockSnapshot> removeCage(IVec3 spawnPos) {
        return BlockSnapshot.copyAndClear(map,
                spawnPos.x - 4, spawnPos.y - 2, spawnPos.z - 4,
                9, 6, 9
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
            entity.text(Component.text("Your Goal\n", color).append(
                    Component.text("Defend it!").decorate(TextDecoration.ITALIC)));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setVisibleByDefault(false);
        });
        (blueTeam ? visibleToBlue : visibleToRed).add(ownGoalText);
        TextDisplay oppGoalText = map.spawn(goalTextPos, TextDisplay.class, entity -> {
            entity.text(Component.text("Opponent's Goal\n", color).append(
                    Component.text("Jump in!").decorate(TextDecoration.ITALIC)));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setVisibleByDefault(false);
        });
        (blueTeam ? visibleToRed : visibleToBlue).add(oppGoalText);
        TextDisplay spectatorGoalText = map.spawn(goalTextPos, TextDisplay.class, entity -> {
            entity.text(Component.text((blueTeam ? "Blue" : "Red") + " Goal", color));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setVisibleByDefault(false);
        });
        visibleToSpectators.add(spectatorGoalText);
        plugin.getLogger().info(visibleToRed.stream().map(Entity::toString).collect(Collectors.joining(", ")));
        plugin.getLogger().info(visibleToBlue.stream().map(Entity::toString).collect(Collectors.joining(", ")));
        plugin.getLogger().info(visibleToSpectators.stream().map(Entity::toString).collect(Collectors.joining(", ")));
    }
}
