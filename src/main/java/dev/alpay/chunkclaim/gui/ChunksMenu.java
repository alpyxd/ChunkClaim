package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.ChunkKey;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.MemberPermission;
import dev.alpay.chunkclaim.claim.UpgradeType;
import dev.alpay.chunkclaim.config.Messages;
import dev.alpay.chunkclaim.economy.EconomyProvider;
import dev.alpay.chunkclaim.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Chunk haritası: oyuncunun bulunduğu chunk merkezde, 9x5'lik alan.
 * Yeşil = senin, kırmızı = başkasının, gri = boş, sarı = claim edilebilir (komşu).
 */
public class ChunksMenu extends Menu {

    private static final int COLS = 9;
    private static final int ROWS = 5;

    public ChunksMenu(ChunkClaimPlugin plugin, Player player, Claim claim) {
        super(plugin, player, claim);
    }

    @Override
    protected String section() {
        return "chunks";
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected void build() {
        boolean canClaim = plugin.claims().has(player, claim, MemberPermission.CLAIM_CHUNKS);
        boolean canUnclaim = plugin.claims().has(player, claim, MemberPermission.UNCLAIM_CHUNKS);
        boolean isOwner = claim.isOwner(player.getUniqueId());
        EconomyProvider eco = plugin.economy().provider();
        int max = plugin.settings().upgradeValue(claim, UpgradeType.MAX_CHUNKS);
        ChunkKey center = ChunkKey.of(player.getLocation());
        boolean sameWorld = center.world().equals(claim.getOriginChunk().world());

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = row * COLS + col;
                ChunkKey key = new ChunkKey(center.world(), center.x() + (col - COLS / 2), center.z() + (row - ROWS / 2));
                boolean isCenter = key.equals(center);
                Claim at = plugin.claims().getClaimAt(key);

                Map<String, String> ph = new HashMap<>();
                ph.put("x", String.valueOf(key.x()));
                ph.put("z", String.valueOf(key.z()));
                ph.put("chunks", String.valueOf(claim.getChunkCount()));
                ph.put("max_chunks", String.valueOf(max));

                Material mat;
                String name;
                ItemBuilder item = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).placeholders(ph);
                Runnable action = null;
                boolean needsShift = false;

                if (at != null && at.getId().equals(claim.getId())) {
                    boolean origin = key.equals(claim.getOriginChunk());
                    mat = origin ? Material.EMERALD_BLOCK : Material.LIME_STAINED_GLASS_PANE;
                    name = t(origin ? "origin" : "own");
                    item.lore(t("coord"));
                    if (!origin && canUnclaim) {
                        if (isOwner) {
                            double refund = plugin.economy().chunkPrice(claim.getChunkCount() - 1) * plugin.settings().unclaimRefund();
                            ph.put("refund", eco.format(refund));
                            item.lore(t("refund"));
                        }
                        item.lore("", t("unclaim-click"));
                        needsShift = true;
                        action = () -> {
                            plugin.claims().unclaimChunk(player, claim, key);
                            refresh();
                        };
                    }
                } else if (at != null) {
                    mat = Material.RED_STAINED_GLASS_PANE;
                    ph.put("name", Messages.mm().escapeTags(at.getName()));
                    ph.put("owner", Messages.mm().escapeTags(plugin.claims().ownerName(at)));
                    name = t("other");
                    item.lore(t("coord"), t("other-owner"));
                } else if (sameWorld && claim.isAdjacentToClaim(key)) {
                    mat = Material.YELLOW_STAINED_GLASS_PANE;
                    name = t("claimable");
                    ph.put("cost", eco.format(plugin.economy().chunkPrice(claim.getChunkCount())));
                    item.lore(t("coord"), t("price"), t("limit"), "");
                    if (!canClaim) item.lore(common("no-permission"));
                    else if (claim.getChunkCount() >= max) item.lore(t("limit-full"));
                    else item.lore(t("claim-click"));
                    if (canClaim) action = () -> {
                        plugin.claims().claimChunk(player, claim, key);
                        refresh();
                    };
                } else {
                    mat = Material.GRAY_STAINED_GLASS_PANE;
                    name = t("empty");
                    item.lore(t("coord"));
                }
                if (isCenter) item.lore("", t("you-are-here"));

                Runnable finalAction = action;
                boolean finalShift = needsShift;
                set(slot, new ItemBuilder(mat).placeholders(ph).name(name).loreComponents(item.buildLore()).glow(isCenter).build(), click -> {
                    if (finalAction == null) return;
                    if (finalShift && !click.isShiftClick()) return;
                    finalAction.run();
                });
            }
        }

        Map<String, String> ph = new HashMap<>(plugin.claims().placeholders(claim));
        ph.put("cost", eco.format(plugin.economy().chunkPrice(claim.getChunkCount())));
        ph.put("balance", eco.format(eco.getBalance(player)));

        set(47, ItemBuilder.of(Material.BEACON)
                .name(t("border-name"))
                .lore(tl("border-lore"))
                .build(), c -> {
            player.closeInventory();
            msg().send(player, "border-shown", ph);
            plugin.border().show(player, claim);
        });

        set(49, ItemBuilder.of(Material.FILLED_MAP)
                .placeholders(ph)
                .name(t("info-name"))
                .lore(tl("info-lore"))
                .build());

        set(51, ItemBuilder.of(Material.COMPASS)
                .name(t("refresh-name"))
                .lore(tl("refresh-lore"))
                .build(), c -> refresh());

        backButton(45, () -> plugin.menus().openMain(player, claim));
        closeButton(53);
        fill(Material.BLACK_STAINED_GLASS_PANE);
    }
}
