package com.cavetale.memberlist;

import com.winthier.generic_events.GenericEvents;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    MemberList memberList = new MemberList();
    Json json = new Json(this);

    @Override
    public void onEnable() {
        load();
    }

    @Override
    public boolean onCommand(final CommandSender sender,
                             final Command command,
                             final String alias,
                             final String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "add": {
            if (args.length < 2) return false;
            int count = 0;
            for (int i = 1; i < args.length; i += 1) {
                UUID uuid = GenericEvents.cachedPlayerUuid(args[i]);
                if (uuid == null) {
                    sender.sendMessage(ChatColor.RED
                                       + "Unknown player: " + args[i]);
                    continue;
                }
                if (!memberList.people.containsKey(uuid)) {
                    String name = GenericEvents.cachedPlayerName(uuid);
                    memberList.people.put(uuid, name);
                    sender.sendMessage("Added: " + name);
                    count += 1;
                }
            }
            if (count > 0) {
                save();
            }
            return true;
        }
        case "addall": {
            int count = 0;
            for (Player player : getServer().getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (!memberList.people.containsKey(uuid)) {
                    String name = player.getName();
                    memberList.people.put(uuid, name);
                    sender.sendMessage("Added: " + name);
                    count += 1;
                }
            }
            if (count > 0) {
                save();
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
            int count = 0;
            for (Player nearby : player.getWorld().getPlayers()) {
                Location nearbyLoc = nearby.getLocation();
                if (nearby.equals(player)) continue;
                if (nearby.getGameMode() == GameMode.SPECTATOR) continue;
                if (nearbyLoc.distance(playerLoc) > radius) {
                    continue;
                }
                UUID uuid = nearby.getUniqueId();
                if (!memberList.people.containsKey(uuid)) {
                    String name = nearby.getName();
                    memberList.people.put(uuid, name);
                    player.sendMessage("Added: " + name);
                    count += 1;
                }
            }
            if (count > 0) {
                save();
            }
            return true;
        }
        case "remove": {
            if (args.length != 2) return false;
            UUID uuid = GenericEvents.cachedPlayerUuid(args[1]);
            if (uuid == null) {
                sender.sendMessage("Unknown player: " + args[1]);
                return true;
            }
            String name = GenericEvents.cachedPlayerName(uuid);
            memberList.people.remove(uuid);
            sender.sendMessage("Removed: " + name);
            save();
            return true;
        }
        case "check": {
            if (args.length != 2) return false;
            UUID uuid = GenericEvents.cachedPlayerUuid(args[1]);
            if (uuid == null) {
                sender.sendMessage("Unknown player: " + args[1]);
                return true;
            }
            String name = GenericEvents.cachedPlayerName(uuid);
            if (memberList.people.containsKey(uuid)) {
                sender.sendMessage(ChatColor.GREEN
                                   + name + " is on the member list.");
            } else {
                sender.sendMessage(ChatColor.RED
                                   + name + " is NOT on the member list.");
            }
            return true;
        }
        case "list": {
            if (args.length != 1) return false;
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ComponentBuilder cb = new ComponentBuilder();
                cb.append(memberList.people.size() + " people: ");
                cb.append(memberList.people.values().stream()
                          .collect(Collectors.joining(", ")));
                cb.insertion(memberList.people.keySet().stream()
                             .map(GenericEvents::cachedPlayerName)
                             .collect(Collectors.joining(" ")));
                player.spigot().sendMessage(cb.create());
                return true;
            }
            sender.sendMessage(memberList.people.size() + " people: "
                               + memberList.people.values().stream()
                               .collect(Collectors.joining(", ")));
            return true;
        }
        case "reload": {
            load();
            sender.sendMessage("reloaded!");
            return true;
        }
        case "clear": {
            memberList.people.clear();
            save();
            sender.sendMessage("Member list cleared");
            return true;
        }
        case "dump": {
            YamlConfiguration cfg = new YamlConfiguration();
            for (Map.Entry<UUID, String> entry : memberList.people.entrySet()) {
                cfg.set(entry.getKey().toString(), entry.getValue());
                File file = new File(getDataFolder(), "dump.yml");
                try {
                    cfg.save(file);
                } catch (IOException ioe) {
                    getLogger().log(Level.SEVERE, "Saving dump.yml", ioe);
                    sender.sendMessage("An error has occured. See console.");
                    return true;
                }
            }
            sender.sendMessage("Dumped to dump.yml");
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
            return Stream.of("add", "addall", "nearby", "remove", "check",
                             "list", "reload", "clear", "dump")
                .filter(s -> s.startsWith(args[0]))
                .collect(Collectors.toList());
        }
        return null;
    }

    void save() {
        json.save("members.json", memberList, true);
    }

    void load() {
        memberList = json.load("members.json", MemberList.class, MemberList::new);
        if (memberList == null) memberList = new MemberList();
    }
}
