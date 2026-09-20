package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Menüden kapatılıp sohbete yazılan cevabı bekleyen basit prompt sistemi. Süresi dolan istekler yok sayılır. */
public class ChatPrompt implements Listener {

    private record Waiting(Consumer<String> callback, long expiresAt) { }

    private final ChunkClaimPlugin plugin;
    private final Map<UUID, Waiting> waiting = new ConcurrentHashMap<>();

    public ChatPrompt(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    /** Oyuncunun bir sonraki sohbet mesajını yakalar. "iptal"/"cancel" yazılırsa callback null ile çağrılır. */
    public void ask(Player player, Consumer<String> callback) {
        player.closeInventory();
        long expires = System.currentTimeMillis() + plugin.settings().chatPromptTimeout() * 1000L;
        waiting.put(player.getUniqueId(), new Waiting(callback, expires));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Waiting w = waiting.remove(event.getPlayer().getUniqueId());
        if (w == null) return;
        if (System.currentTimeMillis() > w.expiresAt()) {
            // Süresi dolmuş: mesaj normal sohbete gitsin, oyuncuya bilgi ver
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.messages().send(event.getPlayer(), "prompt-expired"));
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        String answer = text.equalsIgnoreCase("iptal") || text.equalsIgnoreCase("cancel") ? null : text;
        plugin.getServer().getScheduler().runTask(plugin, () -> w.callback().accept(answer));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        waiting.remove(event.getPlayer().getUniqueId());
    }
}
