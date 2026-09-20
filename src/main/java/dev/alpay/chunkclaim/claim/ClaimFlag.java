package dev.alpay.chunkclaim.claim;

import org.bukkit.Material;

/**
 * Ziyaretçi (üye olmayan) oyuncuların claim içinde neler yapabileceğini
 * belirleyen aç/kapa ayarlar. Görünen isimler dil dosyasında: flags.&lt;NAME&gt;
 */
public enum ClaimFlag {
    BUILD(Material.BRICKS, false),
    BREAK(Material.IRON_PICKAXE, false),
    INTERACT(Material.OAK_DOOR, false),
    CONTAINERS(Material.CHEST, false),
    PVP(Material.DIAMOND_SWORD, false),
    ANIMAL_DAMAGE(Material.LEAD, false),
    ITEM_PICKUP(Material.HOPPER, true),
    ITEM_DROP(Material.DROPPER, true),
    USE_ITEMS(Material.WATER_BUCKET, false),
    ENTITY_INTERACT(Material.VILLAGER_SPAWN_EGG, false),
    ENTER(Material.OAK_FENCE_GATE, true);

    private final Material icon;
    private final boolean defaultValue;

    ClaimFlag(Material icon, boolean defaultValue) {
        this.icon = icon;
        this.defaultValue = defaultValue;
    }

    public Material icon() {
        return icon;
    }

    public boolean defaultValue() {
        return defaultValue;
    }
}
