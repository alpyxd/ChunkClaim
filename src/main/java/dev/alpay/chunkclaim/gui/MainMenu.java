package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.MemberPermission;
import dev.alpay.chunkclaim.claim.UpgradeType;
import dev.alpay.chunkclaim.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Ana yönetim menüsü (5 satır):
 * <pre>
 *  0: [ ][ ][ ][ ][info][ ][ ][ ][ ]
 *  1: [ ][Ayarlar][ ][Korumalar][ ][Geliştirmeler][ ][Üyeler][ ]   ← yönetim
 *  2: [ ][ ][Chunklar][ ][Sınır][ ][Işınlan][ ][ ]                  ← alan
 *  3: [ ][ ][İsim][ ][Bloğu taşı][ ][Sil][ ][ ]                     ← sahip işlemleri
 *  4: [ ][ ][ ][ ][Kapat][ ][ ][ ][ ]
 * </pre>
 */
public class MainMenu extends Menu {

    private static final int SLOT_INFO = 4;
    private static final int SLOT_SETTINGS = 10, SLOT_PROTECTIONS = 12, SLOT_UPGRADES = 14, SLOT_MEMBERS = 16;
    private static final int SLOT_CHUNKS = 20, SLOT_BORDER = 22, SLOT_TELEPORT = 24;
    private static final int SLOT_RENAME = 29, SLOT_MOVE = 31, SLOT_DELETE = 33;
    private static final int SLOT_CLOSE = 40;

    public MainMenu(ChunkClaimPlugin plugin, Player player, Claim claim) {
        super(plugin, player, claim);
    }

    @Override
    protected String section() {
        return "main";
    }

    @Override
    protected int size() {
        return 45;
    }

    @Override
    protected void build() {
        Map<String, String> ph = new HashMap<>(plugin.claims().placeholders(claim));
        ph.put("economy", plugin.economy().provider().name());
        boolean owner = plugin.claims().canManage(player, claim);
        boolean canSettings = plugin.claims().has(player, claim, MemberPermission.MANAGE_SETTINGS);
        boolean canUpgrade = plugin.claims().has(player, claim, MemberPermission.BUY_UPGRADES);
        boolean canRename = plugin.claims().has(player, claim, MemberPermission.RENAME);
        boolean canTp = plugin.claims().has(player, claim, MemberPermission.TELEPORT);
        boolean canSetHome = plugin.claims().has(player, claim, MemberPermission.SET_HOME);

        // ---- Satır 0: bilgi ----
        set(SLOT_INFO, ItemBuilder.of(plugin.settings().claimBlockMaterial())
                .placeholders(ph)
                .name(t("info-name"))
                .lore(tl("info-lore"))
                .glow(true)
                .build());

        // ---- Satır 1: yönetim ----
        set(SLOT_SETTINGS, ItemBuilder.of(Material.COMPARATOR)
                .placeholders(ph)
                .name(t("settings-name"))
                .lore(tl("settings-lore"))
                .lore(canSettings ? common("click-open") : common("no-permission"))
                .build(), c -> {
            if (!plugin.claims().require(player, claim, MemberPermission.MANAGE_SETTINGS)) return;
            new SettingsMenu(plugin, player, claim).open();
        });

        Map<String, String> protPh = new HashMap<>(ph);
        protPh.put("explosion", msg().state(plugin.claims().hasUpgrade(claim, UpgradeType.EXPLOSION_PROTECTION)));
        protPh.put("fire", msg().state(plugin.claims().hasUpgrade(claim, UpgradeType.FIRE_PROTECTION)));
        protPh.put("mob", msg().state(plugin.claims().hasUpgrade(claim, UpgradeType.MOB_SPAWN_BLOCK)));
        set(SLOT_PROTECTIONS, ItemBuilder.of(Material.SHIELD)
                .placeholders(protPh)
                .name(t("protections-name"))
                .lore(tl("protections-lore"))
                .lore(common("click-open"))
                .hideAttributes()
                .build(), c -> new ProtectionsMenu(plugin, player, claim).open());

        set(SLOT_UPGRADES, ItemBuilder.of(Material.NETHER_STAR)
                .placeholders(ph)
                .name(t("upgrades-name"))
                .lore(tl("upgrades-lore"))
                .lore(canUpgrade ? common("click-open") : common("no-permission"))
                .build(), c -> {
            if (!plugin.claims().require(player, claim, MemberPermission.BUY_UPGRADES)) return;
            new UpgradesMenu(plugin, player, claim).open();
        });

        set(SLOT_MEMBERS, ItemBuilder.of(Material.PLAYER_HEAD)
                .placeholders(ph)
                .name(t("members-name"))
                .lore(tl("members-lore"))
                .skull(claim.getOwner())
                .build(), c -> new MembersMenu(plugin, player, claim).open());

        // ---- Satır 2: alan ----
        set(SLOT_CHUNKS, ItemBuilder.of(Material.FILLED_MAP)
                .placeholders(ph)
                .name(t("chunks-name"))
                .lore(tl("chunks-lore"))
                .build(), c -> new ChunksMenu(plugin, player, claim).open());

        set(SLOT_BORDER, ItemBuilder.of(Material.BEACON)
                .placeholders(ph)
                .name(t("border-name"))
                .lore(tl("border-lore"))
                .build(), c -> {
            player.closeInventory();
            msg().send(player, "border-shown", ph);
            plugin.border().show(player, claim);
        });

        Map<String, String> tpPh = new HashMap<>(ph);
        tpPh.put("home", claim.getCustomHome() != null ? t("teleport-home-custom") : t("teleport-home-block"));
        tpPh.put("warmup", String.valueOf(plugin.settings().teleportWarmup()));
        ItemBuilder tp = ItemBuilder.of(Material.ENDER_PEARL)
                .placeholders(tpPh)
                .name(t("teleport-name"))
                .lore(tl("teleport-lore"))
                .lore(canTp ? t("teleport-click") : t("teleport-no-permission"));
        if (canSetHome) tp.lore(t("teleport-sethome"));
        set(SLOT_TELEPORT, tp.build(), c -> {
            if (c.isShiftClick()) {
                plugin.teleports().setHome(player, claim);
                refresh();
                return;
            }
            player.closeInventory();
            plugin.teleports().teleport(player, claim);
        });

        // ---- Satır 3: sahip işlemleri ----
        set(SLOT_RENAME, ItemBuilder.of(Material.NAME_TAG)
                .placeholders(ph)
                .name(t("rename-name"))
                .lore(tl("rename-lore"))
                .lore(canRename ? t("rename-click") : common("no-permission"))
                .build(), c -> {
            if (!plugin.claims().require(player, claim, MemberPermission.RENAME)) return;
            msg().send(player, "name-prompt");
            plugin.chatPrompt().ask(player, answer -> {
                if (answer != null) plugin.claims().rename(player, claim, answer);
                if (plugin.claims().exists(claim)) plugin.menus().openMain(player, claim);
            });
        });

        if (owner) {
            set(SLOT_MOVE, ItemBuilder.of(Material.PISTON)
                    .name(t("move-name"))
                    .lore(tl("move-lore"))
                    .build(), c -> {
                player.closeInventory();
                plugin.claims().moveBlockHere(player, claim);
            });

            set(SLOT_DELETE, ItemBuilder.of(Material.TNT)
                    .name(t("delete-name"))
                    .lore(tl("delete-lore"))
                    .build(), c -> new DeleteConfirmMenu(plugin, player, claim).open());
        }

        // ---- Satır 4 ----
        closeButton(SLOT_CLOSE);
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        fill(Material.GRAY_STAINED_GLASS_PANE);
    }
}
