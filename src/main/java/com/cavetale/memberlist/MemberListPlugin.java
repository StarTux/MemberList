package com.cavetale.memberlist;

import com.cavetale.core.util.Json;
import com.winthier.connect.Connect;
import com.winthier.sql.SQLDatabase;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MemberListPlugin extends JavaPlugin {
    protected static MemberListPlugin instance;
    protected SQLDatabase database;
    @Getter protected String listName;

    @Override
    public void onEnable() {
        instance = this;
        loadConfiguration();
        database = new SQLDatabase(this);
        database.registerTables(List.of(SQLMember.class));
        if (!database.createAllTables()) {
            throw new IllegalStateException("Table creation failed!");
        }
        new MemberListCommand(this).enable();
    }

    protected void loadConfiguration() {
        reloadConfig();
        listName = getConfig().getString("list");
        if (listName == null || listName.isEmpty()) {
            listName = Connect.getInstance().getServerName();
        }
        getLogger().info("List name: " + listName);
    }

    public List<SQLMember> findMembers(String name) {
        return database.find(SQLMember.class).eq("list", name).findList();
    }

    protected void dump(String name) {
        YamlConfiguration cfg = new YamlConfiguration();
        Map<UUID, String> map = new HashMap<>();
        List<SQLMember> list = findMembers(name);
        for (SQLMember row : list) {
            cfg.set(row.getUuid().toString(), row.getName());
            map.put(row.getUuid(), row.getName());
            File file = new File(getDataFolder(), name + ".yml");
            try {
                cfg.save(file);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
            Json.save(new File(getDataFolder(), name + ".json"), map, true);
        }
    }
}
