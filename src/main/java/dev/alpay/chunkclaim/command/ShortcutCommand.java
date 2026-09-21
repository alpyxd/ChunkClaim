package dev.alpay.chunkclaim.command;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tek kelimelik kısayol komutları (/border, /chome). plugin.yml yerine runtime'da kaydedilir;
 * böylece alias'lar ve açıklamalar dil dosyasından (commands.&lt;key&gt;) gelir ve reload ile değişir.
 */
public class ShortcutCommand extends Command {

    private static final String PREFIX = "chunkclaim";
    private static final List<ShortcutCommand> REGISTERED = new ArrayList<>();

    private final ChunkClaimPlugin plugin;
    private final Consumer<Player> action;

    private ShortcutCommand(ChunkClaimPlugin plugin, String name, String description, List<String> aliases, Consumer<Player> action) {
        super(name, description, "/" + name, aliases);
        this.plugin = plugin;
        this.action = action;
    }

    /** Dil dosyasındaki commands.&lt;key&gt; bölümünden bir kısayol kaydeder. */
    public static void register(ChunkClaimPlugin plugin, String name, String langKey, Consumer<Player> action) {
        String description = plugin.messages().raw("commands." + langKey + ".description");
        List<String> aliases = new ArrayList<>();
        for (String a : plugin.messages().list("commands." + langKey + ".aliases")) {
            String alias = a.trim().toLowerCase();
            if (!alias.isEmpty() && !alias.equals(name)) aliases.add(alias);
        }
        ShortcutCommand cmd = new ShortcutCommand(plugin, name, description, aliases, action);
        Bukkit.getCommandMap().register(PREFIX, cmd);
        REGISTERED.add(cmd);
    }

    /** Tüm kısayolları kaldırır (reload / disable). */
    public static void unregisterAll() {
        CommandMap map = Bukkit.getCommandMap();
        for (ShortcutCommand cmd : REGISTERED) {
            cmd.unregister(map);
            map.getKnownCommands().entrySet().removeIf(e -> e.getValue() == cmd);
        }
        REGISTERED.clear();
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
