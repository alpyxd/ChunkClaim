package dev.alpay.chunkclaim.hologram;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.config.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Yönetim bloğunun üstünde TextDisplay hologramı. Entity'ler kalıcı değildir (persistent=false);
 * chunk yüklenince yeniden oluşturulur, eklenti kapanınca silinir.
 */
public class HologramManager {

    private final ChunkClaimPlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, UUID> entityByClaim = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public HologramManager(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "hologram");
    }

    public void start() {
        stop();
        for (Claim claim : plugin.claims().getAllClaims()) spawn(claim);
        int interval = plugin.settings().hologramUpdateInterval();
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Claim claim : plugin.claims().getAllClaims()) refreshText(claim);
        }, interval, interval);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (Claim claim : plugin.claims().getAllClaims()) remove(claim);
        entityByClaim.clear();
    }

    private Location hologramLocation(Claim claim) {
        Location b = claim.getBlockLocation();
        if (b == null || b.getWorld() == null) return null;
        return b.clone().add(0.5, plugin.settings().hologramYOffset(), 0.5);
    }

    private Component buildText(Claim claim) {
        Map<String, String> ph = plugin.claims().placeholders(claim);
        List<Component> lines = new ArrayList<>();
        for (String raw : plugin.messages().list("hologram.lines")) lines.add(Messages.parse(raw, ph));
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    public void spawn(Claim claim) {
        if (!plugin.settings().hologramEnabled()) return;
        Location loc = hologramLocation(claim);
        if (loc == null) return;
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;

        remove(claim);
        // Aynı chunk'ta eskiden kalmış hologramları temizle (crash sonrası vs.)
        cleanupStale(loc.getChunk(), claim.getId());

        String claimId = claim.getId().toString();
        TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.text(buildText(claim));
            td.setBillboard(Display.Billboard.CENTER);
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            td.setShadowed(true);
            td.setSeeThrough(false);
            td.setDefaultBackground(false);
            td.setBackgroundColor(Color.fromARGB(90, 0, 0, 0));
            td.setViewRange(0.6f);
            td.setPersistent(false);
            td.getPersistentDataContainer().set(key, PersistentDataType.STRING, claimId);
        });
        entityByClaim.put(claim.getId(), display.getUniqueId());
    }

    private TextDisplay find(Claim claim) {
        UUID id = entityByClaim.get(claim.getId());
        if (id == null) return null;
        Entity e = plugin.getServer().getEntity(id);
        if (e instanceof TextDisplay td && td.isValid()) return td;
        entityByClaim.remove(claim.getId());
        return null;
    }

    private void refreshText(Claim claim) {
        TextDisplay td = find(claim);
        if (td != null) td.text(buildText(claim));
    }

    /** Metni günceller; entity yoksa (chunk yüklüyse) yeniden oluşturur. */
    public void update(Claim claim) {
        TextDisplay td = find(claim);
        if (td == null) spawn(claim);
        else td.text(buildText(claim));
    }

    public void remove(Claim claim) {
        TextDisplay td = find(claim);
        if (td != null) td.remove();
        entityByClaim.remove(claim.getId());
    }

    /** Bu claim'e ait, takip edilmeyen (eski) hologram entity'lerini siler. */
    private void cleanupStale(Chunk chunk, UUID claimId) {
        String id = claimId.toString();
        for (Entity e : chunk.getEntities()) {
            if (!(e instanceof TextDisplay td)) continue;
            String tag = td.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (id.equals(tag)) td.remove();
        }
    }

    /** Chunk yüklendiğinde: eski hologramları sil, bu chunk'taki claim bloklarının hologramını kur. */
    public void onChunkLoad(Chunk chunk) {
        if (!plugin.settings().hologramEnabled()) return;
        boolean any = false;
        for (Claim claim : plugin.claims().getAllClaims()) {
            Location b = claim.getBlockLocation();
            if (b == null || b.getWorld() == null) continue;
            if (!b.getWorld().equals(chunk.getWorld())) continue;
            if ((b.getBlockX() >> 4) != chunk.getX() || (b.getBlockZ() >> 4) != chunk.getZ()) continue;
            spawn(claim);
            any = true;
        }
        if (!any) {
            // Claim'i olmayan artık hologramlar
            for (Entity e : chunk.getEntities()) {
                if (e instanceof TextDisplay td && td.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    td.remove();
                }
            }
        }
    }

    public void onChunkUnload(Chunk chunk) {
        for (Claim claim : plugin.claims().getAllClaims()) {
            Location b = claim.getBlockLocation();
            if (b == null || b.getWorld() == null || !b.getWorld().equals(chunk.getWorld())) continue;
            if ((b.getBlockX() >> 4) == chunk.getX() && (b.getBlockZ() >> 4) == chunk.getZ()) {
                entityByClaim.remove(claim.getId());
            }
        }
    }
}
