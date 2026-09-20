package dev.alpay.chunkclaim.border;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.ChunkKey;
import dev.alpay.chunkclaim.claim.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Claim alanını istemci tarafı (sanal) dünya sınırı ile gösterir.
 * Sınır, ana chunk'ın merkezinden başlayıp tüm chunkları kapsayacak kareye
 * yumuşak şekilde genişler, bir süre bekler, sonra küçülerek kaybolur.
 */
public class BorderVisualizer {

    /** Oyuncuya gösterilen aktif sınır: merkez ve hedef boyut (kenar mesafesi hesabı için). */
    private record Active(WorldBorder border, double cx, double cz, long readyAt) { }

    private final ChunkClaimPlugin plugin;
    private final Map<UUID, BukkitTask> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Active> active = new ConcurrentHashMap<>();

    public BorderVisualizer(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Her harekette çağrılır. İstemci tarafı sınır gerçek bir duvar olduğundan, oyuncu
     * kenara yaklaşınca önizlemeyi kaldırır ki içeri/dışarı geçişi engellemesin.
     */
    public void onMove(Player player, Location to) {
        Active a = active.get(player.getUniqueId());
        if (a == null) return;
        double dist = plugin.settings().borderAutoHideDistance();
        if (dist <= 0) return;
        // Genişleme bitmeden kenar oyuncunun yanından geçer; o sırada tetiklenmesin
        if (System.currentTimeMillis() < a.readyAt()) return;
        double half = a.border().getSize() / 2.0;
        double dx = half - Math.abs(to.getX() - a.cx());
        double dz = half - Math.abs(to.getZ() - a.cz());
        // Negatif = dışarıda; mutlak değer kenara uzaklık
        if (Math.abs(dx) <= dist || Math.abs(dz) <= dist) cancel(player);
    }

    public void show(Player player, Claim claim) {
        if (!player.getWorld().getUID().equals(claim.getOriginChunk().world())) return;
        cancel(player);

        ChunkKey origin = claim.getOriginChunk();
        double cx = origin.x() * 16 + 8.0;
        double cz = origin.z() * 16 + 8.0;

        // Merkezden en uzak chunk kenarına olan mesafe → kare sınır yarı-genişliği
        double half = 0;
        for (ChunkKey c : claim.getChunks()) {
            double minX = c.x() * 16.0, maxX = minX + 16;
            double minZ = c.z() * 16.0, maxZ = minZ + 16;
            half = Math.max(half, Math.max(Math.abs(minX - cx), Math.abs(maxX - cx)));
            half = Math.max(half, Math.max(Math.abs(minZ - cz), Math.abs(maxZ - cz)));
        }
        double targetSize = Math.max(2.0, half * 2);

        long expand = Math.max(0, plugin.settings().borderExpandMs());
        long hold = Math.max(0, plugin.settings().borderHoldMs());
        long shrink = Math.max(0, plugin.settings().borderShrinkMs());

        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(cx, cz);
        border.setSize(1.0);
        border.setWarningDistance(0);
        border.setWarningTimeTicks(0);
        border.setDamageAmount(0);
        border.setDamageBuffer(0);
        player.setWorldBorder(border);
        border.changeSize(targetSize, msToTicks(expand));
        active.put(player.getUniqueId(), new Active(border, cx, cz, System.currentTimeMillis() + expand + 100));

        long shrinkAtTicks = msToTicks(expand + hold);
        long clearAtTicks = shrinkAtTicks + msToTicks(shrink) + 2;

        BukkitTask shrinkTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (player.getWorldBorder() == border) border.changeSize(1.0, msToTicks(shrink));
            BukkitTask clearTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                pending.remove(player.getUniqueId());
                active.remove(player.getUniqueId());
                if (player.isOnline() && player.getWorldBorder() == border) player.setWorldBorder(null);
            }, Math.max(1, clearAtTicks - shrinkAtTicks));
            pending.put(player.getUniqueId(), clearTask);
        }, Math.max(1, shrinkAtTicks));
        pending.put(player.getUniqueId(), shrinkTask);
    }

    public void cancel(Player player) {
        BukkitTask t = pending.remove(player.getUniqueId());
        if (t != null) t.cancel();
        active.remove(player.getUniqueId());
        if (player.isOnline()) player.setWorldBorder(null);
    }

    public void clearAll() {
        for (UUID id : active.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.setWorldBorder(null);
        }
        pending.values().forEach(BukkitTask::cancel);
        pending.clear();
        active.clear();
    }

    private static long msToTicks(long ms) {
        return Math.max(1, Math.round(ms / 50.0));
    }
}
