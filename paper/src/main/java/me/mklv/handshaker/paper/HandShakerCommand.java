package me.mklv.handshaker.paper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class HandShakerCommand implements TabExecutor {
    private final HandShakerPlugin plugin;

    public HandShakerCommand(HandShakerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.getBlacklistConfig().load();
                sender.sendMessage("HandShaker config reloaded. Re-checking all online players.");
                plugin.checkAllPlayers();
            }
            case "mode" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /handshaker mode <blacklist|whitelist>");
                    return true;
                }
                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "blacklist" -> {
                        plugin.getBlacklistConfig().setMode(BlacklistConfig.Mode.BLACKLIST);
                        sender.sendMessage("HandShaker mode set to blacklist.");
                        plugin.checkAllPlayers();
                    }
                    case "whitelist" -> {
                        plugin.getBlacklistConfig().setMode(BlacklistConfig.Mode.WHITELIST);
                        sender.sendMessage("HandShaker mode set to whitelist.");
                        plugin.checkAllPlayers();
                    }
                    default -> sender.sendMessage("Unknown mode. Use blacklist or whitelist.");
                }
            }
            case "whitelist_update" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /handshaker whitelist_update <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                Set<String> mods = plugin.getClientMods(target.getUniqueId());
                if (mods == null) {
                    sender.sendMessage("Mod list for " + target.getName() + " not found. Make sure they are online and using Fabric.");
                    return true;
                }
                plugin.getBlacklistConfig().setWhitelistedMods(mods);
                sender.sendMessage("Whitelist updated with " + target.getName() + "'s mods. " + mods.size() + " mods added.");
                plugin.checkAllPlayers();
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /handshaker remove <mod>");
                    return true;
                }
                boolean removed = plugin.getBlacklistConfig().removeMod(args[1]);
                sender.sendMessage(removed ? "Removed " + args[1] : args[1] + " not found in blacklist.");
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
                }
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("/handshaker reload | add <mod> | remove <mod> | player <player> [mod] | mode <b|w> | whitelist_update <player>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0],
                    Arrays.asList("reload", "add", "remove", "player", "mode", "whitelist_update"),
                    new ArrayList<>());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "remove" -> {
                    return StringUtil.copyPartialMatches(args[1], plugin.getBlacklistConfig().getBlacklistedMods(), new ArrayList<>());
                }
                case "add" -> {
                    if (sender instanceof Player p) {
                        Set<String> clientMods = plugin.getClientMods(p.getUniqueId());
                        if (clientMods != null) {
                            List<String> suggestions = new ArrayList<>(clientMods);
                            suggestions.removeAll(plugin.getBlacklistConfig().getBlacklistedMods());
                            return StringUtil.copyPartialMatches(args[1], suggestions, new ArrayList<>());
                        }
                    }
                }
                case "player", "whitelist_update" -> {
                    List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
                }
                case "mode" -> {
                    return StringUtil.copyPartialMatches(args[1], Arrays.asList("blacklist", "whitelist"), new ArrayList<>());
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("player")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                Set<String> clientMods = plugin.getClientMods(target.getUniqueId());
                if (clientMods != null) {
                    List<String> suggestions = new ArrayList<>(clientMods);
                    suggestions.removeAll(plugin.getBlacklistConfig().getBlacklistedMods());
                    return StringUtil.copyPartialMatches(args[2], suggestions, new ArrayList<>());
                }
            }
        }
        return Collections.emptyList();
    }
}