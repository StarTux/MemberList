# MemberList

Member List plugin. Keep track of a simple player list so they can be
copied and pasted into other commands later on.

## Commands

The `/memberlist` (`/ml`) commmand is intended for console or admins,
never for regular players.

- `/ml add <player>` Add player
- `/ml remove <player>` Remove player
- `/ml check <player>` Check membership
- `/ml list` List members (players may shift click this to copy)
- `/ml reload` Reload save file

## Permissions

There is only one permission:

- `memberlist.memberlist` to run the command.

## Storage

The member list is stored in a text file in JSON format:
`members.json`.  It contains a mapping from UUIDs to usernames.  At
runtime however, PlayerCache is used to find cached player UUIDs and
names.  It does not work without.

## Dependencies

- **PlayerCache** (<https://github.com/StarTux/PlayerCache>) to
    look up cached player UUIDs and names

## Website

- Minecraft: <https://cavetale.com>
- Github: <https://github.com/StarTux/MemberList>
