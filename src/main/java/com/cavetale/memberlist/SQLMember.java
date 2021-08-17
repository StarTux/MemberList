package com.cavetale.memberlist;

import com.winthier.playercache.PlayerCache;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import org.bukkit.entity.Player;

@Data
@Table(name = "members",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"list", "uuid"})})
public final class SQLMember {
    @Id
    private Integer id;
    @Column(nullable = false, length = 16)
    private String list; // serverName
    @Column(nullable = false)
    private UUID uuid;
    @Column(nullable = false, length = 16)
    private String name; // Player name
    @Column(nullable = false)
    private Date added;

    public SQLMember() { }

    public SQLMember(final String list, final PlayerCache player) {
        this.list = list;
        this.uuid = player.uuid;
        this.name = player.name;
        this.added = new Date();
    }

    public SQLMember(final String list, final Player player) {
        this.list = list;
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.added = new Date();
    }
}
