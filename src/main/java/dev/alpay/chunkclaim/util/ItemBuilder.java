package dev.alpay.chunkclaim.util;

import dev.alpay.chunkclaim.config.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Menü eşyaları için küçük yardımcı. Tüm metinler MiniMessage, italic kapalı. */
public class ItemBuilder {

    private final ItemStack item;
    private final List<Component> lore = new ArrayList<>();
    private Map<String, String> placeholders = Map.of();

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public ItemBuilder placeholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
        return this;
    }

    public ItemBuilder name(String miniMessage) {
        item.editMeta(meta -> meta.displayName(Messages.item(miniMessage, placeholders)));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        for (String l : lines) lore.add(Messages.item(l, placeholders));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        for (String l : lines) lore.add(Messages.item(l, placeholders));
        return this;
    }

    public ItemBuilder loreComponents(List<Component> lines) {
        lore.addAll(lines);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        item.editMeta(meta -> {
            meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null);
        });
        return this;
    }

    public ItemBuilder hideAttributes() {
        item.editMeta(meta -> meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES));
        return this;
    }

    public ItemBuilder skull(UUID owner) {
        if (item.getType() != Material.PLAYER_HEAD) return this;
        OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
        item.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(op));
        return this;
    }

    public ItemBuilder meta(java.util.function.Consumer<ItemMeta> consumer) {
        item.editMeta(consumer);
        return this;
    }

    /** Şimdiye kadar eklenen lore satırları (başka bir builder'a aktarmak için). */
    public List<Component> buildLore() {
        return new ArrayList<>(lore);
    }

    public ItemStack build() {
        if (!lore.isEmpty()) item.editMeta(meta -> meta.lore(lore));
        return item;
    }
}
