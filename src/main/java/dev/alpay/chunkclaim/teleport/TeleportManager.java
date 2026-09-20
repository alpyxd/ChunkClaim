package dev.alpay.chunkclaim.teleport;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.ChunkKey;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.MemberPermission;
import dev.alpay.chunkclaim.config.Messages;
import dev.alpay.chunkclaim.economy.EconomyProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Claim'e ışınlanma: bekleme süresi, hareket/hasar iptali, cooldown, ücret. */
public class TeleportManager implements Listener {

    private record Pending(Claim claim, Location start, BukkitTask task) { }

    private final ChunkClaimPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public TeleportManager(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    public void teleport(Player player, Claim claim) {
        Messages m = plugin.messages();
        if (!plugin.claims().require(player, claim, MemberPermission.TELEPORT)) return;

        if (!plugin.claims().exists(claim)) return;
        Location target = claim.getHome();
        if (target == null) {
            m.send(player, "teleport-no-home");
            return;
        }
        if (pending.containsKey(player.getUniqueId())) {
            m.send(player, "teleport-already-pending");
            return;
        }
        int cooldown = plugin.settings().teleportCooldown();
        if (cooldown > 0 && !player.hasPermission("chunkclaim.admin")) {
            Long last = cooldowns.get(player.getUniqueId());
            if (last != null) {
                long left = (last + cooldown * 1000L - System.currentTimeMillis()) / 1000;
                if (left > 0) {
                    m.send(player, "teleport-cooldown", Map.of("seconds", String.valueOf(left)));
                    return;
                }
            }
        }

        int warmup = plugin.settings().teleportWarmup();
        if (warmup <= 0) {
            doTeleport(player, claim, target);
            return;
        }
        m.send(player, "teleport-warmup", Map.of("seconds", String.valueOf(warmup), "name", claim.getName()));
        final int[] remaining = {warmup};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancel(player, false);
                return;
            }
            remaining[0]--;
            if (remaining[0] <= 0) {
                Pending p = pending.remove(player.getUniqueId());
                if (p != null) p.task().cancel();
                if (!plugin.claims().exists(claim)) return; // bekleme sırasında silindi
                doTeleport(player, claim, claim.getHome() == null ? target : claim.getHome());
            } else {
                player.sendActionBar(Messages.parse(m.raw("teleport-countdown"), Map.of("seconds", String.valueOf(remaining[0]))));
            }
        }, 20L, 20L);
        pending.put(player.getUniqueId(), new Pending(claim, player.getLocation().clone(), task));
        player.sendActionBar(Messages.parse(m.raw("teleport-countdown"), Map.of("seconds", String.valueOf(warmup))));
    }

    private void doTeleport(Player player, Claim claim, Location target) {
        Messages m = plugin.messages();
        EconomyProvider eco = plugin.economy().provider();
        double cost = player.hasPermission("chunkclaim.admin") ? 0 : plugin.settings().teleportCost(eco.usesDiamonds());
        if (cost > 0 && !eco.withdraw(player, cost)) {
            m.send(player, "not-enough-money", Map.of("cost", eco.format(cost), "balance", eco.format(eco.getBalance(player))));
            return;
        }
        Location safe = findSafe(target);
        double charged = cost;
        player.teleportAsync(safe).thenAccept(ok -> {
            if (!ok) {
                if (charged > 0 && player.isOnline()) eco.deposit(player, charged);
                m.send(player, "teleport-failed");
                return;
            }
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            m.send(player, "teleport-done", Map.of("name", claim.getName()));
            player.sendActionBar(Component.empty());
        });
    }

    /** Hedefin üstü kapalıysa yukarı doğru boş bir yer arar (en fazla 10 blok). */
    private Location findSafe(Location loc) {
        Location l = loc.clone();
        for (int i = 0; i < 10; i++) {
            Material feet = l.getBlock().getType();
            Material head = l.clone().add(0, 1, 0).getBlock().getType();
            if (!feet.isSolid() && !head.isSolid()) return l;
            l.add(0, 1, 0);
        }
        return loc;
    }

    public void cancel(Player player, boolean notify) {
        Pending p = pending.remove(player.getUniqueId());
        if (p == null) return;
        p.task().cancel();
        if (notify && player.isOnline()) plugin.messages().send(player, "teleport-cancelled");
    }

    /** Ev noktası ayarlama (claim içinde olmalı). */
    public boolean setHome(Player player, Claim claim) {
        if (!plugin.claims().exists(claim)) return false;
        if (!plugin.claims().require(player, claim, MemberPermission.SET_HOME)) return false;
        Location loc = player.getLocation();
        if (!claim.hasChunk(ChunkKey.of(loc))) {
            plugin.messages().send(player, "home-outside-claim");
            return false;
        }
        claim.setHome(loc);
        plugin.claims().save(claim);
        plugin.messages().send(player, "home-set");
        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!plugin.settings().teleportCancelOnMove()) return;
        Pending p = pending.get(e.getPlayer().getUniqueId());
        if (p == null) return;
        Location to = e.getTo();
        Location s = p.start();
        if (to.getBlockX() != s.getBlockX() || to.getBlockY() != s.getBlockY() || to.getBlockZ() != s.getBlockZ()) {
            cancel(e.getPlayer(), true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!plugin.settings().teleportCancelOnDamage()) return;
        if (e.getEntity() instanceof Player p && pending.containsKey(p.getUniqueId())) cancel(p, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancel(e.getPlayer(), false);
    }

    public void shutdown() {
        pending.values().forEach(p -> p.task().cancel());
        pending.clear();
    }
}
