package dev.alpay.chunkclaim.claim;

import org.bukkit.Material;

/** Satın alınabilir alan geliştirmeleri. Seviyeler config.yml'den, isimler dil dosyasından (upgrades.&lt;key&gt;). */
public enum UpgradeType {
    MAX_CHUNKS("max-chunks", Material.GRASS_BLOCK),
    MAX_MEMBERS("max-members", Material.PLAYER_HEAD),
    EXPLOSION_PROTECTION("explosion-protection", Material.TNT),
    FIRE_PROTECTION("fire-protection", Material.FLINT_AND_STEEL),
    MOB_SPAWN_BLOCK("mob-spawn-block", Material.ZOMBIE_HEAD);

    private final String key;
    private final Material icon;

    UpgradeType(String key, Material icon) {
        this.key = key;
        this.icon = icon;
    }

    public String key() {
        return key;
    }

    public Material icon() {
        return icon;
    }

    /** Boolean tipli geliştirmeler (0 = kapalı, 1+ = açık). */
    public boolean isToggle() {
        return this == EXPLOSION_PROTECTION || this == FIRE_PROTECTION || this == MOB_SPAWN_BLOCK;
    }
}
