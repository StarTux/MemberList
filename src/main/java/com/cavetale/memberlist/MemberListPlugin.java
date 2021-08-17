package com.cavetale.memberlist;

import com.winthier.connect.Connect;
import com.winthier.playercache.PlayerCache;
import com.winthier.sql.SQLDatabase;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MemberListPlugin extends JavaPlugin {
    private final Json json = new Json(this);
    private SQLDatabase database;
    private String listName;

    @Override
    public void onEnable() {
        loadConfiguration();
        database = new SQLDatabase(this);
        database.registerTables(SQLMember.class);
        if (!database.createAllTables()) {
            throw new IllegalStateException("Table creation failed!");
        }
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "add": {
            if (args.length < 2) return false;
            List<SQLMember> list = new ArrayList<>();
            for (int i = 1; i < args.length; i += 1) {
                PlayerCache player = PlayerCache.forName(args[i]);
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown player: " + args[i]);
                    continue;
                }
                list.add(new SQLMember(listName, player));
            }
            final int count = database.insertIgnore(list);
            if (count == 0) {
                sender.sendMessage(ChatColor.RED + "No players were added");
            } else if (count == 1 && list.size() == 1) {
                sender.sendMessage("" + ChatColor.YELLOW + "Member added: " + list.get(0).getName());
            } else {
                sender.sendMessage("" + ChatColor.YELLOW + count + " players added");
            }
            return true;
        }
        case "addall": {
            List<SQLMember> list = new ArrayList<>();
            for (Player player : getServer().getOnlinePlayers()) {
                list.add(new SQLMember(listName, player));
            }
            final int count = database.insertIgnore(list);
            if (count == 0) {
                sender.sendMessage(ChatColor.RED + "No players were added");
            } else {
                sender.sendMessage("" + ChatColor.YELLOW + count + " players added");
            }
            return true;
        }
        case "nearby": {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Player expected");
            }
            Player player = (Player) sender;
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: /ml nearby <radius>");
                return true;
            }
            double radius;
            try {
                radius = (double) Long.parseLong(args[1]);
            } catch (NumberFormatException nfe) {
                player.sendMessage("Bad radius: " + args[1]);
                return true;
            }
            List<String> names = new ArrayList<>();
            Location playerLoc = player.getLocation();
            for (Player nearby : player.getWorld().getPlayers()) {
                Location nearbyLoc = nearby.getLocation();
                if (nearby.equals(player)) continue;
                if (nearby.getGameMode() == GameMode.SPECTATOR) continue;
                if (nearbyLoc.distance(playerLoc) > radius) {
                    continue;
                }
                names.add(nearby.getName());
            }
            if (names.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Nobody found");
                return true;
            }
            ComponentBuilder cb = new ComponentBuilder();
            cb.append(names.size() + " people: ");
            String ls = names.stream().collect(Collectors.joining(" "));
            cb.append(ls);
            cb.insertion(ls);
            player.spigot().sendMessage(cb.create());
            return true;
        }
        case "addnearby": {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Player expected");
            }
            Player player = (Player) sender;
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: /ml nearby <radius>");
                return true;
            }
            double radius;
            try {
                radius = (double) Long.parseLong(args[1]);
            } catch (NumberFormatException nfe) {
                player.sendMessage("Bad radius: " + args[1]);
                return true;
            }
            Location playerLoc = player.getLocation();
            List<SQLMember> list = new ArrayList<>();
            for (Player nearby : player.getWorld().getPlayers()) {
                Location nearbyLoc = nearby.getLocation();
                if (nearby.equals(player)) continue;
                if (nearby.getGameMode() == GameMode.SPECTATOR) continue;
                if (nearbyLoc.distance(playerLoc) > radius) {
                    continue;
                }
                list.add(new SQLMember(listName, nearby));
            }
            final int count = database.insertIgnore(list);
            if (count == 0) {
                sender.sendMessage(ChatColor.RED + "No players were added");
            } else {
                sender.sendMessage("" + ChatColor.YELLOW + count + " players added");
            }
            return true;
        }
        case "remove": {
            if (args.length != 2) return false;
            PlayerCache player = PlayerCache.forName(args[1]);
            if (player == null) {
                sender.sendMessage("Unknown player: " + args[1]);
                return true;
            }
            final int count = database.find(SQLMember.class)
                .eq("list", listName)
                .eq("uuid", player.uuid)
                .delete();
            if (count == 0) {
                sender.sendMessage(ChatColor.RED + "Not on member list: " + player.name);
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Member removed: " + player.name);
            }
            return true;
        }
        case "check": {
            if (args.length != 2) return false;
            PlayerCache player = PlayerCache.forName(args[1]);
            if (player == null) {
                sender.sendMessage("Unknown player: " + args[1]);
                return true;
            }
            final int count = database.find(SQLMember.class)
                .eq("list", listName)
                .eq("uuid", player.uuid)
                .findRowCount();
            if (count == 0) {
                sender.sendMessage(ChatColor.RED + "Not on member list: " + player.name);
            } else {
                sender.sendMessage(ChatColor.GREEN + "Is a member: " + player.name);
            }
            return true;
        }
        case "list": {
            if (args.length != 1) return false;
            List<SQLMember> list = findMembers();
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ComponentBuilder cb = new ComponentBuilder();
                cb.append(list.size() + " people: ");
                cb.append(list.stream()
                          .map(SQLMember::getName)
                          .collect(Collectors.joining(", ")));
                cb.insertion(list.stream()
                             .map(SQLMember::getName)
                             .collect(Collectors.joining(" ")));
                player.spigot().sendMessage(cb.create());
                return true;
            }
            sender.sendMessage(list.size() + " people: "
                               + (list.stream()
                                  .map(SQLMember::getName)
                                  .collect(Collectors.joining(", "))));
            return true;
        }
        case "reload": {
            loadConfiguration();
            sender.sendMessage("Config reloaded!");
            return true;
        }
        case "save": {
            saveDefaultConfig();
            sender.sendMessage("Config saved to disk!");
            return true;
        }
        case "clear": {
            final int count = database.find(SQLMember.class)
                .eq("list", listName)
                .delete();
            if (count == 0) {
                sender.sendMessage(ChatColor.RED + "Member list empty!");
            } else {
                sender.sendMessage("" + ChatColor.YELLOW + count + " members cleared");
            }
            return true;
        }
        case "dump": {
            YamlConfiguration cfg = new YamlConfiguration();
            Map<UUID, String> map = new HashMap<>();
            List<SQLMember> list = findMembers();
            for (SQLMember row : list) {
                cfg.set(row.getUuid().toString(), row.getName());
                map.put(row.getUuid(), row.getName());
                File file = new File(getDataFolder(), "dump.yml");
                try {
                    cfg.save(file);
                } catch (IOException ioe) {
                    getLogger().log(Level.SEVERE, "Saving dump.yml", ioe);
                    sender.sendMessage("An error has occured. See console.");
                    return true;
                }
                json.save("dump.json", map, true);
            }
            sender.sendMessage("Dumped to dump.json and dump.yml");
            return true;
        }
        default: return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 0) return null;
        if (args.length == 1) {
            return Stream.of("add", "addall", "nearby", "addnearby", "remove", "check",
                             "list", "reload", "clear", "dump", "save")
                .filter(s -> s.startsWith(args[0]))
                .collect(Collectors.toList());
        }
        return null;
    }

    private void loadConfiguration() {
        reloadConfig();
        listName = getConfig().getString("list");
        if (listName == null || listName.isEmpty()) {
            listName = Connect.getInstance().getServerName();
        }
        getLogger().info("List name: " + listName);
    }

    public List<SQLMember> findMembers() {
        return database.find(SQLMember.class).eq("list", listName).findList();
    }
}
