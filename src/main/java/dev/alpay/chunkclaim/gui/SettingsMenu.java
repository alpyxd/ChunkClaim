package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.ClaimFlag;
import dev.alpay.chunkclaim.claim.MemberPermission;
import dev.alpay.chunkclaim.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

/** Ziyaretçi izinleri: her flag için aç/kapa. */
public class SettingsMenu extends Menu {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    public SettingsMenu(ChunkClaimPlugin plugin, Player player, Claim claim) {
        super(plugin, player, claim);
    }

    @Override
    protected String section() {
        return "settings";
    }

    @Override
    protected int size() {
        return 36;
    }

    @Override
    protected void build() {
        set(4, ItemBuilder.of(Material.OAK_SIGN)
                .name(t("header-name"))
                .lore(tl("header-lore"))
                .build());

        ClaimFlag[] flags = ClaimFlag.values();
        for (int i = 0; i < flags.length && i < SLOTS.length; i++) {
            ClaimFlag flag = flags[i];
            boolean on = claim.getFlag(flag);
            Map<String, String> ph = Map.of("flag", msg().flagName(flag), "state", msg().state(on));
            set(SLOTS[i], ItemBuilder.of(flag.icon())
                    .placeholders(ph)
                    .name(t(on ? "flag-name-on" : "flag-name-off"))
                    .lore(tl("flag-lore"))
                    .lore(common(on ? "click-turn-off" : "click-turn-on"))
                    .glow(on)
                    .hideAttributes()
                    .build(), click -> {
                if (!plugin.claims().exists(claim)) return;
                if (!plugin.claims().require(player, claim, MemberPermission.MANAGE_SETTINGS)) return;
                boolean v = claim.toggleFlag(flag);
                plugin.claims().save(claim);
                msg().send(player, "flag-toggled", Map.of("flag", msg().flagName(flag), "state", msg().state(v)));
                refresh();
            });
        }

        backButton(31, () -> plugin.menus().openMain(player, claim));
        fill(Material.GRAY_STAINED_GLASS_PANE);
    }
}
