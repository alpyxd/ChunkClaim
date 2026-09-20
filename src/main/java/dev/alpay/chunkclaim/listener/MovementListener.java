package dev.alpay.chunkclaim.listener;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.ClaimFlag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Chunk değişimlerini izler: giriş/çıkış mesajı, giriş yasağı, isteğe bağlı sınır gösterimi. */
public class MovementListener implements Listener {

    private static final UUID WILDERNESS = new UUID(0, 0);

    private final ChunkClaimPlugin plugin;
    private final Map<UUID, UUID> lastClaim = new ConcurrentHashMap<>();

    public MovementListener(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) plugin.border().onMove(e.getPlayer(), to);
        if (from.getBlockX() >> 4 == to.getBlockX() >> 4
                && from.getBlockZ() >> 4 == to.getBlockZ() >> 4
                && from.getWorld().equals(to.getWorld())) return;
        if (handle(e.getPlayer(), from, to)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (handle(e.getPlayer(), e.getFrom(), e.getTo())) e.setCancelled(true);
    }

    /** true dönerse hareket iptal edilmeli. */
    private boolean handle(Player player, Location from, Location to) {
        Claim fromClaim = plugin.claims().getClaimAt(from);
        Claim toClaim = plugin.claims().getClaimAt(to);
        if (fromClaim == toClaim) return false;
        if (fromClaim != null && toClaim != null && fromClaim.getId().equals(toClaim.getId())) return false;

        if (toClaim != null && !toClaim.getFlag(ClaimFlag.ENTER)
                && !toClaim.isTrusted(player.getUniqueId()) && !plugin.claims().canBypass(player)) {
            plugin.messages().send(player, "protection-enter");
            return true;
        }

        UUID prev = lastClaim.get(player.getUniqueId());
        UUID next = toClaim == null ? WILDERNESS : toClaim.getId();
        if (next.equals(prev)) return false;
        lastClaim.put(player.getUniqueId(), next);

        if (plugin.settings().titlesEnabled()) {
            if (toClaim != null) {
                player.sendActionBar(plugin.messages().plain("titles.enter", plugin.claims().placeholders(toClaim)));
            } else if (fromClaim != null) {
                player.sendActionBar(plugin.messages().plain("titles.leave", plugin.claims().placeholders(fromClaim)));
            }
        }
        if (toClaim != null && plugin.settings().borderShowOnEnter()) {
            plugin.border().show(player, toClaim);
        }
        return false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastClaim.remove(e.getPlayer().getUniqueId());
        plugin.border().cancel(e.getPlayer());
    }
}
