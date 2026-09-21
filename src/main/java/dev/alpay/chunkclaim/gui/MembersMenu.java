package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.MemberPermission;
import dev.alpay.chunkclaim.claim.UpgradeType;
import dev.alpay.chunkclaim.config.Messages;
import dev.alpay.chunkclaim.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Üye listesi: ekle / izinleri düzenle / çıkar. */
public class MembersMenu extends Menu {

    public MembersMenu(ChunkClaimPlugin plugin, Player player, Claim claim) {
        super(plugin, player, claim);
    }

    @Override
    protected String section() {
        return "members";
    }

    @Override
    protected int size() {
        return 54;
    }

    static String nameOf(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return Messages.mm().escapeTags(op.getName() == null ? uuid.toString().substring(0, 8) : op.getName());
    }

    @Override
    protected void build() {
        boolean manage = plugin.claims().has(player, claim, MemberPermission.MANAGE_MEMBERS);
        int max = plugin.settings().upgradeValue(claim, UpgradeType.MAX_MEMBERS);

        set(0, ItemBuilder.of(Material.PLAYER_HEAD)
                .placeholders(Map.of("player", nameOf(claim.getOwner())))
                .name(t("owner-name"))
                .lore(tl("owner-lore"))
                .skull(claim.getOwner())
                .glow(true)
                .build());

        int slot = 1;
        for (UUID member : claim.getMembers()) {
            if (slot >= 45) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(member);
            Map<String, String> ph = new HashMap<>();
            ph.put("player", nameOf(member));
            ph.put("status", common(op.isOnline() ? "online" : "offline"));
            ph.put("perms", String.valueOf(claim.getMemberPermissions(member).size()));
            ph.put("max_perms", String.valueOf(MemberPermission.values().length));

            ItemBuilder head = ItemBuilder.of(Material.PLAYER_HEAD)
                    .placeholders(ph)
                    .name(t("member-name"))
                    .lore(tl("member-lore"));
            if (manage) head.lore(tl("member-manage-lore"));
            set(slot++, head.skull(member).build(), click -> {
                if (!manage) return;
                if (click.isShiftClick()) {
                    plugin.claims().removeMember(player, claim, member);
                    refresh();
                } else {
                    new MemberPermissionsMenu(plugin, player, claim, member).open();
                }
            });
        }

        Map<String, String> addPh = Map.of("members", String.valueOf(claim.getMembers().size()), "max_members", String.valueOf(max));
        set(49, ItemBuilder.of(Material.LIME_DYE)
                .placeholders(addPh)
                .name(t("add-name"))
                .lore(tl("add-lore"))
                .lore(manage ? t("add-click") : common("no-permission"))
                .build(), click -> {
            if (!plugin.claims().require(player, claim, MemberPermission.MANAGE_MEMBERS)) return;
            if (claim.getMembers().size() >= max) {
                msg().send(player, "member-limit-reached", Map.of("max", String.valueOf(max)));
                return;
            }
            msg().send(player, "member-add-prompt");
            plugin.chatPrompt().ask(player, answer -> {
                if (answer == null) {
                    msg().send(player, "member-add-cancelled");
                } else {
                    // getPlayer(String) kısmi eşleşme yapar → yanlış oyuncu; tam isim şart
                    OfflinePlayer target = Bukkit.getPlayerExact(answer);
                    if (target == null) target = Bukkit.getOfflinePlayerIfCached(answer);
                    if (target == null) {
                        msg().send(player, "player-not-found", Map.of("player", Messages.mm().escapeTags(answer)));
                    } else {
                        plugin.claims().addMember(player, claim, target);
                    }
                }
                if (plugin.claims().exists(claim)) new MembersMenu(plugin, player, claim).open();
            });
        });

        backButton(45, () -> plugin.menus().openMain(player, claim));
        closeButton(53);
        fill(Material.GRAY_STAINED_GLASS_PANE);
    }
}
