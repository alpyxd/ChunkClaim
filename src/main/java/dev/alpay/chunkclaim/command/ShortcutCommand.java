package dev.alpay.chunkclaim.command;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tek kelimelik kısayol komutları (/border, /chome). plugin.yml yerine runtime'da kaydedilir;
 * böylece alias'lar ve açıklamalar dil dosyasından (commands.&lt;key&gt;) gelir.
 * <p>
 * Paper'ın Brigadier tabanlı komut haritası çalışma anında komut kaldırmayı desteklemez;
 * bu yüzden kayıt yalnızca eklenti açılışında yapılır. /claim reload sonrası alias seti
 * değişmişse konsola "yeniden başlatma gerekli" uyarısı yazılır.
 */
public class ShortcutCommand extends Command {

    private static final String PREFIX = "chunkclaim";
    /** name → kayıtlı alias listesi (değişiklik tespiti için). */
    private static final Map<String, List<String>> REGISTERED = new HashMap<>();

    private final ChunkClaimPlugin plugin;
    private final Consumer<Player> action;

    private ShortcutCommand(ChunkClaimPlugin plugin, String name, String description, List<String> aliases, Consumer<Player> action) {
        super(name, description, "/" + name, aliases);
        this.plugin = plugin;
        this.action = action;
    }

    private static List<String> aliasesFromLang(ChunkClaimPlugin plugin, String name, String langKey) {
        List<String> aliases = new ArrayList<>();
        for (String a : plugin.messages().list("commands." + langKey + ".aliases")) {
            String alias = a.trim().toLowerCase();
            if (!alias.isEmpty() && !alias.equals(name) && !aliases.contains(alias)) aliases.add(alias);
        }
        return aliases;
    }

    /**
     * Dil dosyasındaki commands.&lt;key&gt; bölümünden bir kısayol kaydeder.
     * Zaten kayıtlıysa yeniden kaydetmez; alias'lar değiştiyse uyarır.
     */
    public static void register(ChunkClaimPlugin plugin, String name, String langKey, Consumer<Player> action) {
        List<String> aliases = aliasesFromLang(plugin, name, langKey);
        List<String> existing = REGISTERED.get(name);
        if (existing != null) {
            if (!existing.equals(aliases)) {
                plugin.getLogger().warning("/" + name + " aliases changed (" + existing + " -> " + aliases
                        + "). Command aliases are applied on server restart.");
            }
            return;
        }
        String description = plugin.messages().raw("commands." + langKey + ".description");
        ShortcutCommand cmd = new ShortcutCommand(plugin, name, description, aliases, action);
        Bukkit.getCommandMap().register(PREFIX, cmd);
        REGISTERED.put(name, aliases);
    }

    /** Oyuncuların tab tamamlama listesini yeniler. */
    public static void syncClients() {
        Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("chunkclaim.use")) {
            plugin.messages().send(player, "no-permission");
            return true;
        }
        action.accept(player);
        return true;
    }
}
