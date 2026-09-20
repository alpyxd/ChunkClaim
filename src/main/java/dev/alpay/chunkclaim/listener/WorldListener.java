package dev.alpay.chunkclaim.listener;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/** Chunk yükleme/boşaltma → hologram yaşam döngüsü. */
public class WorldListener implements Listener {

    private final ChunkClaimPlugin plugin;

    public WorldListener(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        plugin.holograms().onChunkLoad(e.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        plugin.holograms().onChunkUnload(e.getChunk());
    }
}
