package me.mklv.handshaker.paper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil; // Spigot's utility for tab completion

import java.util.*;
import java.util.stream.Collectors;

public class HandShakerCommand implements TabExecutor {
    private final HandShakerPlugin plugin;
    public HandShakerCommand(HandShakerPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/handshaker reload | add <mod> | remove <mod> | player <player> [mod]");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.getBlacklistConfig().load();
                sender.sendMessage("HandShaker blacklist reloaded. Re-checking all online players.");
                plugin.checkAllPlayers();
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
                if (added) {
                    plugin.checkAllPlayers();
                }
                return true;
            }
            case "player" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /handshaker player <player> [mod]");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player '" + args[1] + "' not found.");
                    return true;
                }
                Set<String> mods = plugin.getClientMods(target.getUniqueId());
                if (mods == null || mods.isEmpty()) {
                    sender.sendMessage("No mod list found for " + target.getName() + ".");
                    return true;
                }

                if (args.length == 2) {
                    sender.sendMessage(target.getName() + "'s mods: " + String.join(", ", mods));
                    return true;
                }

                if (args.length == 3) {
                    String modToBlock = args[2];
                    if (!mods.contains(modToBlock)) {
                        sender.sendMessage(target.getName() + " does not have the mod '" + modToBlock + "'.");
                        return true;
                    }
                    boolean added = plugin.getBlacklistConfig().addMod(modToBlock);
                    sender.sendMessage(added ? "Added " + modToBlock + " to the blacklist." : modToBlock + " is already in the blacklist.");
                    if (added) {
                        plugin.checkAllPlayers();
                    }
                    return true;
                }

                sender.sendMessage("Usage: /handshaker player <player> [mod]");
                return true;
            }
            default -> sender.sendMessage("Unknown subcommand.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Filter primary subcommands based on user input
            return StringUtil.copyPartialMatches(args[0],
                    Arrays.asList("reload", "add", "remove", "player"),
                    new ArrayList<>());
        }

        if (args.length == 2) {
            String currentInput = args[1];

            // Filter blacklisted mods for the "remove" subcommand
            if (args[0].equalsIgnoreCase("remove")) {
                return StringUtil.copyPartialMatches(currentInput,
                        plugin.getBlacklistConfig().getBlacklistedMods(),
                        new ArrayList<>());
            }

            // Filter sender's own mods for the "add" subcommand
            if (args[0].equalsIgnoreCase("add") && sender instanceof Player p) {
                Set<String> clientMods = plugin.getClientMods(p.getUniqueId());
                if (clientMods != null) {
                    List<String> suggestions = new ArrayList<>(clientMods);
                    suggestions.removeAll(plugin.getBlacklistConfig().getBlacklistedMods());
                    return StringUtil.copyPartialMatches(currentInput, suggestions, new ArrayList<>());
                }
            }

            // Filter online player names for the "player" subcommand
            if (args[0].equalsIgnoreCase("player")) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(currentInput, playerNames, new ArrayList<>());
            }
        }

        if (args.length == 3) {
            // Filter a specific player's mods for the "player <playername>" subcommand
            if (args[0].equalsIgnoreCase("player")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    Set<String> clientMods = plugin.getClientMods(target.getUniqueId());
                    if (clientMods != null) {
                        List<String> suggestions = new ArrayList<>(clientMods);
                        suggestions.removeAll(plugin.getBlacklistConfig().getBlacklistedMods());
                        // Filter suggestions based on the 3rd argument (the mod name)
                        return StringUtil.copyPartialMatches(args[2], suggestions, new ArrayList<>());
                    }
                }
            }
        }
        return Collections.emptyList();
    }
}