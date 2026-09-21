package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Claim silme onayı. 1. adım: menüde "Evet" → 2. adım: claim ismini sohbete birebir yaz.
 * Yanlış isim veya iptal → hiçbir şey olmaz.
 */
public class DeleteConfirmMenu extends Menu {

    public DeleteConfirmMenu(ChunkClaimPlugin plugin, Player player, Claim claim) {
        super(plugin, player, claim);
    }

    @Override
    protected String section() {
        return "delete";
    }

    @Override
    protected int size() {
        return 27;
    }

    @Override
    protected void build() {
        Map<String, String> ph = new HashMap<>(plugin.claims().placeholders(claim));
        double refund = claim.isOwner(player.getUniqueId()) ? plugin.claims().deleteRefund(claim) : 0;
        ph.put("refund", plugin.economy().provider().format(refund));

        set(4, ItemBuilder.of(Material.TNT)
                .placeholders(ph)
                .name(t("info-name"))
                .lore(tl("info-lore"))
                .build());

        set(11, ItemBuilder.of(Material.LIME_CONCRETE)
                .name(t("confirm-name"))
                .lore(tl("confirm-lore"))
                .build(), c -> {
            if (!plugin.claims().canManage(player, claim)) {
                msg().send(player, "not-manage-permission");
                return;
            }
            msg().send(player, "delete-confirm-prompt", ph);
            plugin.chatPrompt().ask(player, answer -> {
                if (!plugin.claims().exists(claim)) return;
                if (answer == null) {
                    msg().send(player, "delete-cancelled");
                    plugin.menus().openMain(player, claim);
                    return;
                }
                if (!answer.equals(claim.getName())) {
                    msg().send(player, "delete-name-mismatch", ph);
                    plugin.menus().openMain(player, claim);
                    return;
                }
                plugin.claims().deleteClaimByPlayer(player, claim);
            });
        });

        set(15, ItemBuilder.of(Material.RED_CONCRETE)
                .name(t("cancel-name"))
                .lore(tl("cancel-lore"))
                .build(), c -> plugin.menus().openMain(player, claim));

        fill(Material.BLACK_STAINED_GLASS_PANE);
    }
}
