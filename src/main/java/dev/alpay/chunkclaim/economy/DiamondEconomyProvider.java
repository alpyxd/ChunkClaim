package dev.alpay.chunkclaim.economy;

import dev.alpay.chunkclaim.config.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Elmas tabanlı ekonomi. 1 birim = 1 elmas. Fiyatlar yukarı yuvarlanır.
 * accept-blocks açıksa elmas blokları 9 elmas olarak sayılır ve gerekirse bozdurulur.
 */
public class DiamondEconomyProvider implements EconomyProvider {

    private final boolean acceptBlocks;
    private final Supplier<Messages> messages;

    public DiamondEconomyProvider(boolean acceptBlocks, Supplier<Messages> messages) {
        this.acceptBlocks = acceptBlocks;
        this.messages = messages;
    }

    @Override
    public String name() {
        return messages.get().raw("economy.diamond-name");
    }

    @Override
    public boolean usesDiamonds() {
        return true;
    }

    private int count(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null) continue;
            if (item.getType() == Material.DIAMOND) total += item.getAmount();
            else if (acceptBlocks && item.getType() == Material.DIAMOND_BLOCK) total += item.getAmount() * 9;
        }
        return total;
    }

    @Override
    public double getBalance(Player player) {
        return count(player);
    }

    @Override
    public boolean has(Player player, double amount) {
        return count(player) >= (int) Math.ceil(amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        int need = (int) Math.ceil(amount);
        if (need <= 0) return true;
        if (count(player) < need) return false;

        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getStorageContents();

        // Önce tekil elmaslar
        for (int i = 0; i < contents.length && need > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != Material.DIAMOND) continue;
            int take = Math.min(need, item.getAmount());
            item.setAmount(item.getAmount() - take);
            need -= take;
            if (item.getAmount() <= 0) contents[i] = null;
        }
        // Sonra bloklar (bozdurarak, kalan elmaslar para üstü olarak geri verilir)
        int change = 0;
        if (need > 0 && acceptBlocks) {
            for (int i = 0; i < contents.length && need > 0; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType() != Material.DIAMOND_BLOCK) continue;
                int blocksNeeded = (int) Math.ceil(need / 9.0);
                int take = Math.min(blocksNeeded, item.getAmount());
                item.setAmount(item.getAmount() - take);
                if (item.getAmount() <= 0) contents[i] = null;
                int gained = take * 9;
                change += Math.max(0, gained - need);
                need = Math.max(0, need - gained);
            }
        }
        // Önce envanteri yaz, SONRA para üstünü ver (aksi hâlde eski dizi para üstünü ezer)
        inv.setStorageContents(contents);
        if (change > 0) giveDiamonds(player, change);
        return need == 0;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        int give = (int) Math.floor(amount);
        if (give <= 0) return true;
        giveDiamonds(player, give);
        return true;
    }

    private void giveDiamonds(Player player, int amount) {
        while (amount > 0) {
            int stack = Math.min(64, amount);
            Map<Integer, ItemStack> left = new HashMap<>(player.getInventory().addItem(new ItemStack(Material.DIAMOND, stack)));
            for (ItemStack leftover : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            amount -= stack;
        }
    }

    @Override
    public String format(double amount) {
        return messages.get().raw("economy.diamond-format", Map.of("amount", String.valueOf((int) Math.ceil(amount))));
    }
}
