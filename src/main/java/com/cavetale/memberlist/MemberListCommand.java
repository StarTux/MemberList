package com.cavetale.memberlist;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MemberListCommand extends AbstractCommand<MemberListPlugin> {
    protected MemberListCommand(final MemberListPlugin plugin) {
        super(plugin, "memberlist");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("add").arguments("<player...>")
            .description("Add players")
            .senderCaller(this::add);
        rootNode.addChild("remove").arguments("<player...>")
            .description("Remove players")
            .senderCaller(this::remove);
        rootNode.addChild("list").arguments("[list]")
            .completers(CommandArgCompleter.supplyList(() -> Arrays.asList(plugin.listName)))
            .description("List members")
            .senderCaller(this::list);
        rootNode.addChild("clear").arguments("[list]")
            .completers(CommandArgCompleter.supplyList(() -> Arrays.asList(plugin.listName)))
            .description("Clear member list")
            .senderCaller(this::clear);
        rootNode.addChild("addall").denyTabCompletion()
            .description("Add everyone on this server")
            .senderCaller(this::addAll);
        rootNode.addChild("addnearby").arguments("<range>")
            .completers(CommandArgCompleter.integer(i -> i > 0))
            .description("Add nearby players")
            .playerCaller(this::addNearby);
        rootNode.addChild("check").arguments("<player>")
            .completers(CommandArgCompleter.NULL)
            .description("Check if player is on list")
            .senderCaller(this::check);
        rootNode.addChild("name").arguments("[list]")
            .completers(CommandArgCompleter.supplyList(() -> Arrays.asList(plugin.listName)))
            .description("Get or set list name")
            .senderCaller(this::name);
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload list name")
            .senderCaller(this::reload);
        rootNode.addChild("save").denyTabCompletion()
            .description("Save default config to disk")
            .senderCaller(this::save);
        rootNode.addChild("dump").arguments("[list]")
            .description("Dump member list to disk")
            .senderCaller(this::dump);
        rootNode.addChild("run").arguments("<list> once|each <command...>")
            .description("Run console command using members")
            .completers(CommandArgCompleter.supplyList(() -> Arrays.asList(plugin.listName)),
                        CommandArgCompleter.enumLowerList(RunType.class),
                        CommandArgCompleter.list("%player%", "%uuid%",
                                                 "%players%", "%uuids%",
                                                 "%oplayers%", "%ouuids%"),
                        CommandArgCompleter.REPEAT)
            .senderCaller(this::run);
    }

    private List<PlayerCache> argsToPlayerCache(CommandSender sender, String[] args) {
        List<PlayerCache> list = new ArrayList<>(args.length);
        for (String arg : args) {
            PlayerCache player = PlayerCache.forArg(arg);
            if (player == null) {
                sender.sendMessage(Component.text("Player not found: " + arg, NamedTextColor.RED));
            } else if (list.contains(player)) {
                sender.sendMessage(Component.text("Duplicate player: " + player.name, NamedTextColor.RED));
            } else {
                list.add(player);
            }
        }
        return list;
    }

    private List<SQLMember> argsToMemberList(CommandSender sender, String[] args) {
        return argsToPlayerCache(sender, args).stream()
            .map(p -> new SQLMember(plugin.listName, p))
            .collect(Collectors.toList());
    }

    protected boolean add(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        List<SQLMember> list = argsToMemberList(sender, args);
        final int count = plugin.database.insertIgnore(list);
        if (count == 0) {
            sender.sendMessage(Component.text("No players were added", NamedTextColor.RED));
        } else if (count == 1 && list.size() == 1) {
            sender.sendMessage(Component.text("Member added to " + plugin.listName + ": "
                                              + list.get(0).getName(), NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text(count + " players added to " + plugin.listName, NamedTextColor.YELLOW));
        }
        return true;
    }

    protected boolean remove(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        List<PlayerCache> list = argsToPlayerCache(sender, args);
        if (list.isEmpty()) {
            throw new CommandWarn(args.length == 1
                                  ? "Player not found: " + args[0]
                                  : "Players not found!");
        }
        Set<UUID> set = list.stream().map(PlayerCache::getUuid).collect(Collectors.toSet());
        final int count = plugin.database.find(SQLMember.class)
            .eq("list", plugin.listName)
            .in("uuid", set)
            .delete();
        if (count == 0) {
            sender.sendMessage(Component.text("Not on member list " + plugin.listName + "!", NamedTextColor.RED));
        } else if (count == 1 && list.size() == 1) {
            sender.sendMessage(Component.text("Member removed from " + plugin.listName + ": "
                                              + list.get(0).getName(), NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text(count + " players removed from " + plugin.listName, NamedTextColor.RED));
        }
        return true;
    }

    protected boolean list(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        String listName = args.length > 0 ? args[0] : plugin.listName;
        List<SQLMember> list = plugin.database.find(SQLMember.class).eq("list", listName).findList();
        if (list.isEmpty()) {
            throw new CommandWarn("Member list empty: " + listName);
        }
        sender.sendMessage(Component.text()
                           .content("List " + listName + " has " + list.size() + " members: ")
                           .color(NamedTextColor.YELLOW)
                           .append(Component.join(Component.space(),
                                                  list.stream()
                                                  .map(row -> Component.text()
                                                       .content(row.getName())
                                                       .color(NamedTextColor.WHITE)
                                                       .insertion(row.getName())
                                                       .hoverEvent(HoverEvent.showText(Component.text()
                                                                                       .content(row.getName())
                                                                                       .append(Component.newline())
                                                                                       .append(Component.text(row.getUuid().toString(),
                                                                                                              NamedTextColor.YELLOW))))
                                                       .build())
                                                  .collect(Collectors.toList()))));
        return true;
    }

    protected boolean clear(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        String listName = args.length > 0 ? args[0] : plugin.listName;
        final int count = plugin.database.find(SQLMember.class)
            .eq("list", listName)
            .delete();
        if (count == 0) {
            sender.sendMessage(Component.text("Member list empty: " + listName, NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text(count + " members cleared from " + listName, NamedTextColor.YELLOW));
        }
        return true;
    }

    protected boolean addAll(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        List<SQLMember> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            list.add(new SQLMember(plugin.listName, player));
        }
        final int count = plugin.database.insertIgnore(list);
        if (count == 0) {
            sender.sendMessage(Component.text("No players were added", NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text(count + " players added to " + plugin.listName, NamedTextColor.YELLOW));
        }
        return true;
    }

    protected boolean addNearby(Player player, String[] args) {
        if (args.length != 1) return false;
        long radius;
        try {
            radius = Long.parseLong(args[0]);
        } catch (NumberFormatException nfe) {
            throw new CommandWarn("Bad radius: " + args[0]);
        }
        Location playerLoc = player.getLocation();
        List<SQLMember> list = new ArrayList<>();
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.equals(player)) continue;
            Location nearbyLoc = nearby.getLocation();
            if (nearbyLoc.distance(playerLoc) > (double) radius) {
                continue;
            }
            list.add(new SQLMember(plugin.listName, nearby));
        }
        final int count = plugin.database.insertIgnore(list);
        if (count == 0) {
            player.sendMessage(Component.text("No players were added", NamedTextColor.RED));
        } else {
            player.sendMessage(Component.text(count + " players added to " + plugin.listName, NamedTextColor.YELLOW));
        }
        return true;
    }

    protected boolean check(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache player = PlayerCache.forArg(args[0]);
        if (player == null) {
            throw new CommandWarn("Player not found: " + args[0]);
        }
        final int count = plugin.database.find(SQLMember.class)
            .eq("list", plugin.listName)
            .eq("uuid", player.uuid)
            .findRowCount();
        if (count == 0) {
            sender.sendMessage(Component.text("Not on member list " + plugin.listName
                                              + ": " + player.name, NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text("Is on member list " + plugin.listName
                                              + ": " + player.name, NamedTextColor.GREEN));
        }
        return true;
    }

    protected boolean name(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 0) {
            sender.sendMessage(Component.text("List name: " + plugin.listName, NamedTextColor.YELLOW));
            return true;
        }
        plugin.listName = args[0];
        sender.sendMessage(Component.text("Using list " + plugin.listName, NamedTextColor.YELLOW));
        return true;
    }

    protected boolean reload(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.loadConfiguration();
        sender.sendMessage(Component.text("Config reloaded,"
                                          + " using list " + plugin.listName, NamedTextColor.YELLOW));
        return true;
    }

    protected boolean save(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.saveDefaultConfig();
        sender.sendMessage(Component.text("Config saved to disk", NamedTextColor.YELLOW));
        return true;
    }

    protected boolean dump(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        String listName = args.length > 0 ? args[0] : plugin.listName;
        plugin.dump(listName);
        sender.sendMessage(Component.text("Dumped list " + listName + " to "
                                          + listName + ".json and "
                                          + listName + ".yml", NamedTextColor.YELLOW));
        return true;
    }

    private enum RunType {
        EACH,
        ONCE;
    }

    protected boolean run(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Replacements:"
                                              + "\n%player% (each) player name"
                                              + "\n%players% all player names"
                                              + "\n%oplayers% (once) other player names"
                                              + "\n%uuid% (each) player uuid"
                                              + "\n%uuids% all player uuids"
                                              + "\n%ouuids% (once) other player uuids",
                                              NamedTextColor.YELLOW));
            return true;
        }
        if (args.length < 3) return false;
        String listName = args[0];
        RunType runType;
        try {
            runType = RunType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Unknown type: " + args[1]);
        }
        List<SQLMember> list = plugin.findMembers(listName);
        if (list.isEmpty()) {
            throw new CommandWarn("Member list empty: " + listName);
        }
        String cmd = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        cmd = cmd
            .replace("%players%", list.stream().map(SQLMember::getName).collect(Collectors.joining(" ")))
            .replace("%uuids%", list.stream().map(SQLMember::getUuid).map(UUID::toString).collect(Collectors.joining(" ")));
        if (runType == RunType.EACH) {
            for (SQLMember row : list) {
                String cmd2 = cmd
                    .replace("%player%", row.getName())
                    .replace("%uuid%", row.getUuid().toString())
                    .replace("%oplayers%", list.stream().filter(r -> r != row).map(SQLMember::getName).collect(Collectors.joining(" ")))
                    .replace("%ouuids%", list.stream().filter(r -> r != row).map(SQLMember::getUuid).map(UUID::toString).collect(Collectors.joining(" ")));
                plugin.getLogger().info("Running console command: " + cmd2);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd2);
            }
        } else if (runType == RunType.ONCE) {
            plugin.getLogger().info("Running console command: " + cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else {
            throw new IllegalStateException("runType=" + runType);
        }
        return true;
    }
}
