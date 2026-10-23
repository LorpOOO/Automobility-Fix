# Automobility Fix (NeoForge 1.21.1)

Author: **Lorp_OOO**

An unofficial addon for **[Automobility](https://modrinth.com/mod/automobility) 0.5.0.h** that fixes bugs in the mod and its conflicts with claim and team mods (Open Parties and Claims, FTB Teams, FTB Chunks).

This is an addon, not a fork: it contains no Automobility code and requires the original Automobility to be installed. All fixes are applied with mixins.

- Required on **both server and client**.
- Config: `config/automobility_fix-server.toml` (ownership settings are in the `ownership` section).
- Optional integrations: Open Parties and Claims, FTB Teams, FTB Chunks. Each one is enabled only when that mod is installed.

## What is fixed (and the actual cause)

| Bug | Cause in Automobility | Fix |
|---|---|---|
| Ghost hitboxes; vehicle can't be entered after rejoining or reloading a chunk | `HitboxEntity` was saved to the chunk without a link to its vehicle. The loaded copy did not tick in lazy chunks and stayed forever. Hitboxes only moved in their own tick | Hitboxes are no longer saved; old saved ones are removed on load; the vehicle positions its hitboxes every tick; a vehicle's hitboxes are removed together with it |
| Invisible obstacle left after trying to break a vehicle | An orphaned hitbox stayed collidable on the client | A hitbox without a vehicle has no collision and can't be targeted |
| AABB errors / invalid coordinates | NaN/infinite values in physics (normalizing a zero vector in the suspension, division by a weight of 0) | Speeds, position and suspension are sanitized |
| Vehicles flung into the air at chunk borders or in collisions | (1) `slopeStickingTimer` never decremented (`Math.max(0, t--)`), so the vehicle was pulled into the ground forever; (2) `(int)` instead of `floor` read the wrong block at negative coordinates; (3) vehicle-to-vehicle collisions accumulated impulse without limit | Timer fixed, `BlockPos.containing`, configurable limits for collision impulse and added velocity |
| Trunk item duplication / voiding | The trunk menu (`stillValid` always `true`) stayed open after the vehicle or attachment was destroyed, so items dropped and could still be taken from the menu | Menus are closed before items drop, and also when moving more than 8 blocks away or when the vehicle is destroyed |
| Auto Mechanic Table crash | NPE when hovering an empty recipe category (recipes removed or not synced by other mods); the output slot took part in "collect all" | An empty category is an empty list; output slot excluded from pick-all; shift-click also fills the 9th input slot |
| Dedicated server crash on startup: `Attempted to load class net/minecraft/client/player/LocalPlayer for invalid dist DEDICATED_SERVER` | Automobility's `EntityMixin` is registered as a common mixin but references the client-only `LocalPlayer` class | The mixin is cancelled on dedicated servers with [MixinSquared](https://github.com/Bawnorton/MixinSquared) (bundled in the jar); it only ever did something for the local player, so clients are unaffected |
| Autopilot render crash | `Math.clamp(anim, 0, values().length)` went out of array bounds | Upper bound is `length - 1` |
| Autopilot vehicles falling through or vanishing in unloaded chunks | Moving into an unloaded chunk means no collision, so the vehicle falls | The vehicle stops before an unloaded chunk (or, without a player driving, a non-ticking chunk) |
| OPAC: vehicle "duplicated" at claim borders | OPAC moves the vehicle back on the server, but the driver's client never learns about it | The server sends the driver a position correction and stops the vehicle |
| OPAC: driver ejected in a claim | The OPAC barrier throws the player out of the vehicle | If a passenger would be blocked by the barrier, the whole vehicle stops at the border instead |
| OPAC: checks were made against `automobility:hitbox` in the hitbox's chunk | The interaction target is the hitbox | Checks use the vehicle itself (its type and chunk); `automobility:automobile` is enough in OPAC configs |
| Vehicle and trunk theft | Vehicles had no owner | The owner is whoever assembled or placed the vehicle. The owner's whole team counts as owners too (FTB Teams or OPAC party; allies optional). Other players can't get in, open the trunk or dismantle the vehicle — anywhere, not only in claims |
| Owner can't use their vehicle in someone else's claim | OPAC / FTB Chunks don't know about the vehicle's owner | The owner and their team can use their vehicle in foreign OPAC and FTB Chunks claims too; other claim rules are unchanged |

Vehicles placed **before** installing the addon have no owner: anyone can use them, and normal claim rules apply.

## Config (`ownership` section)

| Option | Default | Meaning |
|---|---|---|
| `ownerProtection` | `true` | Only the owner and their team can use the vehicle |
| `trustPartyMembers` | `true` | Members of the owner's FTB Teams / OPAC team count as owners |
| `trustAllies` | `false` | Allies of the owner's team count as owners too |
| `ownerTeamIgnoresClaims` | `true` | The owner and their team may use the vehicle inside anyone's claims |
| `opsBypass` | `false` | Operators may use any vehicle (in LAN worlds with cheats, every player is an operator) |

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`.
