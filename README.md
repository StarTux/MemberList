# MemberList

Member List plugin. Keep track of a simple player list so they can be
copied and pasted into other commands later on.

## Commands

The `/memberlist` (`/ml`) commmand is intended for console or admins,
never for regular players.

- `/ml add <player...>` Add player(s)
- `/ml remove <player...>` Remove player(s)
- `/ml list [list]` List members
- `/ml clear [list]` Clear members
- `/ml addall` Add all online players
- `/ml addnearby <range>` Add nearby players
- `/ml check <player>` Check membership
- `/ml name <list>` Use list name
- `/ml reload` Reload save file
- `/ml save` Save default config to disk
- `/ml dump` Dump to disk

Commands don't accept a list name argument, unless it is the only
argument! In order to access other lists with regular commands, use
the `name` command. You may follow it up with `reload` to restore the
default list name.

## Configuration

The configuration may be edited to override the default list name
which is usually retrived from Connect.

```yml
name: ''
```

## Permissions

There is only one permission:

- `memberlist.memberlist` to run the command.

## Storage

The member list is stored in a database. Each row contains:

- List name (server name)
- Player UUID
- Player name

## Dependencies

- **Core** (<https://github.com/StarTux/Core>) Command framework
- **Connect** (<https://github.com/StarTux/Connect>) Get the server
    name
- **SQL** (<https://github.com/StarTux/SQL>) Access the database
- **PlayerCache** (<https://github.com/StarTux/PlayerCache>) look up
    cached player UUIDs and names

## Website

- Minecraft: <https://cavetale.com>
- Github: <https://github.com/StarTux/MemberList>
