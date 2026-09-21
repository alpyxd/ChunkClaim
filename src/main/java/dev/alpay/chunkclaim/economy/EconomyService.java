package dev.alpay.chunkclaim.economy;

import dev.alpay.chunkclaim.config.Messages;
import dev.alpay.chunkclaim.config.Settings;
import dev.alpay.chunkclaim.config.UpgradeLevel;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

/** Config'e göre uygun EconomyProvider'ı seçer. */
public class EconomyService {

    private final JavaPlugin plugin;
    private final Settings settings;
    private final Supplier<Messages> messages;
    private EconomyProvider provider;

    public EconomyService(JavaPlugin plugin, Settings settings, Supplier<Messages> messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        setup();
    }

    public void setup() {
        Settings.EconomyType type = settings.economyType();
        EconomyProvider vault = (type == Settings.EconomyType.VAULT || type == Settings.EconomyType.AUTO) ? hookVault() : null;

        if (vault != null) {
            provider = vault;
        } else {
            if (type == Settings.EconomyType.VAULT) {
                plugin.getLogger().warning("economy.type is VAULT but no Vault economy was found — falling back to diamonds.");
            }
            provider = new DiamondEconomyProvider(settings.diamondAcceptBlocks(), messages);
        }
        plugin.getLogger().info("Economy provider: " + provider.name());
    }

    private EconomyProvider hookVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null;
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) return null;
            return new VaultEconomyProvider(rsp.getProvider());
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    public EconomyProvider provider() {
        return provider;
    }

    /** Aktif sağlayıcıya göre chunk fiyatı. */
    public double chunkPrice(int currentChunks) {
        return settings.chunkPrice(currentChunks, provider.usesDiamonds());
    }

    /** Aktif sağlayıcıya göre geliştirme seviyesi fiyatı. */
    public double upgradeCost(UpgradeLevel level) {
        return settings.upgradeCost(level, provider.usesDiamonds());
    }
}
