package dev.alpay.chunkclaim.claim;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

/**
 * Üyelere tek tek verilebilen izinler. Sahip her zaman hepsine sahiptir.
 * {@code management} olanlar yalnızca sahip (veya admin) tarafından değiştirilebilir.
 * Görünen isimler dil dosyasında: permissions.&lt;NAME&gt;
 */
public enum MemberPermission {
    BUILD(Material.BRICKS, true, false),
    BREAK(Material.IRON_PICKAXE, true, false),
    INTERACT(Material.OAK_DOOR, true, false),
    CONTAINERS(Material.CHEST, true, false),
    USE_ITEMS(Material.WATER_BUCKET, true, false),
    ENTITIES(Material.LEAD, true, false),
    TELEPORT(Material.ENDER_PEARL, true, false),
    CLAIM_CHUNKS(Material.GRASS_BLOCK, false, true),
    UNCLAIM_CHUNKS(Material.DIRT, false, true),
    MANAGE_SETTINGS(Material.COMPARATOR, false, true),
    MANAGE_MEMBERS(Material.PLAYER_HEAD, false, true),
    BUY_UPGRADES(Material.NETHER_STAR, false, true),
    RENAME(Material.NAME_TAG, false, true),
    SET_HOME(Material.RED_BED, false, true);

    private final Material icon;
    private final boolean defaultValue;
    private final boolean management;

    MemberPermission(Material icon, boolean defaultValue, boolean management) {
        this.icon = icon;
        this.defaultValue = defaultValue;
        this.management = management;
    }

    public Material icon() {
        return icon;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    /** Yönetim izni: sadece sahip/admin verebilir. */
    public boolean isManagement() {
        return management;
    }

    public static Set<MemberPermission> defaults() {
        EnumSet<MemberPermission> set = EnumSet.noneOf(MemberPermission.class);
        for (MemberPermission p : values()) if (p.defaultValue) set.add(p);
        return set;
    }

    /** Ziyaretçi flag'inin üye izni karşılığı; null ise üyeler için her zaman serbest. */
    public static MemberPermission forFlag(ClaimFlag flag) {
        return switch (flag) {
            case BUILD -> BUILD;
            case BREAK -> BREAK;
            case INTERACT -> INTERACT;
            case CONTAINERS -> CONTAINERS;
            case USE_ITEMS -> USE_ITEMS;
            case ENTITY_INTERACT, ANIMAL_DAMAGE -> ENTITIES;
            case PVP, ITEM_PICKUP, ITEM_DROP, ENTER -> null;
        };
    }
}
