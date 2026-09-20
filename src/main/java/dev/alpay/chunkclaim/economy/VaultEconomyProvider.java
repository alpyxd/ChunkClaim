package dev.alpay.chunkclaim.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

/** Vault üzerinden ekonomi. */
public class VaultEconomyProvider implements EconomyProvider {

    private final Economy economy;

    public VaultEconomyProvider(Economy economy) {
        this.economy = economy;
    }

    @Override
    public String name() {
        return "Vault (" + economy.getName() + ")";
    }

    @Override
    public boolean usesDiamonds() {
        return false;
    }

    @Override
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(Player player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (amount <= 0) return true;
        EconomyResponse r = economy.withdrawPlayer(player, amount);
        return r.transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (amount <= 0) return true;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}
