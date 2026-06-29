package com.aurasmp.ruin.command;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
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
        if (args.length == 0 || args[0].equalsIgnoreCase("level")) {
            // /ruin level add <n> [player] — op testing shortcut
            if (args.length >= 2 && args[1].equalsIgnoreCase("add")) {
                handleLevelAdd(sender, args);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players have a Ruin level.");
                return true;
            }
            PlayerData data = plugin.data().get(player.getUniqueId());
            player.sendMessage(Component.text("✦ Ruin — Level " + data.level()
                    + (data.isMaxLevel() ? " (MAX)" : "  " + data.xp() + "/" + plugin.progression().threshold(data.level())),
                    NamedTextColor.LIGHT_PURPLE));
            player.sendMessage(Component.text("Talents: ", NamedTextColor.GRAY)
                    .append(Component.text(data.cards().isEmpty() ? "none"
                            : String.join(", ", data.cards().stream().map(c -> c.displayName()).toList()), NamedTextColor.AQUA)));
            player.sendMessage(Component.text("Manifestations: ", NamedTextColor.GRAY)
                    .append(Component.text(data.abilities().isEmpty() ? "none"
                            : String.join(", ", data.abilities().stream().map(a -> a.displayName()).toList()), NamedTextColor.LIGHT_PURPLE)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "reset" -> handleReset(sender, args);
            case "xp" -> handleXp(sender, args);
            default -> sender.sendMessage(Component.text("Usage: /ruin <level|give|reset|xp>", NamedTextColor.RED));
        }
        return true;
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

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ruin.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ruin give <mirror|catalyst> [player]", NamedTextColor.RED));
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
        target.getInventory().addItem(item);
        sender.sendMessage(Component.text("Gave " + args[1] + " to " + target.getName() + ".", NamedTextColor.GREEN));
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
            for (String s : List.of("level", "give", "reset", "xp")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (String s : List.of("mirror", "catalyst")) {
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("level")) {
            if ("add".startsWith(args[1].toLowerCase())) out.add("add");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        }
        return out;
    }
}
