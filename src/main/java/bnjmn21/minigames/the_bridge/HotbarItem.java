package bnjmn21.minigames.the_bridge;

import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.framework.PlayerDataField;
import com.google.gson.reflect.TypeToken;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public enum HotbarItem {
    Sword(Material.IRON_SWORD),
    Blocks(Material.TERRACOTTA),
    Bow(Material.BOW),
    Apples(Material.GOLDEN_APPLE),
    Pickaxe(Material.DIAMOND_PICKAXE);

    private final Material material;

    public static final PlayerDataField<Hotbar> hotbarField = new PlayerDataField<>(
            Minigames.ns("the_bridge/hotbar"), new TypeToken<>() {}, Hotbar::new
    );

    HotbarItem(Material material) {
        this.material = material;
    }

    public ItemStack create(boolean blue) {
        return switch (this) {
            case Sword -> {
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
                yield sword;
            }
            case Blocks -> new ItemStack(blue ? Material.BLUE_TERRACOTTA : Material.RED_TERRACOTTA, 64);
            case Bow -> {
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addEnchantment(Enchantment.INFINITY, 1);
                bow.editMeta(meta -> meta.setUnbreakable(true));
                bow.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(2.5f).cooldownGroup(Key.key("bow")).build());
                yield bow;
            }
            case Apples -> new ItemStack(Material.GOLDEN_APPLE, 8);
            case Pickaxe -> {
                ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
                pickaxe.addEnchantment(Enchantment.EFFICIENCY, 2);
                pickaxe.editMeta(meta -> meta.setUnbreakable(true));
                yield pickaxe;
            }
        };
    }

    private ItemStack createEditor() {
        return new ItemStack(material);
    }

    public static class Editor implements InventoryHolder {
        public Hotbar hotbar;
        public UUID player;
        private final Runnable onClose;
        private final Inventory inventory;
        private final Minigames plugin;
        private boolean closed = false;

        public Editor(Player player, Minigames plugin, Runnable onClose) {
            this.hotbar = plugin.playerData.get(player.getUniqueId(), hotbarField);
            this.player = player.getUniqueId();
            this.inventory = plugin.getServer().createInventory(this, 18, Component.text("Hotbar editor"));
            hotbar.applyEditor(this.inventory);
            ItemStack closeButton = new ItemStack(Material.NETHER_STAR);
            closeButton.editMeta(meta -> meta.displayName(Component.text("Save layout", NamedTextColor.GREEN)));
            this.inventory.setItem(13, closeButton);
            this.onClose = onClose;
            this.plugin = plugin;
            player.openInventory(this.inventory);
        }

        public final void onInventoryClick(InventoryClickEvent event) {
            Inventory inv = event.getInventory();
            if (inv.getHolder() != this) {
                return;
            }

            Inventory clickedInv = event.getClickedInventory();
            if (clickedInv == null) {
                event.setCancelled(true);
                return;
            } else if (clickedInv.getHolder() != this) {
                event.setCancelled(true);
                return;
            }

            ItemStack currentItem = event.getCurrentItem();
            if (currentItem != null && currentItem.getType() == Material.NETHER_STAR) {
                if (!event.getCursor().isEmpty()) {
                    event.setCancelled(true);
                    return;
                }
                close(event.getWhoClicked());
                return;
            }

            InventoryAction action = event.getAction();
            if (action == InventoryAction.PICKUP_ALL
                    || action == InventoryAction.PICKUP_HALF
                    || action == InventoryAction.PICKUP_SOME
                    || action == InventoryAction.PICKUP_ONE) {
                if (event.getSlot() < 9) {
                    return;
                }
            } else if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_SOME || action == InventoryAction.PLACE_ONE) {
                if (event.getSlot() < 9) {
                    Bukkit.getScheduler().runTask(plugin, () -> this.hotbar = getHotbar(event.getInventory()));
                    return;
                }
            }

            event.setCancelled(true);
        }

        private void close(HumanEntity by) {
            closed = true;
            by.closeInventory();
            plugin.playerData.set(player, hotbarField, hotbar);
            by.sendMessage(Component.text("Hotbar layout saved!", NamedTextColor.GREEN));
            onClose.run();

        }

        public final void onPlayerDropItem(PlayerDropItemEvent event) {
            if (event.getPlayer().getUniqueId() == player) {
                event.setCancelled(true);
            }
        }

        public final void onInventoryClose(InventoryCloseEvent event) {
            if (!closed) {
                close(event.getPlayer());
            }
        }

        public static Hotbar getHotbar(Inventory inv) {
            int sword = inv.first(HotbarItem.Sword.material);
            int blocks = inv.first(HotbarItem.Blocks.material);
            int bow = inv.first(HotbarItem.Bow.material);
            int apples = inv.first(HotbarItem.Apples.material);
            int pickaxe = inv.first(HotbarItem.Pickaxe.material);
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            int blocks2 = inv.all(HotbarItem.Blocks.material).entrySet().stream().max(Comparator.comparingInt(Map.Entry::getKey)).get().getKey();
            return new Hotbar(sword, blocks, bow, apples, pickaxe, blocks2);
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public record Hotbar(int sword, int blocks, int bow, int apples, int pickaxe, int blocks2) {
        public Hotbar() {
            this(0, 1, 2, 3, 6, 7);
        }

        public void apply(Inventory inv, boolean blue) {
            inv.setItem(sword, HotbarItem.Sword.create(blue));
            inv.setItem(blocks, HotbarItem.Blocks.create(blue));
            inv.setItem(bow, HotbarItem.Bow.create(blue));
            inv.setItem(apples, HotbarItem.Apples.create(blue));
            inv.setItem(pickaxe, HotbarItem.Pickaxe.create(blue));
            inv.setItem(blocks2, HotbarItem.Blocks.create(blue));
        }

        void applyEditor(Inventory inv) {
            inv.setItem(sword, HotbarItem.Sword.createEditor());
            inv.setItem(blocks, HotbarItem.Blocks.createEditor());
            inv.setItem(bow, HotbarItem.Bow.createEditor());
            inv.setItem(apples, HotbarItem.Apples.createEditor());
            inv.setItem(pickaxe, HotbarItem.Pickaxe.createEditor());
            inv.setItem(blocks2, HotbarItem.Blocks.createEditor());
        }
    }
}
