package dev.alpay.chunkclaim.util;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;

/**
 * Claim pusulası: yönetim bloğuna pusulayla sağ tıklanınca pusula claim'e bağlanır.
 * Lodestone takibi kapalı tutulur (tracked=false) ki blok yerinde olmasa da hedefi göstersin;
 * blok taşınınca / claim silinince online oyuncuların pusulaları güncellenir.
 */
public class ClaimCompass implements Listener {

    private final ChunkClaimPlugin plugin;
    private final NamespacedKey key;

    public ClaimCompass(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "compass_claim");
    }

    public boolean isClaimCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    private UUID claimIdOf(ItemMeta meta) {
        String s = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Pusulayı claim'e bağlar (isim, lore, hedef, PDC). Tüm yığın bağlanır. */
    public void bind(Player player, Claim claim, ItemStack compass) {
        Location target = claim.getBlockLocation();
        if (target == null) return;
        compass.editMeta(CompassMeta.class, meta -> apply(meta, claim, target));
        plugin.messages().send(player, "compass-bound", plugin.claims().placeholders(claim));
    }

    private void apply(CompassMeta meta, Claim claim, Location target) {
        Map<String, String> ph = plugin.claims().placeholders(claim);
        meta.setLodestone(target.clone().add(0.5, 0, 0.5));
        meta.setLodestoneTracked(false);
        meta.displayName(Messages.item(plugin.messages().raw("compass.name"), ph));
        meta.lore(plugin.messages().items(plugin.messages().list("compass.lore"), ph));
        meta.setEnchantmentGlintOverride(Boolean.TRUE);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, claim.getId().toString());
    }

    /** Blok taşındı / isim değişti: online oyuncuların bu claim'e bağlı pusulalarını yeniler. */
    public void refresh(Claim claim) {
        Location target = claim.getBlockLocation();
        if (target == null) return;
        forEachCompass(claim.getId(), item -> item.editMeta(CompassMeta.class, meta -> apply(meta, claim, target)));
    }

    /** Claim silindi: pusulalar normal pusulaya döner, ismi "bağlantı koptu" olur. */
    public void invalidate(UUID claimId) {
        forEachCompass(claimId, item -> item.editMeta(CompassMeta.class, meta -> {
            meta.setLodestone(null);
            meta.setLodestoneTracked(false);
            meta.displayName(Messages.item(plugin.messages().raw("compass.name-deleted")));
            meta.lore(plugin.messages().items(plugin.messages().list("compass.lore-deleted"), Map.of()));
            meta.setEnchantmentGlintOverride(null);
            meta.getPersistentDataContainer().remove(key);
        }));
    }

    /** Oyuncu girince: çevrimdışıyken silinen/taşınan claim'lerin pusulalarını düzelt. */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        for (ItemStack item : p.getInventory().getContents()) syncItem(item);
        for (ItemStack item : p.getEnderChest().getContents()) syncItem(item);
    }

    private void syncItem(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return;
        UUID id = claimIdOf(item.getItemMeta());
        if (id == null) return;
        Claim claim = plugin.claims().getClaimById(id);
        if (claim == null || claim.getBlockLocation() == null) {
            item.editMeta(CompassMeta.class, meta -> {
                meta.setLodestone(null);
                meta.setLodestoneTracked(false);
                meta.displayName(Messages.item(plugin.messages().raw("compass.name-deleted")));
                meta.lore(plugin.messages().items(plugin.messages().list("compass.lore-deleted"), Map.of()));
                meta.setEnchantmentGlintOverride(null);
                meta.getPersistentDataContainer().remove(key);
            });
        } else {
            item.editMeta(CompassMeta.class, meta -> apply(meta, claim, claim.getBlockLocation()));
        }
    }

    private void forEachCompass(UUID claimId, java.util.function.Consumer<ItemStack> action) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : p.getInventory().getContents()) {
                if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) continue;
                if (claimId.equals(claimIdOf(item.getItemMeta()))) action.accept(item);
            }
            for (ItemStack item : p.getEnderChest().getContents()) {
                if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) continue;
                if (claimId.equals(claimIdOf(item.getItemMeta()))) action.accept(item);
            }
        }
    }
}
