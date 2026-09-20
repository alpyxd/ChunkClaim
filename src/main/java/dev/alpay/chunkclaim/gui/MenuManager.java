package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Açık menüleri takip eder ve envanter olaylarını ilgili menüye yönlendirir. */
public class MenuManager implements Listener {

    private final ChunkClaimPlugin plugin;
    private final Map<UUID, Menu> open = new ConcurrentHashMap<>();

    public MenuManager(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    void track(Menu menu) {
        open.put(menu.player().getUniqueId(), menu);
    }

    public void openMain(Player player, Claim claim) {
        new MainMenu(plugin, player, claim).open();
    }

    public void closeAllFor(Claim claim) {
        List<Player> toClose = new ArrayList<>();
        for (Menu m : open.values()) {
            if (m.claim().getId().equals(claim.getId())) toClose.add(m.player());
        }
        for (Player p : toClose) p.closeInventory();
    }

    /** Claim verisi değişince açık menüleri yeniler. */
    public void refreshAllFor(Claim claim) {
        for (Menu m : open.values()) {
            if (m.claim().getId().equals(claim.getId())) m.refresh();
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Menu menu)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        // Tıklama olayı içinde envanter açmak/kapatmak güvenli değil → bir sonraki tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (open.get(menu.player().getUniqueId()) == menu) menu.handleClick(event);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Menu) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu menu) {
            open.remove(event.getPlayer().getUniqueId(), menu);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        open.remove(event.getPlayer().getUniqueId());
    }
}
