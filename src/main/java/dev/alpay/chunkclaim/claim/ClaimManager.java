package dev.alpay.chunkclaim.claim;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.config.Messages;
import dev.alpay.chunkclaim.config.Settings;
import dev.alpay.chunkclaim.config.UpgradeLevel;
import dev.alpay.chunkclaim.economy.EconomyProvider;
import dev.alpay.chunkclaim.storage.ClaimStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tüm claim'lerin bellek içi indeksi ve iş mantığı. */
public class ClaimManager {

    private final ChunkClaimPlugin plugin;
    private final ClaimStorage storage;

    private final Map<UUID, Claim> claimsById = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Claim> claimsByChunk = new ConcurrentHashMap<>();
    private final Map<UUID, List<Claim>> claimsByOwner = new ConcurrentHashMap<>();

    public ClaimManager(ChunkClaimPlugin plugin, ClaimStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    // ---------- Yükleme / indeks ----------

    public void loadAll() {
        claimsById.clear();
        claimsByChunk.clear();
        claimsByOwner.clear();
        for (Claim c : storage.loadAll()) index(c);
        plugin.getLogger().info("Loaded " + claimsById.size() + " claims.");
    }

    private void index(Claim claim) {
        claimsById.put(claim.getId(), claim);
        for (ChunkKey key : claim.getChunks()) claimsByChunk.put(key, claim);
        claimsByOwner.computeIfAbsent(claim.getOwner(), k -> new ArrayList<>()).add(claim);
    }

    private void unindex(Claim claim) {
        claimsById.remove(claim.getId());
        for (ChunkKey key : claim.getChunks()) claimsByChunk.remove(key, claim);
        List<Claim> list = claimsByOwner.get(claim.getOwner());
        if (list != null) {
            list.remove(claim);
            if (list.isEmpty()) claimsByOwner.remove(claim.getOwner());
        }
    }

    /** Yalnızca hâlâ indeksli (silinmemiş) claim'leri yazar; geç kalan callback'ler dosyayı diriltemez. */
    public void save(Claim claim) {
        if (!claimsById.containsKey(claim.getId())) return;
        storage.save(claim);
    }

    public boolean exists(Claim claim) {
        return claim != null && claimsById.containsKey(claim.getId());
    }

    public void saveAll() {
        for (Claim c : claimsById.values()) storage.save(c);
    }

    // ---------- Sorgular ----------

    public Claim getClaimAt(ChunkKey key) {
        return claimsByChunk.get(key);
    }

    public Claim getClaimAt(Location loc) {
        return claimsByChunk.get(ChunkKey.of(loc));
    }

    public Claim getClaimById(UUID id) {
        return claimsById.get(id);
    }

    public List<Claim> getClaimsOf(UUID owner) {
        return Collections.unmodifiableList(claimsByOwner.getOrDefault(owner, List.of()));
    }

    /** Oyuncunun ilk (genelde tek) claim'i. */
    public Claim getPrimaryClaim(UUID owner) {
        List<Claim> list = claimsByOwner.get(owner);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public Collection<Claim> getAllClaims() {
        return Collections.unmodifiableCollection(claimsById.values());
    }

    /** Verilen konumdaki bloğa bağlı claim (yönetim bloğu mu?). */
    public Claim getClaimByBlock(Location loc) {
        Claim c = getClaimAt(loc);
        if (c == null) return null;
        Location b = c.getBlockLocation();
        if (b == null || b.getWorld() == null || !b.getWorld().equals(loc.getWorld())) return null;
        return b.getBlockX() == loc.getBlockX() && b.getBlockY() == loc.getBlockY() && b.getBlockZ() == loc.getBlockZ() ? c : null;
    }

    /** Tam yetki: sahip veya admin (claim silme, yönetim izinlerini dağıtma). */
    public boolean canManage(Player player, Claim claim) {
        return claim.isOwner(player.getUniqueId()) || player.hasPermission("chunkclaim.admin");
    }

    /** Belirli bir üye iznine sahip mi? Sahip ve admin her zaman evet. */
    public boolean has(Player player, Claim claim, MemberPermission perm) {
        return player.hasPermission("chunkclaim.admin") || claim.hasPermission(player.getUniqueId(), perm);
    }

    /** İzin yoksa mesaj gönderir ve false döner. */
    public boolean require(Player player, Claim claim, MemberPermission perm) {
        if (has(player, claim, perm)) return true;
        plugin.messages().send(player, "no-claim-permission", Map.of("permission", plugin.messages().permissionName(perm)));
        return false;
    }

    /** Oyuncunun içinde bulunduğu ya da sahibi/üyesi olduğu claim (menü açmak için). */
    public Claim getRelevantClaim(Player player) {
        Claim here = getClaimAt(player.getLocation());
        if (here != null && (here.isTrusted(player.getUniqueId()) || player.hasPermission("chunkclaim.admin"))) return here;
        Claim own = getPrimaryClaim(player.getUniqueId());
        if (own != null) return own;
        for (Claim c : claimsById.values()) {
            if (c.isMember(player.getUniqueId())) return c;
        }
        return null;
    }

    public boolean canBypass(Player player) {
        return player.hasPermission("chunkclaim.bypass");
    }

    // ---------- Oluşturma / silme ----------

    /**
     * 1. faz (BlockPlaceEvent HIGH): claim oluşturulabilir mi? Başarısızsa mesaj gönderir ve false döner.
     * Gerçek oluşturma MONITOR'da {@link #createClaim} ile yapılır; böylece başka bir eklenti
     * yerleştirmeyi iptal ederse bloksuz claim kalmaz.
     */
    public boolean canCreateClaim(Player player, Location blockLoc) {
        Settings s = plugin.settings();
        Messages m = plugin.messages();
        World world = blockLoc.getWorld();

        if (!s.allowedWorlds().isEmpty() && !s.allowedWorlds().contains(world.getName())) {
            m.send(player, "world-not-allowed");
            return false;
        }
        ChunkKey key = ChunkKey.of(blockLoc);
        Claim existing = getClaimAt(key);
        if (existing != null) {
            m.send(player, "chunk-already-claimed", Map.of("name", Messages.mm().escapeTags(existing.getName())));
            return false;
        }
        int max = s.maxClaimsPerPlayer();
        if (max > 0 && getClaimsOf(player.getUniqueId()).size() >= max && !player.hasPermission("chunkclaim.admin")) {
            m.send(player, "max-claims-reached", Map.of("max", String.valueOf(max)));
            return false;
        }
        int minDist = s.minDistanceChunks();
        if (minDist > 0) {
            for (Claim other : claimsById.values()) {
                for (ChunkKey ck : other.getChunks()) {
                    if (!ck.world().equals(key.world())) continue;
                    if (Math.abs(ck.x() - key.x()) <= minDist && Math.abs(ck.z() - key.z()) <= minDist) {
                        m.send(player, "too-close", Map.of("distance", String.valueOf(minDist)));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** 2. faz: claim'i oluşturur. {@link #canCreateClaim} önceden çağrılmış olmalı. */
    public Claim createClaim(Player player, Location blockLoc) {
        Settings s = plugin.settings();
        Messages m = plugin.messages();
        ChunkKey key = ChunkKey.of(blockLoc);
        if (getClaimAt(key) != null) return null; // aynı tick içinde yarış

        String name = Messages.replace(m.raw("creation-default-name"), Map.of("player", player.getName()));
        Claim claim = new Claim(UUID.randomUUID(), player.getUniqueId(), name, key, blockLoc.clone(), System.currentTimeMillis());
        index(claim);
        save(claim);
        plugin.holograms().spawn(claim);
        m.send(player, "claim-created");
        plugin.border().show(player, claim);

        if (s.creationAskName()) {
            m.send(player, "creation-name-prompt", Map.of("default", name, "max", String.valueOf(s.maxNameLength())));
            plugin.chatPrompt().ask(player, answer -> {
                if (!exists(claim)) return; // bu arada silinmiş
                if (answer == null || answer.isBlank() || !rename(player, claim, answer)) {
                    m.send(player, "creation-name-default", Map.of("name", Messages.mm().escapeTags(name)));
                }
            });
        }
        return claim;
    }

    /** İsim doğrulaması + kaydetme. Hatalıysa mesaj gönderir ve false döner. */
    public boolean rename(Player player, Claim claim, String newName) {
        if (!exists(claim)) return false;
        String trimmed = newName.trim();
        int max = plugin.settings().maxNameLength();
        if (trimmed.isEmpty() || trimmed.length() > max) {
            plugin.messages().send(player, "name-too-long", Map.of("max", String.valueOf(max)));
            return false;
        }
        if (!plugin.settings().namePattern().matcher(trimmed).matches()) {
            plugin.messages().send(player, "name-invalid");
            return false;
        }
        claim.setName(trimmed);
        save(claim);
        plugin.holograms().update(claim);
        plugin.menus().refreshAllFor(claim);
        plugin.messages().send(player, "name-changed", Map.of("name", Messages.mm().escapeTags(trimmed)));
        return true;
    }

    public void deleteClaim(Claim claim) {
        plugin.holograms().remove(claim);
        unindex(claim);
        storage.delete(claim);
        plugin.menus().closeAllFor(claim);
    }

    /**
     * Onaylı silme akışının son adımı: yönetim bloğunu dünyadan kaldırır,
     * bloğu oyuncuya geri verir, claim'i siler. Sadece sahip/admin.
     */
    public boolean deleteClaimByPlayer(Player player, Claim claim) {
        if (!canManage(player, claim)) {
            plugin.messages().send(player, "not-manage-permission");
            return false;
        }
        Location b = claim.getBlockLocation();
        deleteClaim(claim);
        if (b != null && b.getWorld() != null) {
            if (b.getBlock().getType() == plugin.settings().claimBlockMaterial()) b.getBlock().setType(org.bukkit.Material.AIR);
            player.getInventory().addItem(plugin.claimBlockItem().create(1)).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
        plugin.messages().send(player, "claim-removed");
        return true;
    }

    /**
     * Yönetim bloğunu oyuncunun durduğu yere taşır. Sadece sahip/admin.
     * Hedef ana chunk içinde ve boş (hava/çimen gibi değiştirilebilir) olmalı.
     */
    public boolean moveBlockHere(Player player, Claim claim) {
        Messages m = plugin.messages();
        if (!exists(claim)) return false;
        if (!canManage(player, claim)) {
            m.send(player, "not-manage-permission");
            return false;
        }
        Location target = player.getLocation().getBlock().getLocation();
        if (!claim.getOriginChunk().equals(ChunkKey.of(target))) {
            m.send(player, "block-move-outside");
            return false;
        }
        Location old = claim.getBlockLocation();
        if (old != null && old.getWorld() != null && old.getWorld().equals(target.getWorld())
                && old.getBlockX() == target.getBlockX() && old.getBlockY() == target.getBlockY() && old.getBlockZ() == target.getBlockZ()) {
            m.send(player, "block-move-same");
            return false;
        }
        Block targetBlock = target.getBlock();
        if (!targetBlock.getType().isAir() && !targetBlock.isReplaceable()) {
            m.send(player, "block-move-blocked");
            return false;
        }

        plugin.holograms().remove(claim);
        if (old != null && old.getWorld() != null && old.getBlock().getType() == plugin.settings().claimBlockMaterial()) {
            old.getBlock().setType(org.bukkit.Material.AIR);
        }
        targetBlock.setType(plugin.settings().claimBlockMaterial());
        claim.relocateBlock(target);
        save(claim);
        plugin.holograms().spawn(claim);
        plugin.menus().refreshAllFor(claim);

        // Oyuncu bloğun içinde kalmasın
        Location up = target.clone().add(0.5, 1.0, 0.5);
        up.setYaw(player.getLocation().getYaw());
        up.setPitch(player.getLocation().getPitch());
        player.teleport(up);
        m.send(player, "block-moved");
        return true;
    }

    // ---------- Chunk claim / unclaim ----------

    public boolean claimChunk(Player player, Claim claim, ChunkKey key) {
        Settings s = plugin.settings();
        Messages m = plugin.messages();
        EconomyProvider eco = plugin.economy().provider();

        if (!exists(claim)) return false;
        if (!require(player, claim, MemberPermission.CLAIM_CHUNKS)) return false;
        if (!key.world().equals(claim.getOriginChunk().world())) {
            m.send(player, "chunk-different-world");
            return false;
        }
        Claim existing = getClaimAt(key);
        if (existing != null) {
            m.send(player, "chunk-already-claimed", Map.of("name", Messages.mm().escapeTags(existing.getName())));
            return false;
        }
        if (!claim.isAdjacentToClaim(key)) {
            m.send(player, "chunk-not-adjacent");
            return false;
        }
        int max = s.upgradeValue(claim, UpgradeType.MAX_CHUNKS);
        if (claim.getChunkCount() >= max) {
            m.send(player, "chunk-limit-reached", Map.of("max", String.valueOf(max)));
            return false;
        }
        double cost = plugin.economy().chunkPrice(claim.getChunkCount());
        if (!eco.withdraw(player, cost)) {
            m.send(player, "not-enough-money", Map.of("cost", eco.format(cost), "balance", eco.format(eco.getBalance(player))));
            return false;
        }
        claim.addChunk(key);
        claimsByChunk.put(key, claim);
        save(claim);
        plugin.holograms().update(claim);
        m.send(player, "chunk-claimed", Map.of(
                "chunks", String.valueOf(claim.getChunkCount()),
                "max", String.valueOf(max),
                "cost", eco.format(cost)));
        plugin.border().show(player, claim);
        return true;
    }

    public boolean unclaimChunk(Player player, Claim claim, ChunkKey key) {
        Messages m = plugin.messages();
        EconomyProvider eco = plugin.economy().provider();

        if (!exists(claim)) return false;
        if (!require(player, claim, MemberPermission.UNCLAIM_CHUNKS)) return false;
        if (!claim.hasChunk(key)) {
            m.send(player, "chunk-not-yours");
            return false;
        }
        if (key.equals(claim.getOriginChunk())) {
            m.send(player, "chunk-is-origin");
            return false;
        }
        // Chunk bırakıldıktan sonra kalan chunklar hâlâ bağlantılı mı? (adalar oluşmasın)
        if (!remainsConnected(claim, key)) {
            m.send(player, "chunk-not-adjacent");
            return false;
        }
        claim.removeChunk(key);
        claimsByChunk.remove(key, claim);
        // İade yalnızca sahibe: üye bırakıp sahibin parasını cebe atamasın
        double refund = 0;
        if (claim.isOwner(player.getUniqueId())) {
            refund = plugin.economy().chunkPrice(claim.getChunkCount()) * plugin.settings().unclaimRefund();
            eco.deposit(player, refund);
        }
        save(claim);
        plugin.holograms().update(claim);
        m.send(player, "chunk-unclaimed", Map.of("refund", eco.format(refund)));
        plugin.border().show(player, claim);
        return true;
    }

    /** Origin'den BFS ile, key çıkarıldığında tüm chunklara ulaşılabiliyor mu? */
    private boolean remainsConnected(Claim claim, ChunkKey removed) {
        var remaining = new java.util.HashSet<>(claim.getChunks());
        remaining.remove(removed);
        var visited = new java.util.HashSet<ChunkKey>();
        var queue = new java.util.ArrayDeque<ChunkKey>();
        queue.add(claim.getOriginChunk());
        visited.add(claim.getOriginChunk());
        while (!queue.isEmpty()) {
            ChunkKey c = queue.poll();
            ChunkKey[] neighbors = {
                    new ChunkKey(c.world(), c.x() + 1, c.z()), new ChunkKey(c.world(), c.x() - 1, c.z()),
                    new ChunkKey(c.world(), c.x(), c.z() + 1), new ChunkKey(c.world(), c.x(), c.z() - 1)
            };
            for (ChunkKey n : neighbors) {
                if (remaining.contains(n) && visited.add(n)) queue.add(n);
            }
        }
        return visited.size() == remaining.size();
    }

    // ---------- Üyeler ----------

    public boolean addMember(Player actor, Claim claim, OfflinePlayer target) {
        Messages m = plugin.messages();
        String targetName = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        if (!exists(claim)) return false;
        if (!require(actor, claim, MemberPermission.MANAGE_MEMBERS)) return false;
        if (claim.isOwner(target.getUniqueId())) {
            m.send(actor, "member-cannot-add-self");
            return false;
        }
        if (claim.isMember(target.getUniqueId())) {
            m.send(actor, "member-already", Map.of("player", targetName));
            return false;
        }
        int max = plugin.settings().upgradeValue(claim, UpgradeType.MAX_MEMBERS);
        if (claim.getMembers().size() >= max) {
            m.send(actor, "member-limit-reached", Map.of("max", String.valueOf(max)));
            return false;
        }
        claim.addMember(target.getUniqueId());
        save(claim);
        plugin.holograms().update(claim);
        m.send(actor, "member-added", Map.of("player", targetName));
        return true;
    }

    public boolean removeMember(Player actor, Claim claim, UUID target) {
        if (!exists(claim)) return false;
        if (!require(actor, claim, MemberPermission.MANAGE_MEMBERS)) return false;
        // Üye yöneticisi, kendisi gibi yönetim yetkisi olan birini atamasın (sahip/admin hariç)
        if (!canManage(actor, claim) && claim.hasPermission(target, MemberPermission.MANAGE_MEMBERS)) {
            plugin.messages().send(actor, "member-cannot-remove-manager");
            return false;
        }
        if (!claim.removeMember(target)) return false;
        save(claim);
        plugin.holograms().update(claim);
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        plugin.messages().send(actor, "member-removed", Map.of("player", op.getName() == null ? "?" : op.getName()));
        return true;
    }

    /**
     * Üyenin bir iznini değiştirir. Yönetim izinlerini sadece sahip/admin verebilir;
     * MANAGE_MEMBERS sahibi üye yalnızca temel izinleri düzenleyebilir.
     */
    public boolean toggleMemberPermission(Player actor, Claim claim, UUID target, MemberPermission perm) {
        if (!exists(claim) || !claim.isMember(target)) return false;
        if (!require(actor, claim, MemberPermission.MANAGE_MEMBERS)) return false;
        // Üye yöneticisi başka bir yöneticinin izinlerine dokunamaz
        if (!canManage(actor, claim) && claim.hasPermission(target, MemberPermission.MANAGE_MEMBERS)) {
            plugin.messages().send(actor, "member-cannot-remove-manager");
            return false;
        }
        if (perm.isManagement() && !canManage(actor, claim)) {
            plugin.messages().send(actor, "permission-owner-only");
            return false;
        }
        boolean v = claim.toggleMemberPermission(target, perm);
        save(claim);
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        plugin.messages().send(actor, "permission-toggled", Map.of(
                "player", op.getName() == null ? "?" : op.getName(),
                "permission", plugin.messages().permissionName(perm),
                "state", plugin.messages().raw(v ? "state-on" : "state-off")));
        return true;
    }

    // ---------- Geliştirmeler ----------

    public boolean purchaseUpgrade(Player player, Claim claim, UpgradeType type) {
        Settings s = plugin.settings();
        Messages m = plugin.messages();
        EconomyProvider eco = plugin.economy().provider();

        if (!exists(claim)) return false;
        if (!require(player, claim, MemberPermission.BUY_UPGRADES)) return false;
        UpgradeLevel next = s.nextUpgrade(claim, type);
        if (next == null) {
            m.send(player, "upgrade-max-level");
            return false;
        }
        double cost = plugin.economy().upgradeCost(next);
        if (!eco.withdraw(player, cost)) {
            m.send(player, "not-enough-money", Map.of("cost", eco.format(cost), "balance", eco.format(eco.getBalance(player))));
            return false;
        }
        claim.setUpgradeLevel(type, claim.getUpgradeLevel(type) + 1);
        save(claim);
        plugin.holograms().update(claim);
        m.send(player, "upgrade-purchased", Map.of("upgrade", m.upgradeName(type), "level", String.valueOf(claim.getUpgradeLevel(type))));
        return true;
    }

    /** Toggle tipli koruma satın alınmış (veya base ile açık) mı? */
    public boolean isPurchased(Claim claim, UpgradeType type) {
        return plugin.settings().upgradeValue(claim, type) > 0;
    }

    /** Koruma etkin mi: satın alınmış VE oyuncu tarafından kapatılmamış. */
    public boolean hasUpgrade(Claim claim, UpgradeType type) {
        return isPurchased(claim, type) && claim.isProtectionEnabled(type);
    }

    /** Satın alınmış bir korumayı aç/kapa. */
    public boolean toggleProtection(Player player, Claim claim, UpgradeType type) {
        if (!exists(claim) || !type.isToggle()) return false;
        if (!require(player, claim, MemberPermission.MANAGE_PROTECTIONS)) return false;
        if (!isPurchased(claim, type)) {
            plugin.messages().send(player, "protection-not-purchased", Map.of("protection", plugin.messages().upgradeName(type)));
            return false;
        }
        boolean v = claim.toggleProtection(type);
        save(claim);
        plugin.messages().send(player, "protection-toggled", Map.of(
                "protection", plugin.messages().upgradeName(type),
                "state", plugin.messages().state(v)));
        return true;
    }

    // ---------- Yardımcı ----------

    public String ownerName(Claim claim) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(claim.getOwner());
        return op.getName() == null ? plugin.messages().raw("unknown-player") : op.getName();
    }

    public Map<String, String> placeholders(Claim claim) {
        Settings s = plugin.settings();
        Map<String, String> p = new HashMap<>();
        p.put("name", Messages.mm().escapeTags(claim.getName()));
        p.put("owner", Messages.mm().escapeTags(ownerName(claim)));
        p.put("chunks", String.valueOf(claim.getChunkCount()));
        p.put("max_chunks", String.valueOf(s.upgradeValue(claim, UpgradeType.MAX_CHUNKS)));
        p.put("members", String.valueOf(claim.getMembers().size()));
        p.put("max_members", String.valueOf(s.upgradeValue(claim, UpgradeType.MAX_MEMBERS)));
        return p;
    }
}
