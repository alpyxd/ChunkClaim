package dev.alpay.chunkclaim.listener;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.ClaimFlag;
import dev.alpay.chunkclaim.claim.UpgradeType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Claim içi koruma kuralları. Flag'ler ziyaretçiler için geçerlidir; üyeler ve sahip her zaman serbesttir. */
public class ProtectionListener implements Listener {

    private static final Set<Material> INTERACTABLE = EnumSet.of(
            Material.LEVER, Material.ENCHANTING_TABLE, Material.NOTE_BLOCK, Material.JUKEBOX, Material.CAKE,
            Material.REPEATER, Material.COMPARATOR, Material.DAYLIGHT_DETECTOR, Material.LECTERN, Material.LOOM,
            Material.STONECUTTER, Material.GRINDSTONE, Material.CARTOGRAPHY_TABLE, Material.SMITHING_TABLE,
            Material.BELL, Material.RESPAWN_ANCHOR, Material.DRAGON_EGG, Material.COMPOSTER, Material.BEEHIVE,
            Material.BEE_NEST, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.TNT, Material.CRAFTING_TABLE,
            Material.CHISELED_BOOKSHELF, Material.DECORATED_POT, Material.CRAFTER, Material.VAULT, Material.TRIAL_SPAWNER,
            Material.TRIPWIRE, Material.END_PORTAL_FRAME, Material.COMMAND_BLOCK, Material.STRUCTURE_BLOCK,
            Material.SWEET_BERRY_BUSH, Material.CAVE_VINES, Material.CAVE_VINES_PLANT, Material.PUMPKIN,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.FARMLAND, Material.TURTLE_EGG
    );

    private static final Set<Material> USE_ITEMS = EnumSet.of(
            Material.FLINT_AND_STEEL, Material.FIRE_CHARGE, Material.BONE_MEAL, Material.ARMOR_STAND,
            Material.END_CRYSTAL, Material.MINECART, Material.CHEST_MINECART, Material.FURNACE_MINECART,
            Material.HOPPER_MINECART, Material.TNT_MINECART, Material.COMMAND_BLOCK_MINECART, Material.ITEM_FRAME,
            Material.GLOW_ITEM_FRAME, Material.PAINTING, Material.LEAD, Material.NAME_TAG, Material.SHEARS,
            Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.POWDER_SNOW_BUCKET, Material.BUCKET,
            Material.AXOLOTL_BUCKET, Material.COD_BUCKET, Material.SALMON_BUCKET, Material.PUFFERFISH_BUCKET,
            Material.TROPICAL_FISH_BUCKET, Material.TADPOLE_BUCKET, Material.GLASS_BOTTLE, Material.HONEYCOMB,
            Material.BRUSH
    );

    private final ChunkClaimPlugin plugin;

