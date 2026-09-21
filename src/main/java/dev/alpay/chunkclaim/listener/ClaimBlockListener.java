package dev.alpay.chunkclaim.listener;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Yönetim bloğu: yerleştirme → claim oluştur, sağ tık → menü, kırma → claim sil (onaylı). */
public class ClaimBlockListener implements Listener {

    private final ChunkClaimPlugin plugin;

    public ClaimBlockListener(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    /** 1. faz: doğrulama. Başarısızsa yerleştirme iptal edilir. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceValidate(BlockPlaceEvent event) {
        if (!plugin.claimBlockItem().isClaimBlock(event.getItemInHand())) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("chunkclaim.use")) {
            plugin.messages().send(player, "no-permission");
            event.setCancelled(true);
            return;
        }
        if (!plugin.claims().canCreateClaim(player, event.getBlock().getLocation())) event.setCancelled(true);
    }

    /** 2. faz: başka hiçbir eklenti iptal etmediyse claim'i oluştur. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceCreate(BlockPlaceEvent event) {
        if (!plugin.claimBlockItem().isClaimBlock(event.getItemInHand())) return;
        if (!event.canBuild()) return;
        plugin.claims().createClaim(event.getPlayer(), event.getBlock().getLocation());
    }

    /** Yönetim bloğu kırılamaz; claim silme yalnızca menüdeki onaylı akışla yapılır. */
    @EventHandler(priority = EventPriority.LOW)
    public void onBreak(BlockBreakEvent event) {
        Claim claim = plugin.claims().getClaimByBlock(event.getBlock().getLocation());
        if (claim == null) return;
        event.setCancelled(true);
        plugin.messages().send(event.getPlayer(),
                plugin.claims().canManage(event.getPlayer(), claim) ? "claim-block-use-menu" : "cannot-break-claim-block");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Claim claim = plugin.claims().getClaimByBlock(block.getLocation());
        if (claim == null) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        // Pusula ile sağ tık → claim pusulası (eğilerek de olsa; vanilla lodestone bağlamasının yerine geçer)
        if (hand.getType() == Material.COMPASS && plugin.settings().compassEnabled()) {
            event.setCancelled(true);
            plugin.compass().bind(player, claim, hand);
            return;
        }
        if (player.isSneaking()) return; // eğilerek üstüne blok koymaya izin ver
        event.setCancelled(true);
        plugin.menus().openMain(player, claim);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (plugin.claims().getClaimByBlock(b.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (plugin.claims().getClaimByBlock(b.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
