package com.aurasmp.ruin.command;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.item.RuinItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** {@code /ruin} command: status, admin give, reset and XP grants. */
public final class RuinCommand implements CommandExecutor, TabCompleter {

    private final RuinPlugin plugin;

    public RuinCommand(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0
                || args[0].equalsIgnoreCase("build")
                || args[0].equalsIgnoreCase("level")) {
            // Admin level subcommands still live under "level".
            if (args.length >= 2 && args[0].equalsIgnoreCase("level") && args[1].equalsIgnoreCase("add")) {
                handleLevelAdd(sender, args);
                return true;
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("level") && args[1].equalsIgnoreCase("give")) {
                handleLevelGive(sender, args);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players have a build.");
                return true;
            }
            // The admin can open another player's build to edit it.
            java.util.UUID target = player.getUniqueId();
            if (args.length >= 2 && args[0].equalsIgnoreCase("build")
                    && com.aurasmp.ruin.gui.BuildGui.isAdmin(player)) {
                Player other = Bukkit.getPlayerExact(args[1]);
                if (other != null) target = other.getUniqueId();
            }
            plugin.buildGui().open(player, target);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "reset" -> handleReset(sender, args);
            case "xp" -> handleXp(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(Component.text("Usage: /ruin <build|give|reset|xp|reload>", NamedTextColor.RED));
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("ruin.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        plugin.config().reload();
        sender.sendMessage(Component.text("Ruin config reloaded.", NamedTextColor.GREEN));
    }

    private void handleLevelAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ruin.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /ruin level add <amount> [player]", NamedTextColor.RED));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
            return;
        }
        Player target = resolveTarget(sender, args, 3);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        int gained = plugin.progression().awardLevels(target, plugin.data().get(target.getUniqueId()), amount);
        sender.sendMessage(Component.text("Added " + gained + " level(s) to " + target.getName()
                + " — drafts will open in sequence.", NamedTextColor.GREEN));
    }

    private void handleLevelGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ruin.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /ruin level give <player> <amount>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
            return;
        }
        int gained = plugin.progression().awardLevels(target, plugin.data().get(target.getUniqueId()), amount);
        sender.sendMessage(Component.text("Gave " + gained + " level(s) to " + target.getName()
                + " — drafts will open in sequence.", NamedTextColor.GREEN));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ruin.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ruin give <mirror|catalyst> [player] [amount]", NamedTextColor.RED));
            return;
        }
        Player target = resolveTarget(sender, args, 2);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        ItemStack item = switch (args[1].toLowerCase()) {
            case "mirror", "mirrorshard", "mirror_shard" -> plugin.items().create(RuinItems.MIRROR_SHARD);
            case "catalyst" -> plugin.items().catalyst(plugin.data().get(target.getUniqueId()).abilities());
            default -> null;
        };
        if (item == null) {
            sender.sendMessage(Component.text("Unknown item. Use: mirror | catalyst", NamedTextColor.RED));
            return;
        }
        // Optional amount: /ruin give mirror <player> <amount>
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
                return;
            }
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(Component.text("Gave " + amount + "x " + args[1] + " to " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleReset(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("ruin.admin")) {
                sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                return;
            }
            target = Bukkit.getPlayerExact(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("Usage: /ruin reset <player>", NamedTextColor.RED));
            return;
        }
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        plugin.resetPlayer(target);
        sender.sendMessage(Component.text("Reset Ruin progression for " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleXp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ruin.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ruin xp <amount> [player]", NamedTextColor.RED));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
            return;
        }
        Player target = resolveTarget(sender, args, 2);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        plugin.progression().award(target, plugin.data().get(target.getUniqueId()), amount);
        sender.sendMessage(Component.text("Granted " + amount + " XP to " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) return Bukkit.getPlayerExact(args[index]);
        return sender instanceof Player player ? player : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("build", "give", "reset", "xp", "reload")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (String s : List.of("mirror", "catalyst")) {
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("level")) {
            for (String s : List.of("add", "give")) {
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (args.length == 3
                && (args[0].equalsIgnoreCase("give")
                    || (args[0].equalsIgnoreCase("level") && args[1].equalsIgnoreCase("give")))) {
            // player slot for: /ruin give <type> <player> and /ruin level give <player>
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        }
        return out;
    }
}
