package dev.alpay.chunkclaim.command;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/** Tek kelimelik kısayol komutları: /border, /chome. */
public class ShortcutCommand implements CommandExecutor {

    private final ChunkClaimPlugin plugin;
    private final Consumer<Player> action;

    public ShortcutCommand(ChunkClaimPlugin plugin, Consumer<Player> action) {
        this.plugin = plugin;
        this.action = action;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
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
