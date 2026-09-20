package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.MemberPermission;
import dev.alpay.chunkclaim.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Tek bir üyenin izinleri. Üst sıra temel izinler, alt sıra yönetim izinleri.
 * Yönetim izinlerini sadece sahip/admin değiştirebilir.
 */
public class MemberPermissionsMenu extends Menu {

    private static final int[] BASIC_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] MANAGEMENT_SLOTS = {28, 29, 30, 31, 32, 33, 34};

    private final UUID member;
    private final String memberName;

    public MemberPermissionsMenu(ChunkClaimPlugin plugin, Player player, Claim claim, UUID member) {
        super(plugin, player, claim);
        this.member = member;
        this.memberName = MembersMenu.nameOf(member);
    }

    @Override
    protected String section() {
        return "permissions";
    }

    @Override
    protected Map<String, String> titlePlaceholders() {
        return Map.of("player", memberName);
    }

    @Override
    protected int size() {
        return 45;
    }

    @Override
    protected void build() {
        boolean owner = plugin.claims().canManage(player, claim);
        Map<String, String> ph = Map.of("player", memberName);

        set(4, ItemBuilder.of(Material.PLAYER_HEAD)
                .placeholders(ph)
                .name(t("header-name"))
                .lore(tl("header-lore"))
                .skull(member)
                .build());

        set(19, ItemBuilder.of(Material.OAK_SIGN).name(t("basic-header")).build());
        set(37, ItemBuilder.of(Material.OAK_SIGN)
                .name(t("management-header"))
                .lore(tl("management-header-lore"))
                .build());

        int b = 0, m = 0;
        for (MemberPermission perm : MemberPermission.values()) {
            int slot;
            if (perm.isManagement()) {
                if (m >= MANAGEMENT_SLOTS.length) continue;
                slot = MANAGEMENT_SLOTS[m++];
            } else {
                if (b >= BASIC_SLOTS.length) continue;
                slot = BASIC_SLOTS[b++];
            }
            boolean on = claim.hasPermission(member, perm);
            boolean editable = !perm.isManagement() || owner;
            Map<String, String> pph = Map.of("permission", msg().permissionName(perm), "state", msg().state(on));

            ItemBuilder item = ItemBuilder.of(perm.icon())
                    .placeholders(pph)
                    .name(t(on ? "perm-name-on" : "perm-name-off"))
                    .lore(tl("perm-lore"))
                    .lore(editable ? common(on ? "click-turn-off" : "click-turn-on") : common("owner-only"))
                    .glow(on)
                    .hideAttributes();
            if (perm == MemberPermission.MANAGE_MEMBERS) item.skull(member);

            set(slot, item.build(), click -> {
                if (plugin.claims().toggleMemberPermission(player, claim, member, perm)) refresh();
            });
        }

        backButton(40, () -> new MembersMenu(plugin, player, claim).open());
        fill(Material.GRAY_STAINED_GLASS_PANE);
    }
}
