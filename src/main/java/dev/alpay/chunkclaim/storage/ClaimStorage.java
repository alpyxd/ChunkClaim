package dev.alpay.chunkclaim.storage;

import dev.alpay.chunkclaim.claim.ChunkKey;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.ClaimFlag;
import dev.alpay.chunkclaim.claim.MemberPermission;
import dev.alpay.chunkclaim.claim.UpgradeType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Her claim'i plugins/ChunkClaim/claims/<id>.yml dosyasında saklar. */
public class ClaimStorage {

    private final JavaPlugin plugin;
    private final File dir;

    public ClaimStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "claims");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().severe("Could not create claims directory: " + dir);
        }
    }

    public List<Claim> loadAll() {
        List<Claim> claims = new ArrayList<>();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return claims;
        for (File f : files) {
            try {
                Claim c = load(f);
                if (c != null) claims.add(c);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not load claim: " + f.getName(), e);
            }
        }
        return claims;
    }

    private Claim load(File file) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        UUID id = UUID.fromString(y.getString("id", file.getName().replace(".yml", "")));
        UUID owner = UUID.fromString(y.getString("owner"));
        String name = y.getString("name", "Claim");
        ChunkKey origin = ChunkKey.deserialize(y.getString("origin"));
        long created = y.getLong("created", System.currentTimeMillis());

        Location blockLoc = null;
        String worldId = y.getString("block.world");
        if (worldId != null) {
            World w = Bukkit.getWorld(UUID.fromString(worldId));
            if (w != null) {
                blockLoc = new Location(w, y.getInt("block.x"), y.getInt("block.y"), y.getInt("block.z"));
            } else {
                plugin.getLogger().warning("Claim " + id + ": world not loaded: " + worldId);
                return null;
            }
        }

        Claim claim = new Claim(id, owner, name, origin, blockLoc, created);
        if (y.contains("home.x") && blockLoc != null) {
            claim.setHome(new Location(blockLoc.getWorld(), y.getDouble("home.x"), y.getDouble("home.y"), y.getDouble("home.z"),
                    (float) y.getDouble("home.yaw"), (float) y.getDouble("home.pitch")));
        }
        for (String s : y.getStringList("members")) {
            UUID member = UUID.fromString(s);
            claim.addMember(member);
            // İzin listesi kayıtlıysa varsayılanın üzerine yaz (eski dosyalarda yoksa varsayılan kalır)
            if (y.contains("member-permissions." + s)) {
                Set<MemberPermission> perms = EnumSet.noneOf(MemberPermission.class);
                for (String p : y.getStringList("member-permissions." + s)) {
                    try {
                        perms.add(MemberPermission.valueOf(p));
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Claim " + id + ": unknown member permission " + p);
                    }
                }
                claim.setMemberPermissions(member, perms);
            }
        }
        for (String s : y.getStringList("chunks")) claim.addChunk(ChunkKey.deserialize(s));
        for (ClaimFlag flag : ClaimFlag.values()) {
            if (y.contains("flags." + flag.name())) claim.setFlag(flag, y.getBoolean("flags." + flag.name()));
        }
        for (UpgradeType type : UpgradeType.values()) {
            claim.setUpgradeLevel(type, y.getInt("upgrades." + type.key(), 0));
        }
        return claim;
    }

    public void save(Claim claim) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("id", claim.getId().toString());
        y.set("owner", claim.getOwner().toString());
        y.set("name", claim.getName());
        y.set("origin", claim.getOriginChunk().serialize());
        y.set("created", claim.getCreatedAt());
        Location b = claim.getBlockLocation();
        if (b != null && b.getWorld() != null) {
            y.set("block.world", b.getWorld().getUID().toString());
            y.set("block.x", b.getBlockX());
            y.set("block.y", b.getBlockY());
            y.set("block.z", b.getBlockZ());
        }
        Location h = claim.getCustomHome();
        if (h != null) {
            y.set("home.x", h.getX());
            y.set("home.y", h.getY());
            y.set("home.z", h.getZ());
            y.set("home.yaw", (double) h.getYaw());
            y.set("home.pitch", (double) h.getPitch());
        }
        y.set("members", claim.getMembers().stream().map(UUID::toString).toList());
        for (UUID member : claim.getMembers()) {
            y.set("member-permissions." + member, claim.getMemberPermissions(member).stream().map(Enum::name).toList());
        }
        y.set("chunks", claim.getChunks().stream().map(ChunkKey::serialize).toList());
        for (var e : claim.getFlags().entrySet()) y.set("flags." + e.getKey().name(), e.getValue());
        for (var e : claim.getUpgrades().entrySet()) y.set("upgrades." + e.getKey().key(), e.getValue());

        File file = new File(dir, claim.getId() + ".yml");
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save claim: " + claim.getId(), e);
        }
    }

    public void delete(Claim claim) {
        File file = new File(dir, claim.getId() + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete claim file: " + file);
        }
    }
}
