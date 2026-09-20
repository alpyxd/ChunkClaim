package dev.alpay.chunkclaim.claim;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.UUID;

/** Bir chunk'ı dünya + koordinat ile tekil olarak tanımlar. */
public record ChunkKey(UUID world, int x, int z) {

    public static ChunkKey of(Chunk chunk) {
        return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    public static ChunkKey of(Location loc) {
        return new ChunkKey(loc.getWorld().getUID(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    public boolean isAdjacent(ChunkKey other) {
        if (!world.equals(other.world)) return false;
        int dx = Math.abs(x - other.x);
        int dz = Math.abs(z - other.z);
        return dx + dz == 1;
    }

    public String serialize() {
        return world + ";" + x + ";" + z;
    }

    public static ChunkKey deserialize(String s) {
        String[] parts = s.split(";");
        return new ChunkKey(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
