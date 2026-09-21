package dev.alpay.chunkclaim;

import dev.alpay.chunkclaim.border.BorderVisualizer;
import dev.alpay.chunkclaim.claim.ClaimManager;
import dev.alpay.chunkclaim.command.ClaimCommand;
import dev.alpay.chunkclaim.command.ShortcutCommand;
import dev.alpay.chunkclaim.config.Messages;
import dev.alpay.chunkclaim.config.Settings;
import dev.alpay.chunkclaim.economy.EconomyService;
import dev.alpay.chunkclaim.gui.ChatPrompt;
import dev.alpay.chunkclaim.gui.MenuManager;
import dev.alpay.chunkclaim.hologram.HologramManager;
import dev.alpay.chunkclaim.listener.ClaimBlockListener;
import dev.alpay.chunkclaim.listener.MovementListener;
import dev.alpay.chunkclaim.listener.ProtectionListener;
import dev.alpay.chunkclaim.listener.WorldListener;
import dev.alpay.chunkclaim.storage.ClaimStorage;
import dev.alpay.chunkclaim.teleport.TeleportManager;
import dev.alpay.chunkclaim.util.ClaimBlockItem;
import dev.alpay.chunkclaim.util.ClaimCompass;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChunkClaimPlugin extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private EconomyService economy;
    private ClaimStorage storage;
    private ClaimManager claims;
    private HologramManager holograms;
    private BorderVisualizer border;
    private MenuManager menus;
    private ChatPrompt chatPrompt;
    private ClaimBlockItem claimBlockItem;
    private ClaimCompass compass;
    private TeleportManager teleports;
    private ClaimCommand claimCommand;

    @Override
    public void onEnable() {
        settings = new Settings(this);
        messages = new Messages(this, settings.language());
        economy = new EconomyService(this, settings, this::messages);
        storage = new ClaimStorage(this);
        claims = new ClaimManager(this, storage);
        holograms = new HologramManager(this);
        border = new BorderVisualizer(this);
        menus = new MenuManager(this);
        chatPrompt = new ChatPrompt(this);
        claimBlockItem = new ClaimBlockItem(this);
        compass = new ClaimCompass(this);
        teleports = new TeleportManager(this);

        claims.loadAll();
        claimBlockItem.registerRecipe();
        holograms.start();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(menus, this);
        pm.registerEvents(chatPrompt, this);
        pm.registerEvents(teleports, this);
        pm.registerEvents(compass, this);
        pm.registerEvents(new ClaimBlockListener(this), this);
        pm.registerEvents(new ProtectionListener(this), this);
        pm.registerEvents(new MovementListener(this), this);
        pm.registerEvents(new WorldListener(this), this);

        claimCommand = new ClaimCommand(this);
        PluginCommand cmd = getCommand("claim");
        if (cmd != null) {
            cmd.setExecutor(claimCommand);
            cmd.setTabCompleter(claimCommand);
        }
        registerShortcuts();

        getLogger().info("ChunkClaim aktif. Ekonomi: " + economy.provider().name());
    }

    @Override
    public void onDisable() {
        if (teleports != null) teleports.shutdown();
        if (holograms != null) holograms.stop();
        if (border != null) border.clearAll();
        if (claims != null) claims.saveAll();
        if (claimBlockItem != null) claimBlockItem.unregisterRecipe();
    }

    /** Dil dosyasındaki alias'larla /border ve /chome kısayollarını kaydeder (ilk çağrıda; sonrası sadece uyarı). */
    private void registerShortcuts() {
        ShortcutCommand.register(this, "border", "border", claimCommand::showBorder);
        ShortcutCommand.register(this, "chome", "home", claimCommand::home);
        ShortcutCommand.syncClients();
    }

    /** /claim reload */
    public void reloadAll() {
        settings.reload();
        messages.reload(settings.language());
        economy.setup();
        claimBlockItem.registerRecipe();
        holograms.start();
        registerShortcuts();
    }

    public Settings settings() { return settings; }
    public Messages messages() { return messages; }
    public EconomyService economy() { return economy; }
    public ClaimManager claims() { return claims; }
    public HologramManager holograms() { return holograms; }
    public BorderVisualizer border() { return border; }
    public MenuManager menus() { return menus; }
    public ChatPrompt chatPrompt() { return chatPrompt; }
    public ClaimBlockItem claimBlockItem() { return claimBlockItem; }
    public TeleportManager teleports() { return teleports; }
    public ClaimCompass compass() { return compass; }
}
