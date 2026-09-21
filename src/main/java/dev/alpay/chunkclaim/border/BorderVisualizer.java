package dev.alpay.chunkclaim.border;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.ChunkKey;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Claim alanını gösterir. İki yöntem:
 * <ul>
 *   <li><b>World border</b> — claim tam kare ise. Sanal sınır kare merkezinden büyür, bekler, küçülür.</li>
 *   <li><b>Partikül</b> — claim kare değilse (L, T, şerit…). Claim'in gerçek çevresi partikülle çizilir;
 *       çizgi ana chunk'ın merkezinden dışa doğru "yayılarak" belirir, bekler, içe doğru kaybolur.</li>
 * </ul>
 * World border doğası gereği kare olduğundan kare olmayan claim'lerde claim dışı alanı da kaplar;
 * bu yüzden AUTO modda sadece tam kare claim'lerde kullanılır.
 */
public class BorderVisualizer {

    /** Oyuncuya gösterilen aktif world border: merkez ve genişleme bitiş zamanı (kenar mesafesi hesabı için). */
    private record Active(WorldBorder border, double cx, double cz, long readyAt) { }

    /** Çevre çizgisi üzerindeki bir nokta ve merkezden uzaklığı. */
    private record Point(double x, double z, double dist) { }

    private final ChunkClaimPlugin plugin;
    private final Map<UUID, BukkitTask> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Active> active = new ConcurrentHashMap<>();

    public BorderVisualizer(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    public void show(Player player, Claim claim) {
        if (!player.getWorld().getUID().equals(claim.getOriginChunk().world())) return;
        cancel(player);

        Set<ChunkKey> chunks = claim.getChunks();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (ChunkKey c : chunks) {
            minX = Math.min(minX, c.x()); maxX = Math.max(maxX, c.x());
            minZ = Math.min(minZ, c.z()); maxZ = Math.max(maxZ, c.z());
        }
        int w = maxX - minX + 1, h = maxZ - minZ + 1;
        boolean square = w == h && chunks.size() == w * h;

        Settings.BorderMode mode = plugin.settings().borderMode();
        boolean useWorldBorder = mode == Settings.BorderMode.WORLD_BORDER || (mode == Settings.BorderMode.AUTO && square);

        if (useWorldBorder) {
            // Kare değilse bile WORLD_BORDER modu zorlanmışsa bounding box'ı göster
            double cx = (minX + maxX + 1) * 8.0;
            double cz = (minZ + maxZ + 1) * 8.0;
            showWorldBorder(player, cx, cz, Math.max(w, h) * 16.0);
        } else {
            showParticles(player, claim);
        }
    }

    // ---------- World border ----------

    private void showWorldBorder(Player player, double cx, double cz, double targetSize) {
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
        border.changeSize(Math.max(2.0, targetSize), msToTicks(expand));
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

    /**
     * Her harekette çağrılır. İstemci tarafı sınır gerçek bir duvar olduğundan, oyuncu
     * kenara yaklaşınca önizlemeyi kaldırır ki içeri/dışarı geçişi engellemesin.
     */
    public void onMove(Player player, Location to) {
        Active a = active.get(player.getUniqueId());
        if (a == null) return;
        double dist = plugin.settings().borderAutoHideDistance();
        if (dist <= 0) return;
        if (System.currentTimeMillis() < a.readyAt()) return;
        double half = a.border().getSize() / 2.0;
        double dx = half - Math.abs(to.getX() - a.cx());
        double dz = half - Math.abs(to.getZ() - a.cz());
        if (Math.abs(dx) <= dist || Math.abs(dz) <= dist) cancel(player);
    }

    // ---------- Partikül çevre çizgisi ----------

    private List<Point> outline(Claim claim) {
        Set<ChunkKey> chunks = claim.getChunks();
        ChunkKey origin = claim.getOriginChunk();
        double ox = origin.x() * 16 + 8.0, oz = origin.z() * 16 + 8.0;
        UUID world = origin.world();
        List<Point> points = new ArrayList<>();

        for (ChunkKey c : chunks) {
            double x0 = c.x() * 16.0, x1 = x0 + 16, z0 = c.z() * 16.0, z1 = z0 + 16;
            if (!chunks.contains(new ChunkKey(world, c.x(), c.z() - 1))) line(points, x0, z0, x1, z0, ox, oz); // kuzey
            if (!chunks.contains(new ChunkKey(world, c.x(), c.z() + 1))) line(points, x0, z1, x1, z1, ox, oz); // güney
            if (!chunks.contains(new ChunkKey(world, c.x() - 1, c.z()))) line(points, x0, z0, x0, z1, ox, oz); // batı
            if (!chunks.contains(new ChunkKey(world, c.x() + 1, c.z()))) line(points, x1, z0, x1, z1, ox, oz); // doğu
        }
        return points;
    }

    private static void line(List<Point> out, double x0, double z0, double x1, double z1, double ox, double oz) {
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = x0 + (x1 - x0) * t, z = z0 + (z1 - z0) * t;
            out.add(new Point(x, z, Math.hypot(x - ox, z - oz)));
        }
    }

    private void showParticles(Player player, Claim claim) {
        List<Point> points = outline(claim);
        if (points.isEmpty()) return;
        double maxDist = 0;
        for (Point p : points) maxDist = Math.max(maxDist, p.dist());

        long expand = Math.max(1, plugin.settings().borderExpandMs());
        long hold = Math.max(0, plugin.settings().borderHoldMs());
        long shrink = Math.max(1, plugin.settings().borderShrinkMs());
        long total = expand + hold + shrink;
        long start = System.currentTimeMillis();
        Particle.DustOptions dust = new Particle.DustOptions(plugin.settings().borderParticleColor(), 1.3f);
        final double reach = maxDist;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancel(player);
                return;
            }
            long t = System.currentTimeMillis() - start;
            if (t >= total) {
                cancel(player);
                return;
            }
            // Görünür yarıçap: genişlerken 0→max, beklerken max, küçülürken max→0
            double radius;
            if (t < expand) radius = reach * (t / (double) expand);
            else if (t < expand + hold) radius = reach;
            else radius = reach * (1 - (t - expand - hold) / (double) shrink);

            Location loc = player.getLocation();
            double y0 = loc.getY() + 0.2, y1 = loc.getY() + 1.2;
            double px = loc.getX(), pz = loc.getZ();
            for (Point p : points) {
                if (p.dist() > radius) continue;
                // Uzaktaki partiküller zaten görünmez; paket sayısını sınırla
                if (Math.abs(p.x() - px) > 96 || Math.abs(p.z() - pz) > 96) continue;
                player.spawnParticle(Particle.DUST, p.x(), y0, p.z(), 1, 0, 0, 0, 0, dust);
                player.spawnParticle(Particle.DUST, p.x(), y1, p.z(), 1, 0, 0, 0, 0, dust);
            }
        }, 0L, 3L);
        pending.put(player.getUniqueId(), task);
    }

    // ---------- Ortak ----------

    public void cancel(Player player) {
        BukkitTask t = pending.remove(player.getUniqueId());
        if (t != null) t.cancel();
        if (active.remove(player.getUniqueId()) != null && player.isOnline()) player.setWorldBorder(null);
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
