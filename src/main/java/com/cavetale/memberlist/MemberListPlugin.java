package com.cavetale.memberlist;

import com.winthier.generic_events.GenericEvents;
import java.util.UUID;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
            for (int i = 1; i < args.length; i += 1) {
                UUID uuid = GenericEvents.cachedPlayerUuid(args[i]);
                if (uuid == null) {
                    sender.sendMessage(ChatColor.RED
                                       + "Unknown player: " + args[i]);
                    continue;
                }
                String name = GenericEvents.cachedPlayerName(uuid);
                memberList.people.put(uuid, name);
                sender.sendMessage("Added: " + name);
            }
            save();
            return true;
        }
        case "addall": {
            for (Player player : getServer().getOnlinePlayers()) {
                memberList.people.put(player.getUniqueId(), player.getName());
                sender.sendMessage("Added: " + player.getName());
            }
            save();
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
        default: return false;
        }
    }

    void save() {
        json.save("members.json", memberList, true);
    }

    void load() {
        memberList = json.load("members.json", MemberList.class, MemberList::new);
        if (memberList == null) memberList = new MemberList();
    }
}