    public ProtectionListener(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- Yardımcılar ----------

    /** Oyuncu bu konumda verilen eylemi yapabilir mi? */
    private boolean allowed(Player player, Location loc, ClaimFlag flag) {
        Claim claim = plugin.claims().getClaimAt(loc);
        if (claim == null) return true;
        if (plugin.claims().canBypass(player)) return true;
        return claim.canDo(player.getUniqueId(), flag);
    }

    private boolean deny(Player player, Location loc, ClaimFlag flag, String messageKey) {
        if (allowed(player, loc, flag)) return false;
        plugin.messages().send(player, messageKey);
        return true;
    }

    private Player playerFrom(Entity entity) {
        if (entity instanceof Player p) return p;
        if (entity instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        return null;
    }

    private boolean isSpawnEgg(Material m) {
        return m.name().endsWith("_SPAWN_EGG");
    }

    private boolean isBoat(Material m) {
        String n = m.name();
        return n.endsWith("_BOAT") || n.endsWith("_RAFT");
    }

    private boolean isInteractable(Block block) {
        Material m = block.getType();
        if (INTERACTABLE.contains(m)) return true;
        return Tag.DOORS.isTagged(m) || Tag.TRAPDOORS.isTagged(m) || Tag.FENCE_GATES.isTagged(m)
                || Tag.BUTTONS.isTagged(m) || Tag.PRESSURE_PLATES.isTagged(m) || Tag.BEDS.isTagged(m)
                || Tag.ANVIL.isTagged(m) || Tag.ALL_SIGNS.isTagged(m) || Tag.FLOWER_POTS.isTagged(m)
                || Tag.CANDLE_CAKES.isTagged(m) || Tag.CAULDRONS.isTagged(m) || Tag.CANDLES.isTagged(m);
    }

    // ---------- Blok koyma / kırma ----------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation(), ClaimFlag.BREAK, "protection-break")) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block block = e.getBlock();
        if (deny(e.getPlayer(), block.getLocation(), ClaimFlag.BUILD, "protection-build")) {
            e.setCancelled(true);
            return;
        }
        // Sınırın dışına sandık koyup içerideki sandıkla birleştirme açığı
        Material m = block.getType();
        if (m == Material.CHEST || m == Material.TRAPPED_CHEST) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block n = block.getRelative(face);
                if (n.getType() != m) continue;
                if (deny(e.getPlayer(), n.getLocation(), ClaimFlag.CONTAINERS, "protection-container")) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation(), ClaimFlag.USE_ITEMS, "protection-use")) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation(), ClaimFlag.USE_ITEMS, "protection-use")) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent e) {
        if (e.getPlayer() == null) return;
        if (deny(e.getPlayer(), e.getEntity().getLocation(), ClaimFlag.BUILD, "protection-build")) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        Player p = playerFrom(e.getRemover());
        if (p == null) {
            // Patlama vb. — claim içindeki tabloları koru
            if (plugin.claims().getClaimAt(e.getEntity().getLocation()) != null) e.setCancelled(true);
            return;
        }
        if (deny(p, e.getEntity().getLocation(), ClaimFlag.BREAK, "protection-break")) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent e) {
        Player p = playerFrom(e.getAttacker());
        if (p == null) return;
        if (deny(p, e.getVehicle().getLocation(), ClaimFlag.BREAK, "protection-break")) e.setCancelled(true);
    }

    // ---------- Etkileşim ----------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();
        Action action = e.getAction();

        if (action == Action.PHYSICAL) {
            if (block != null && deny(player, block.getLocation(), ClaimFlag.INTERACT, "protection-interact")) {
                e.setCancelled(true);
            }
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK && block != null) {
            if (block.getState(false) instanceof Container container) {
                if (deny(player, block.getLocation(), ClaimFlag.CONTAINERS, "protection-container")) {
                    e.setCancelled(true);
                    return;
                }
                // Çift sandık: diğer yarı başka bir claim'de olabilir (sınıra dışarıdan sandık ekleme açığı)
                if (container.getInventory().getHolder(false) instanceof DoubleChest dc) {
                    for (InventoryHolder half : new InventoryHolder[]{dc.getLeftSide(false), dc.getRightSide(false)}) {
                        if (half instanceof Chest c && deny(player, c.getLocation(), ClaimFlag.CONTAINERS, "protection-container")) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            } else if (isInteractable(block)) {
                if (deny(player, block.getLocation(), ClaimFlag.INTERACT, "protection-interact")) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        // Elindeki eşyanın kullanımı (blok üzerine veya havaya)
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            ItemStack item = e.getItem();
            if (item == null) return;
            Material m = item.getType();
            if (USE_ITEMS.contains(m) || isSpawnEgg(m) || isBoat(m)) {
                Location target = block != null ? block.getRelative(e.getBlockFace()).getLocation() : player.getLocation();
                if (deny(player, target, ClaimFlag.USE_ITEMS, "protection-use")) {
                    e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND && !(e instanceof PlayerInteractAtEntityEvent)) return;
        Entity target = e.getRightClicked();
        if (target instanceof Player) return;
        if (deny(e.getPlayer(), target.getLocation(), ClaimFlag.ENTITY_INTERACT, "protection-interact")) e.setCancelled(true);
    }

    /** Kürsüden kitap alma (kürsü GUI'si INTERACT ile açılabilir ama kitap alma sandık erişimi sayılır). */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLecternTake(PlayerTakeLecternBookEvent e) {
        if (deny(e.getPlayer(), e.getLectern().getLocation(), ClaimFlag.CONTAINERS, "protection-container")) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (deny(e.getPlayer(), e.getRightClicked().getLocation(), ClaimFlag.ENTITY_INTERACT, "protection-interact")) e.setCancelled(true);
    }

    // ---------- Hasar ----------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        Player attacker = playerFrom(e.getDamager());
        Claim claim = plugin.claims().getClaimAt(victim.getLocation());
        if (claim == null) return;

        if (victim instanceof Player) {
            if (attacker == null || attacker == victim) return;
            if (plugin.claims().canBypass(attacker)) return;
            if (!claim.getFlag(ClaimFlag.PVP)) {
                plugin.messages().send(attacker, "protection-pvp");
                e.setCancelled(true);
            }
            return;
        }

        if (attacker == null) {
            // Patlama / mob hasarı ile tablo, zırh askısı vb. kırılmasın
            if ((victim instanceof ArmorStand || victim instanceof Hanging) && !(e.getDamager() instanceof Player)) {
                e.setCancelled(true);
            }
            return;
        }
        if (victim instanceof ArmorStand || victim instanceof Hanging || victim instanceof Vehicle) {
            if (deny(attacker, victim.getLocation(), ClaimFlag.BREAK, "protection-break")) e.setCancelled(true);
            return;
        }
        boolean passive = victim instanceof LivingEntity && !(victim instanceof Enemy);
        if (passive && deny(attacker, victim.getLocation(), ClaimFlag.ANIMAL_DAMAGE, "protection-animal")) {
            e.setCancelled(true);
        }
    }

    // ---------- Eşya ----------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!allowed(p, e.getItem().getLocation(), ClaimFlag.ITEM_PICKUP)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (deny(e.getPlayer(), e.getPlayer().getLocation(), ClaimFlag.ITEM_DROP, "protection-drop")) e.setCancelled(true);
    }

    // ---------- Patlama / yangın / canavar ----------

    private void filterExplosion(List<Block> blocks) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block b = it.next();
            Claim claim = plugin.claims().getClaimAt(b.getLocation());
            if (claim == null) continue;
            if (plugin.claims().getClaimByBlock(b.getLocation()) != null
                    || plugin.claims().hasUpgrade(claim, UpgradeType.EXPLOSION_PROTECTION)) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        filterExplosion(e.blockList());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        filterExplosion(e.blockList());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        Claim claim = plugin.claims().getClaimAt(e.getBlock().getLocation());
        if (claim == null) return;
        if (plugin.claims().getClaimByBlock(e.getBlock().getLocation()) != null
                || plugin.claims().hasUpgrade(claim, UpgradeType.FIRE_PROTECTION)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        Claim claim = plugin.claims().getClaimAt(e.getBlock().getLocation());
        if (claim == null) return;
        // Çakmak, ateş topu veya alev oku: kaynağı oyuncuysa onun iznine bak
        Player igniter = e.getPlayer() != null ? e.getPlayer() : playerFrom(e.getIgnitingEntity());
        if (igniter != null) {
            if (deny(igniter, e.getBlock().getLocation(), ClaimFlag.USE_ITEMS, "protection-use")) e.setCancelled(true);
            return;
        }
        if (plugin.claims().hasUpgrade(claim, UpgradeType.FIRE_PROTECTION)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (e.getSource().getType() != Material.FIRE && e.getSource().getType() != Material.SOUL_FIRE) return;
        Claim claim = plugin.claims().getClaimAt(e.getBlock().getLocation());
        if (claim != null && plugin.claims().hasUpgrade(claim, UpgradeType.FIRE_PROTECTION)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Enemy)) return;
        switch (e.getSpawnReason()) {
            case NATURAL, JOCKEY, PATROL, REINFORCEMENTS, VILLAGE_INVASION -> {
                Claim claim = plugin.claims().getClaimAt(e.getLocation());
                if (claim != null && plugin.claims().hasUpgrade(claim, UpgradeType.MOB_SPAWN_BLOCK)) e.setCancelled(true);
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        Entity en = e.getEntity();
        if (en instanceof Enderman || en instanceof Wither || en instanceof Ravager || en instanceof Silverfish) {
            if (plugin.claims().getClaimAt(e.getBlock().getLocation()) != null) e.setCancelled(true);
        }
    }

    // ---------- Piston / sıvı: claim sınırından geçiş ----------

    private boolean crossesBorder(Block piston, List<Block> blocks, org.bukkit.block.BlockFace direction) {
        Claim source = plugin.claims().getClaimAt(piston.getLocation());
        for (Block b : blocks) {
            Claim from = plugin.claims().getClaimAt(b.getLocation());
            Claim to = plugin.claims().getClaimAt(b.getRelative(direction).getLocation());
            if (!same(from, source) || !same(to, source)) return true;
        }
        return false;
    }

    private static boolean same(Claim a, Claim b) {
        if (a == null) return b == null;
        return b != null && a.getId().equals(b.getId());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (crossesBorder(e.getBlock(), e.getBlocks(), e.getDirection())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (crossesBorder(e.getBlock(), e.getBlocks(), e.getDirection())) e.setCancelled(true);
    }

    /** Sınır dışındaki dispenser'dan claim içine lav/su/ateş/spawn egg püskürtme. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        Material m = e.getItem().getType();
        if (!USE_ITEMS.contains(m) && !isSpawnEgg(m)) return;
        Block dispenser = e.getBlock();
        if (!(dispenser.getBlockData() instanceof Directional dir)) return;
        Block target = dispenser.getRelative(dir.getFacing());
        Claim from = plugin.claims().getClaimAt(dispenser.getLocation());
        Claim to = plugin.claims().getClaimAt(target.getLocation());
        if (to != null && !same(from, to)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFromTo(BlockFromToEvent e) {
        Claim from = plugin.claims().getClaimAt(e.getBlock().getLocation());
        Claim to = plugin.claims().getClaimAt(e.getToBlock().getLocation());
        if (to != null && !same(from, to)) e.setCancelled(true);
    }
}
