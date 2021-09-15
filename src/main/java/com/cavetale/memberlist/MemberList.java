package com.cavetale.memberlist;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Public API.
 */
public final class MemberList {
    private MemberList() { }

    public static String name() {
        return MemberListPlugin.instance.listName;
    }

    public static Map<UUID, String> get(String listName) {
        Map<UUID, String> result = new HashMap<>();
        for (SQLMember row : MemberListPlugin.instance.findMembers(listName)) {
            result.put(row.getUuid(), row.getName());
        }
        return result;
    }
}
