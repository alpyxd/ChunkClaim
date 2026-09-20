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

/** Ana yönetim menüsü: alt menülere geçiş ve hızlı bilgi. */
public class MainMenu extends Menu {

    public MainMenu(ChunkClaimPlugin plugin, Player player, Claim claim) {
        super(plugin, player, claim);
    }

    @Override
    protected String section() {
        return "main";
    }

    @Override
    protected int size() {
        return 27;
    }

    @Override
    protected void build() {
        Map<String, String> ph = new HashMap<>(plugin.claims().placeholders(claim));
        ph.put("economy", plugin.economy().provider().name());
        boolean canSettings = plugin.claims().has(player, claim, MemberPermission.MANAGE_SETTINGS);
        boolean canUpgrade = plugin.claims().has(player, claim, MemberPermission.BUY_UPGRADES);
        boolean canRename = plugin.claims().has(player, claim, MemberPermission.RENAME);

        set(4, ItemBuilder.of(plugin.settings().claimBlockMaterial())
                .placeholders(ph)
                .name(t("info-name"))
                .lore(tl("info-lore"))
                .glow(true)
                .build());

        set(10, ItemBuilder.of(Material.COMPARATOR)
                .placeholders(ph)
                .name(t("settings-name"))
                .lore(tl("settings-lore"))
                .lore(canSettings ? common("click-open") : common("no-permission"))
                .build(), c -> {
            if (!plugin.claims().require(player, claim, MemberPermission.MANAGE_SETTINGS)) return;
            new SettingsMenu(plugin, player, claim).open();
        });

        set(12, ItemBuilder.of(Material.NETHER_STAR)
                .placeholders(ph)
                .name(t("upgrades-name"))
                .lore(tl("upgrades-lore"))
                .lore(canUpgrade ? common("click-open") : common("no-permission"))
                .build(), c -> {
            if (!plugin.claims().require(player, claim, MemberPermission.BUY_UPGRADES)) return;
            new UpgradesMenu(plugin, player, claim).open();
        });

        set(14, ItemBuilder.of(Material.PLAYER_HEAD)
                .placeholders(ph)
                .name(t("members-name"))
                .lore(tl("members-lore"))
                .skull(claim.getOwner())
                .build(), c -> new MembersMenu(plugin, player, claim).open());

        set(16, ItemBuilder.of(Material.FILLED_MAP)
                .placeholders(ph)
                .name(t("chunks-name"))
                .lore(tl("chunks-lore"))
                .build(), c -> new ChunksMenu(plugin, player, claim).open());

        set(20, ItemBuilder.of(Material.BEACON)
                .placeholders(ph)
                .name(t("border-name"))
                .lore(tl("border-lore"))
                .build(), c -> {
            player.closeInventory();
            msg().send(player, "border-shown", ph);
            plugin.border().show(player, claim);
        });

        set(22, ItemBuilder.of(Material.NAME_TAG)
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

        boolean canTp = plugin.claims().has(player, claim, MemberPermission.TELEPORT);
        boolean canSetHome = plugin.claims().has(player, claim, MemberPermission.SET_HOME);
        Map<String, String> tpPh = new HashMap<>(ph);
        tpPh.put("home", claim.getCustomHome() != null ? t("teleport-home-custom") : t("teleport-home-block"));
        tpPh.put("warmup", String.valueOf(plugin.settings().teleportWarmup()));
        ItemBuilder tp = ItemBuilder.of(Material.ENDER_PEARL)
                .placeholders(tpPh)
                .name(t("teleport-name"))
                .lore(tl("teleport-lore"))
                .lore(canTp ? t("teleport-click") : t("teleport-no-permission"));
        if (canSetHome) tp.lore(t("teleport-sethome"));
        set(8, tp.build(), c -> {
            if (c.isShiftClick()) {
                plugin.teleports().setHome(player, claim);
                refresh();
                return;
            }
            player.closeInventory();
            plugin.teleports().teleport(player, claim);
        });

        Map<String, String> protPh = Map.of(
                "explosion", msg().state(plugin.claims().hasUpgrade(claim, UpgradeType.EXPLOSION_PROTECTION)),
                "fire", msg().state(plugin.claims().hasUpgrade(claim, UpgradeType.FIRE_PROTECTION)),
                "mob", msg().state(plugin.claims().hasUpgrade(claim, UpgradeType.MOB_SPAWN_BLOCK)));
        set(24, ItemBuilder.of(Material.BOOK)
                .placeholders(protPh)
                .name(t("protections-name"))
                .lore(tl("protections-lore"))
                .build());

        if (plugin.claims().canManage(player, claim)) {
            set(18, ItemBuilder.of(Material.TNT)
                    .name(t("delete-name"))
                    .lore(tl("delete-lore"))
                    .build(), c -> new DeleteConfirmMenu(plugin, player, claim).open());
        }

        closeButton(26);
        fill(Material.GRAY_STAINED_GLASS_PANE);
    }
}
