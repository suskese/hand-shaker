# HandShaker

**Fabric Mod & Paper Plugin for Minecraft 1.21+**  
Java 17+ compatible (built/tested on Java 21)

## What is HandShaker?

HandShaker is a cross-platform mod/plugin system for Minecraft servers and clients.  
It lets Paper servers see which Fabric mods a player is using, and automatically enforce a blacklist with customizable kick messages.

- **Fabric mod**: Sends your mod list to the server when you join.
- **Paper plugin**: Checks mod lists against a blacklist and kicks players using forbidden mods.

## Features

- **Fabric <-> Paper handshake**: Server sees all Fabric mods on join.
- **Blacklist enforcement**: Server kicks or warns players using blacklisted mods.
- **Configurable kick message**: Customize the message shown to kicked players.
- **Kick modes**:
  - `All`: Kick any client (vanilla or modded) with a blacklisted mod.
  - `Fabric`: Only kick Fabric clients with a blacklisted mod.
- **Admin commands** (`/handshaker`):
  - `/handshaker reload` — Reload blacklist and config.
  - `/handshaker add <mod>` — Add a mod to the blacklist (tab-completes from last client mod list).
  - `/handshaker remove <mod>` — Remove a mod from the blacklist (tab-completes from blacklist).
- **Permission-based**: Only server operators or those with `handshaker.admin` permission can use admin commands.

## Installation

### Server (Paper)

1. Build or download `hand-shaker-paper-<version>.jar`.
2. Place it in your server's `plugins/` folder.
3. Start the server once to generate the config.
4. Edit `plugins/HandShaker/config.yml` to set your blacklist and kick message.

### Client (Fabric)

1. Build or download `hand-shaker-<version>.jar`.
2. Place it in your Fabric client's `mods/` folder.
3. No config needed — the mod just sends your mod list to the server.

## Configuration

Example `config.yml`:
```yaml
Kick Mode: Fabric
# "Force" will kick all clients without Hand Shaker mod and with blacklisted mod, while "Fabric" will only kick Fabric clients without Hand Shaker mod or blacklisted mod.

Kick Message: "You are using a blacklisted mod: {mod}. Please remove it to join this server."

Blacklisted Mods:
- xraymod
- testmod
- forge
```
- `{mod}` in `Kick Message` will be replaced with the actual mod(s) found.

## Commands

- `/handshaker reload` — Reloads config and blacklist.
- `/handshaker add <mod>` — Adds a mod to the blacklist.
- `/handshaker remove <mod>` — Removes a mod from the blacklist.

## Permissions

- `handshaker.admin` — Required for all `/handshaker` commands (ops have this by default).

## Compatibility

- **Server**: Paper 1.21+ (should work on 1.20+ with minor tweaks)
- **Client**: Fabric Loader 0.17.3+, Minecraft 1.21+

## Building

```sh
# Build both mod and plugin
./gradlew build

# Build just the Paper plugin
./gradlew :paper:build
```

## License

MIT
