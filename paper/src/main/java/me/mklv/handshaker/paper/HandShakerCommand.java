package me.mklv.handshaker.paper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class HandShakerCommand implements TabExecutor {
    private final HandShakerPlugin plugin;
    public HandShakerCommand(HandShakerPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/handshaker reload | add <mod> | remove <mod>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.getBlacklistConfig().load();
                sender.sendMessage("HandShaker blacklist reloaded.");
                return true;
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /handshaker remove <mod>");
                    return true;
                }
                boolean removed = plugin.getBlacklistConfig().removeMod(args[1]);
                sender.sendMessage(removed ? "Removed " + args[1] : args[1] + " not found in blacklist.");
                return true;
            }
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /handshaker add <mod>");
                    return true;
                }
                boolean added = plugin.getBlacklistConfig().addMod(args[1]);
                sender.sendMessage(added ? "Added " + args[1] : args[1] + " already in blacklist.");
                return true;
            }
            default -> sender.sendMessage("Unknown subcommand.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "add", "remove");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return new ArrayList<>(plugin.getBlacklistConfig().getBlacklistedMods());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            // Autocomplete from last mod list if sender is a player and has joined with HandShaker
            if (sender instanceof Player p) {
                Set<String> clientMods = plugin.getClientMods(p.getUniqueId());
                if (clientMods != null) {
                    List<String> suggestions = new ArrayList<>(clientMods);
                    suggestions.removeAll(plugin.getBlacklistConfig().getBlacklistedMods());
                    return suggestions;
                }
            }
        }
        return Collections.emptyList();
    }
}
