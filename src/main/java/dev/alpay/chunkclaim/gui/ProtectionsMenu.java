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
 * Satın alınmış korumaları (patlama, yangın, canavar) aç/kapa.
 * Satın alınmamış koruma tıklanınca geliştirme menüsüne yönlendirir.
 */
public class ProtectionsMenu extends Menu {

    private static final int[] SLOTS = {11, 13, 15, 20, 22, 24};

    public ProtectionsMenu(ChunkClaimPlugin plugin, Player player, Claim claim) {
        super(plugin, player, claim);
    }

    @Override
    protected String section() {
        return "protections";
    }

    @Override
    protected int size() {
        return 36;
    }

    @Override
    protected void build() {
        boolean canManage = plugin.claims().has(player, claim, MemberPermission.MANAGE_PROTECTIONS);
        boolean canBuy = plugin.claims().has(player, claim, MemberPermission.BUY_UPGRADES);

        set(4, ItemBuilder.of(Material.SHIELD)
                .name(t("header-name"))
                .lore(tl("header-lore"))
                .hideAttributes()
                .build());

        int i = 0;
        for (UpgradeType type : UpgradeType.values()) {
            if (!type.isToggle() || i >= SLOTS.length) continue;
            boolean purchased = plugin.claims().isPurchased(claim, type);
            boolean enabled = claim.isProtectionEnabled(type);
            boolean active = purchased && enabled;

            Map<String, String> ph = new HashMap<>();
            ph.put("protection", msg().upgradeName(type));
            ph.put("description", msg().upgradeDescription(type));
            ph.put("state", purchased ? msg().state(enabled) : t("not-purchased"));

            ItemBuilder item = ItemBuilder.of(type.icon())
                    .placeholders(ph)
                    .name(t(active ? "item-name-on" : (purchased ? "item-name-off" : "item-name-locked")))
                    .lore(tl("item-lore"));
            if (!purchased) {
                item.lore(canBuy ? t("buy-hint") : common("no-permission"));
            } else if (canManage) {
                item.lore(common(enabled ? "click-turn-off" : "click-turn-on"));
            } else {
                item.lore(common("no-permission"));
            }

            set(SLOTS[i++], item.glow(active).hideAttributes().build(), click -> {
                if (!plugin.claims().isPurchased(claim, type)) {
                    if (plugin.claims().require(player, claim, MemberPermission.BUY_UPGRADES)) {
                        new UpgradesMenu(plugin, player, claim).open();
                    }
                    return;
                }
                if (plugin.claims().toggleProtection(player, claim, type)) refresh();
            });
        }

        backButton(31, () -> plugin.menus().openMain(player, claim));
        fill(Material.GRAY_STAINED_GLASS_PANE);
    }
}
