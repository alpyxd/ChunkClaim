package dev.alpay.chunkclaim.command;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.claim.ChunkKey;
import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.config.Messages;
import dev.alpay.chunkclaim.gui.DeleteConfirmMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /claim komutu. */
public class ClaimCommand implements TabExecutor {

    private final ChunkClaimPlugin plugin;

    public ClaimCommand(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Messages m = plugin.messages();
        String sub = args.length == 0 ? "menu" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("chunkclaim.admin")) { m.send(sender, "no-permission"); return true; }
                plugin.reloadAll();
                m.send(sender, "reloaded");
                return true;
            }
            case "give" -> {
                if (!sender.hasPermission("chunkclaim.admin")) { m.send(sender, "no-permission"); return true; }
                Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player p ? p : null);
                if (target == null) {
                    m.send(sender, "player-not-found", Map.of("player", args.length >= 2 ? args[1] : "?"));
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) { }
                }
                ItemStack item = plugin.claimBlockItem().create(amount);
                target.getInventory().addItem(item).values()
                        .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
                m.send(sender, "claim-block-given", Map.of("amount", String.valueOf(amount), "player", target.getName()));
                if (target != sender) m.send(target, "claim-block-received", Map.of("amount", String.valueOf(amount)));
                return true;
            }
            case "help" -> {
                sender.sendMessage(m.plain("help", Map.of()));
                return true;
            }
            default -> { }
        }

        if (!(sender instanceof Player player)) {
            m.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("chunkclaim.use")) {
            m.send(player, "no-permission");
            return true;
        }

        switch (sub) {
            case "menu" -> {
                Claim claim = plugin.claims().getRelevantClaim(player);
                if (claim == null) { m.send(player, "no-claim"); return true; }
                plugin.menus().openMain(player, claim);
            }
            case "info" -> {
                Claim here = plugin.claims().getClaimAt(player.getLocation());
                if (here == null) { m.send(player, "not-in-claim"); return true; }
                m.send(player, "claim-info", plugin.claims().placeholders(here));
            }
            case "border" -> showBorder(player);
            case "home", "tp", "ev" -> home(player);
            case "sethome", "evayarla" -> {
                Claim claim = plugin.claims().getClaimAt(player.getLocation());
                if (claim == null || !claim.isTrusted(player.getUniqueId()) && !player.hasPermission("chunkclaim.admin")) {
                    m.send(player, "home-outside-claim");
                    return true;
                }
                plugin.teleports().setHome(player, claim);
            }
            case "delete", "sil" -> {
                Claim claim = plugin.claims().getRelevantClaim(player);
                if (claim == null) { m.send(player, "no-claim"); return true; }
                if (!plugin.claims().canManage(player, claim)) { m.send(player, "not-manage-permission"); return true; }
                new DeleteConfirmMenu(plugin, player, claim).open();
            }
            case "claim" -> {
                Claim claim = plugin.claims().getRelevantClaim(player);
                if (claim == null) { m.send(player, "no-claim"); return true; }
                plugin.claims().claimChunk(player, claim, ChunkKey.of(player.getLocation()));
            }
            case "unclaim" -> {
                Claim claim = plugin.claims().getRelevantClaim(player);
                if (claim == null) { m.send(player, "no-claim"); return true; }
                plugin.claims().unclaimChunk(player, claim, ChunkKey.of(player.getLocation()));
            }
            default -> sender.sendMessage(m.plain("help", Map.of()));
        }
        return true;
    }

    /** Bulunduğun claim'in, yoksa kendi/üyesi olduğun claim'in sınırını gösterir. /border ve /claim border. */
    public void showBorder(Player player) {
        Claim claim = plugin.claims().getClaimAt(player.getLocation());
        if (claim == null) claim = plugin.claims().getRelevantClaim(player);
        if (claim == null) { plugin.messages().send(player, "no-claim"); return; }
        plugin.messages().send(player, "border-shown", plugin.claims().placeholders(claim));
        plugin.border().show(player, claim);
    }

    /** Kendi claim'ine (yoksa üyesi olduğun claim'e) ışınlan. /claim home ve /chome. */
    public void home(Player player) {
        Claim claim = plugin.claims().getPrimaryClaim(player.getUniqueId());
        if (claim == null) claim = plugin.claims().getRelevantClaim(player);
        if (claim == null) { plugin.messages().send(player, "no-claim"); return; }
        plugin.teleports().teleport(player, claim);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("menu", "info", "border", "home", "sethome", "claim", "unclaim", "delete", "help"));
            if (sender.hasPermission("chunkclaim.admin")) subs.addAll(List.of("give", "reload"));
            for (String s : subs) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
            }
        }
        return out;
    }
}
