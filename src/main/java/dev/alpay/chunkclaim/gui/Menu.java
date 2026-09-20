package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.config.Messages;
import dev.alpay.chunkclaim.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Tüm menülerin temel sınıfı. InventoryHolder olarak kendini tanıtır; tıklamalar slot → aksiyon eşlemesiyle işlenir. */
public abstract class Menu implements InventoryHolder {

    protected final ChunkClaimPlugin plugin;
    protected final Player player;
    protected final Claim claim;
    private final Map<Integer, Consumer<ClickType>> actions = new HashMap<>();
    private Inventory inventory;

    protected Menu(ChunkClaimPlugin plugin, Player player, Claim claim) {
        this.plugin = plugin;
        this.player = player;
        this.claim = claim;
    }

    /** Başlık: varsayılan olarak gui.&lt;section&gt;.title, claim placeholder'larıyla. */
    protected String title() {
        return t("title");
    }

    protected Map<String, String> titlePlaceholders() {
        return plugin.claims().placeholders(claim);
    }

    protected abstract int size();

    protected abstract void build();

    public void open() {
        inventory = Bukkit.createInventory(this, size(), Messages.parse(title(), titlePlaceholders()));
        actions.clear();
        build();
        player.openInventory(inventory);
        plugin.menus().track(this);
    }

    public void refresh() {
        if (inventory == null) return;
        inventory.clear();
        actions.clear();
        build();
    }

    protected void set(int slot, ItemStack item, Consumer<ClickType> action) {
        inventory.setItem(slot, item);
        if (action != null) actions.put(slot, action);
        else actions.remove(slot);
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected void fill(Material material) {
        ItemStack filler = ItemBuilder.of(material).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
    }

    protected void backButton(int slot, Runnable back) {
        set(slot, ItemBuilder.of(Material.ARROW).name(msg().raw("gui.common.back")).build(), click -> back.run());
    }

    protected void closeButton(int slot) {
        set(slot, ItemBuilder.of(Material.BARRIER).name(msg().raw("gui.common.close")).build(), click -> player.closeInventory());
    }

    protected Messages msg() {
        return plugin.messages();
    }

    /** Menü metinleri için kısayol: gui.&lt;section&gt;.&lt;key&gt; */
    protected String t(String key) {
        return msg().raw("gui." + section() + "." + key);
    }

    protected List<String> tl(String key) {
        return msg().list("gui." + section() + "." + key);
    }

    protected String common(String key) {
        return msg().raw("gui.common." + key);
    }

    /** Dil dosyasındaki gui.* bölümü adı. */
    protected abstract String section();

    public void handleClick(InventoryClickEvent event) {
        Consumer<ClickType> action = actions.get(event.getRawSlot());
        if (action != null) action.accept(event.getClick());
    }

    public Claim claim() {
        return claim;
    }

    public Player player() {
        return player;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
