package dev.alpay.chunkclaim.gui;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.UpgradeType;
import dev.alpay.chunkclaim.config.Settings;
import dev.alpay.chunkclaim.config.UpgradeLevel;
import dev.alpay.chunkclaim.economy.EconomyProvider;
import dev.alpay.chunkclaim.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/** Alan geliştirmeleri: seviye satın alma. */
public class UpgradesMenu extends Menu {

    private static final int[] SLOTS = {10, 12, 14, 16, 22, 24, 26};

    public UpgradesMenu(ChunkClaimPlugin plugin, Player player, Claim claim) {
        super(plugin, player, claim);
    }

    @Override
    protected String section() {
        return "upgrades";
    }

    @Override
    protected int size() {
        return 36;
    }

    @Override
    protected void build() {
        Settings s = plugin.settings();
        EconomyProvider eco = plugin.economy().provider();

        set(4, ItemBuilder.of(Material.GOLD_INGOT)
                .placeholders(Map.of("balance", eco.format(eco.getBalance(player)), "economy", eco.name()))
                .name(t("balance-name"))
                .lore(tl("balance-lore"))
                .build());

        UpgradeType[] types = UpgradeType.values();
        for (int i = 0; i < types.length && i < SLOTS.length; i++) {
            UpgradeType type = types[i];
            int level = claim.getUpgradeLevel(type);
            int max = s.maxUpgradeLevel(type);
            int value = s.upgradeValue(claim, type);
            UpgradeLevel next = s.nextUpgrade(claim, type);

            Map<String, String> ph = new HashMap<>();
            ph.put("upgrade", msg().upgradeName(type));
            ph.put("state", msg().state(value > 0));
            ph.put("level", String.valueOf(level));
            ph.put("max", String.valueOf(max));
            ph.put("value", String.valueOf(value));

            ItemBuilder item = ItemBuilder.of(type.icon())
                    .placeholders(ph)
                    .name(t("item-name"))
                    .lore("<gray>" + msg().upgradeDescription(type), "");
            if (type.isToggle()) {
                item.lore(t("status"));
            } else {
                item.lore(t("level"), t("current-value"));
            }
            item.lore("");
            if (next == null) {
                item.lore(t("max-level"));
            } else {
                double cost = plugin.economy().upgradeCost(next);
                Map<String, String> nextPh = new HashMap<>(ph);
                nextPh.put("value", String.valueOf(next.value()));
                nextPh.put("cost", eco.format(cost));
                item.placeholders(nextPh);
                if (!type.isToggle()) item.lore(t("next-value"));
                item.lore(t("price"), "");
                item.lore(eco.has(player, cost) ? t("buy") : t("insufficient"));
            }

            set(SLOTS[i], item.amount(type.isToggle() ? 1 : Math.max(1, level + 1))
                    .glow(next == null)
                    .hideAttributes()
                    .build(), click -> {
                if (plugin.claims().purchaseUpgrade(player, claim, type)) refresh();
            });
        }

        backButton(31, () -> plugin.menus().openMain(player, claim));
        fill(Material.GRAY_STAINED_GLASS_PANE);
    }
}
