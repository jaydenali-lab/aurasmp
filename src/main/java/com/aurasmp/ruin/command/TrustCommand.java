package com.aurasmp.ruin.command;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The ally system. {@code /trust <player>} marks someone as an ally:
 * your manifestations stop hitting them, and your support effects
 * (Rally, Medic, Pack Leader) start working on them. Trust is one-way —
 * it only changes what YOUR abilities do. {@code /untrust} reverses it,
 * {@code /trust list} shows who you trust.
 */
public final class TrustCommand implements TabExecutor {

    private final RuinPlugin plugin;
    private final boolean untrust; // the /untrust alias-command flips the behaviour

    public TrustCommand(RuinPlugin plugin, boolean untrust) {
        this.plugin = plugin;
        this.untrust = untrust;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        PlayerData data = plugin.data().get(player.getUniqueId());

        if (args.length == 0 || (!untrust && args[0].equalsIgnoreCase("list"))) {
            sendList(player, data);
            return true;
        }

        boolean removing = untrust
                || (args.length >= 2 && args[0].equalsIgnoreCase("remove"));
        String name = removing && !untrust && args.length >= 2 ? args[1] : args[0];

        OfflinePlayer target = resolve(name);
        if (target == null) {
            player.sendMessage(Component.text("Unknown player: " + name, NamedTextColor.RED));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You can always trust yourself.", NamedTextColor.GRAY));
            return true;
        }

        if (removing) {
            if (data.trusted().remove(target.getUniqueId())) {
                plugin.data().save(player.getUniqueId(), data);
                player.sendMessage(Component.text(target.getName() + " is no longer trusted.", NamedTextColor.YELLOW)
                        .append(Component.text(" Your manifestations will hit them again.", NamedTextColor.GRAY)));
            } else {
                player.sendMessage(Component.text(target.getName() + " wasn't trusted.", NamedTextColor.GRAY));
            }
        } else {
            if (data.trusted().add(target.getUniqueId())) {
                plugin.data().save(player.getUniqueId(), data);
                player.sendMessage(Component.text(target.getName() + " is now a trusted ally.", NamedTextColor.GREEN)
                        .append(Component.text(" Your manifestations spare them; Rally/Medic/Pack Leader include them.",
                                NamedTextColor.GRAY)));
                player.sendMessage(Component.text("(Trust is one-way — they must /trust you back for theirs.)",
                        NamedTextColor.DARK_GRAY));
            } else {
                player.sendMessage(Component.text(target.getName() + " is already trusted.", NamedTextColor.GRAY));
            }
        }
        return true;
    }

    private void sendList(Player player, PlayerData data) {
        if (data.trusted().isEmpty()) {
            player.sendMessage(Component.text("You trust no one. Use /trust <player> to mark an ally.",
                    NamedTextColor.GRAY));
            return;
        }
        List<String> names = new ArrayList<>();
        for (UUID id : data.trusted()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            names.add(op.getName() != null ? op.getName() : id.toString().substring(0, 8));
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        player.sendMessage(Component.text("Trusted allies (" + names.size() + "): ", NamedTextColor.GREEN)
                .append(Component.text(String.join(", ", names), NamedTextColor.WHITE)));
    }

    /** Online first, then known offline players by exact name. */
    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(name)) return op;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (!untrust) {
                out.add("list");
                out.add("remove");
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender)) out.add(p.getName());
            }
            out.removeIf(s -> !s.toLowerCase().startsWith(args[0].toLowerCase()));
        } else if (args.length == 2 && !untrust && args[0].equalsIgnoreCase("remove")
                && sender instanceof Player player) {
            for (UUID id : plugin.data().get(player.getUniqueId()).trusted()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                if (op.getName() != null && op.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    out.add(op.getName());
                }
            }
        }
        return out;
    }
}
