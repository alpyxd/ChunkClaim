package dev.alpay.chunkclaim.claim;

import org.bukkit.Location;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tek bir claim: sahip, üyeler, chunk listesi, ayarlar ve geliştirmeler. */
public class Claim {

    private final UUID id;
    private final UUID owner;
    private String name;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Map<UUID, Set<MemberPermission>> memberPermissions = new HashMap<>();
    private final Set<ChunkKey> chunks = new LinkedHashSet<>();
    private final ChunkKey originChunk;
    private Location blockLocation;
    private Location home;
    private final Map<ClaimFlag, Boolean> flags = new EnumMap<>(ClaimFlag.class);
    private final Map<UpgradeType, Integer> upgrades = new EnumMap<>(UpgradeType.class);
    /** Satın alınmış toggle korumaların açık/kapalı durumu (satın alınınca varsayılan açık). */
    private final Map<UpgradeType, Boolean> protectionEnabled = new EnumMap<>(UpgradeType.class);
    private final long createdAt;

    public Claim(UUID id, UUID owner, String name, ChunkKey originChunk, Location blockLocation, long createdAt) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.originChunk = originChunk;
        this.blockLocation = blockLocation;
        this.createdAt = createdAt;
        this.chunks.add(originChunk);
        for (ClaimFlag flag : ClaimFlag.values()) flags.put(flag, flag.defaultValue());
        for (UpgradeType type : UpgradeType.values()) upgrades.put(type, 0);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean addMember(UUID uuid) {
        if (!members.add(uuid)) return false;
        memberPermissions.put(uuid, EnumSet.copyOf(MemberPermission.defaults()));
        return true;
    }

    public boolean removeMember(UUID uuid) {
        memberPermissions.remove(uuid);
        return members.remove(uuid);
    }

    // ---- Üye izinleri ----

    public Set<MemberPermission> getMemberPermissions(UUID uuid) {
        Set<MemberPermission> set = memberPermissions.get(uuid);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    public void setMemberPermissions(UUID uuid, Set<MemberPermission> perms) {
        if (!members.contains(uuid)) return;
        memberPermissions.put(uuid, perms.isEmpty() ? EnumSet.noneOf(MemberPermission.class) : EnumSet.copyOf(perms));
    }

    /** Sahip her şeye sahiptir; üyeler kendi setine bakar; diğerleri false. */
    public boolean hasPermission(UUID uuid, MemberPermission perm) {
        if (isOwner(uuid)) return true;
        Set<MemberPermission> set = memberPermissions.get(uuid);
        return set != null && set.contains(perm);
    }

    public boolean toggleMemberPermission(UUID uuid, MemberPermission perm) {
        Set<MemberPermission> set = memberPermissions.computeIfAbsent(uuid, k -> EnumSet.noneOf(MemberPermission.class));
        if (set.remove(perm)) return false;
        set.add(perm);
        return true;
    }

    /**
     * Bu oyuncu, verilen ziyaretçi flag'inin kapsadığı eylemi yapabilir mi?
     * Sahip → evet. Üye → ilgili üye izni (eşleşme yoksa evet). Ziyaretçi → flag değeri.
     */
    public boolean canDo(UUID uuid, ClaimFlag flag) {
        if (isOwner(uuid)) return true;
        if (isMember(uuid)) {
            MemberPermission perm = MemberPermission.forFlag(flag);
            return perm == null || hasPermission(uuid, perm);
        }
        return getFlag(flag);
    }

    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    /** Sahip veya üye mi? */
    public boolean isTrusted(UUID uuid) {
        return isOwner(uuid) || isMember(uuid);
    }

    public Set<ChunkKey> getChunks() {
        return Collections.unmodifiableSet(chunks);
    }

    public int getChunkCount() {
        return chunks.size();
    }

    public boolean addChunk(ChunkKey key) {
        return chunks.add(key);
    }

    public boolean removeChunk(ChunkKey key) {
        if (key.equals(originChunk)) return false;
        return chunks.remove(key);
    }

    public boolean hasChunk(ChunkKey key) {
        return chunks.contains(key);
    }

    /** Verilen chunk, claim'in mevcut chunklarından birine komşu mu? */
    public boolean isAdjacentToClaim(ChunkKey key) {
        for (ChunkKey c : chunks) {
            if (c.isAdjacent(key)) return true;
        }
        return false;
    }

    public ChunkKey getOriginChunk() {
        return originChunk;
    }

    public Location getBlockLocation() {
        return blockLocation;
    }

    public void setBlockLocation(Location blockLocation) {
        this.blockLocation = blockLocation;
    }

    /** Özel ev noktası (null = yönetim bloğunun üstü). */
    public Location getCustomHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home == null ? null : home.clone();
    }

    /**
     * Işınlanma hedefi: özel ev noktası hâlâ claim içindeyse o, değilse yönetim bloğunun üstü.
     */
    public Location getHome() {
        if (home != null && home.getWorld() != null && hasChunk(ChunkKey.of(home))) return home.clone();
        if (blockLocation == null || blockLocation.getWorld() == null) return null;
        return blockLocation.clone().add(0.5, 1.0, 0.5);
    }

    public boolean getFlag(ClaimFlag flag) {
        return flags.getOrDefault(flag, flag.defaultValue());
    }

    public void setFlag(ClaimFlag flag, boolean value) {
        flags.put(flag, value);
    }

    public boolean toggleFlag(ClaimFlag flag) {
        boolean v = !getFlag(flag);
        flags.put(flag, v);
        return v;
    }

    public Map<ClaimFlag, Boolean> getFlags() {
        return Collections.unmodifiableMap(flags);
    }

    public int getUpgradeLevel(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public void setUpgradeLevel(UpgradeType type, int level) {
        upgrades.put(type, Math.max(0, level));
    }

    public Map<UpgradeType, Integer> getUpgrades() {
        return Collections.unmodifiableMap(upgrades);
    }

    // ---- Koruma aç/kapa ----

    /** Koruma açık mı? (Satın alınıp alınmadığına bakmaz; varsayılan açık.) */
    public boolean isProtectionEnabled(UpgradeType type) {
        return protectionEnabled.getOrDefault(type, true);
    }

    public void setProtectionEnabled(UpgradeType type, boolean enabled) {
        protectionEnabled.put(type, enabled);
    }

    public boolean toggleProtection(UpgradeType type) {
        boolean v = !isProtectionEnabled(type);
        protectionEnabled.put(type, v);
        return v;
    }

    public Map<UpgradeType, Boolean> getProtectionToggles() {
        return Collections.unmodifiableMap(protectionEnabled);
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
