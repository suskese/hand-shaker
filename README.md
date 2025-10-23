[![](https://github.com/gabrielvicenteYT/modrinth-icons/raw/main/Branding/Badge/badge-dark.svg)](https://modrinth.com/plugin/hand-shaker)
>[!WARNING]
>### Current description represents latest (2.0.0) version of project. 
>### **This project requires to be set up on both sides, client and server side.**
# HandShaker

**Fabric Mod & Paper Plugin for Minecraft 1.21+**  
Java 17+ compatible (built/tested on Java 21)  
**If you want to request older version or found issue, please make a issue ticket on github**

## **Supports:**
### - **Fabric Client (2.0.0+ only)**
### - **Paper Servers**
### - **Fabric Servers**

# **What is HandShaker?**

HandShaker is a cross-platform mod/plugin system for Minecraft servers and clients.  
It lets Paper/Fabric servers see which Fabric mods players are using, and automatically enforce a blacklist with customizable kick messages.

- **Fabric mod**: Sends your mod list to the server when you join.
- **Fabric/Paper plugin**: Checks mod lists against a blacklist and kicks players using forbidden mods.

# Features

- **Fabric <-> Paper handshake**: Server sees all Fabric mods on join.
- **(New) Fabric Client <-> Fabric Server (2.0.0+ only)**
- **Blacklist enforcement**: Server kicks players using blacklisted mods.
- **Configurable kick & Missing mod messages**: Customize the message shown to kicked players.
- **Behavior**:
  - `Vanilla`: Allows ANY client, but would kick clients with blacklisted mods if client have mod.
  - `Strict`: Allows only fabric clients with mod and without blacklisted mods
- **Admin commands** (`/handshaker`):
  - `/handshaker reload` — Reload blacklist and config.
  - `/handshaker add <mod>` — Add a mod to the blacklist (tab-completes from last client mod list).
  - `/handshaker remove <mod>` — Remove a mod from the blacklist (tab-completes from blacklist).
  - **(NEW)**`/handshaker player <player> <mod>` - autocompletes other user mod from their modlist to add to blacklist.
- **Permission-based**: Only server operators or those with `handshaker.admin` permission can use admin commands.

## Installation

### Server (Paper)

1. Download `hand-shaker-paper-<version>.jar`.
2. Place it in your server's `plugins/` folder.
3. Start the server once to generate the config.
4. Edit `plugins/HandShaker/config.yml` as you need.

### Server (Fabric)

1. Download `hand-shaker-<version>.jar`.
2. Place it in your server's `mods/` folder.
3. Start the server once to generate the config.
4. Edit `config/hand-shaker.json` as you need.

### Client (Fabric)

1. Download `hand-shaker-<version>.jar`.
2. Place it in your Fabric client's `mods/` folder.
3. No config needed — the mod just sends your mod list to the server.

## Configuration

Example of default `config.yml` for Paper server:
```yaml
Behavior: Strict

#"Strict" - Kick clients with blacklisted mods + Kick non Hand-shaker mod clients
#"Vanilla" - Kick clients with blacklisted mods but ignoring non Hand-shaker mod clients (Could be dangeours if clients spoofs clients)

Kick Message: "You are using a blacklisted mod: {mod}. Please remove it to join this server."

Missing mod message: "To connect to this server please download 'Hand-shaker' mod."

Blacklisted Mods:
- xraymod
- testmod
- forge
```
Example of default `hand-shaker.json` for Fabric server:
```json
{
  "behavior": "VANILLA",
  "kick_message": "You are using a blacklisted mod: {mod}. Please remove it to join this server.",
  "missing_mod_message": "To connect to this server please download \u0027Hand-shaker\u0027 mod.",
  "blacklisted_mods": []
}
```

- `{mod}` in `Kick Message` will be replaced with the actual mod(s) found.

## Commands

- `/handshaker reload` — Reloads config and blacklist.
- `/handshaker add <mod to blacklist>` — Adds a mod to the blacklist.
- `/handshaker remove <mod>` — Removes a mod from the blacklist.
- `/handshaker player <player> <mod to blacklist>` — shows list of other user mods to blacklist mods easier.
## Permissions

- `handshaker.admin` — Required for all `/handshaker` commands (ops have this by default).

## Compatibility

- **Server**: Paper 1.21+
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
