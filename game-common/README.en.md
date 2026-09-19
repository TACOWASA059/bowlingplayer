# Bowling Player Game (Cops and Robbers) Add-on

[日本語](README.md) | [English](README.en.md)

Bowling Player Game is a game management add-on for Minecraft 1.20.1 on Fabric and Forge. It requires the base `bowlingplayer` mod. The red team plays as Pins and tries to escape; the blue team plays as Balls and tries to catch them. Staff members supervise as spectators.

- Add-on JAR: `bowlingplayergame-fabric-1.20.1-1.0.0.jar` or `bowlingplayergame-forge-1.20.1-1.0.0.jar`. The version changes with the build configuration.
- Command root: `/bpg`, available to operators with permission level 2.
- Install the base mod and add-on for the same loader.

## 1. How a round works

1. Set the red spawn, blue spawn, and at least one jail location with `/bpg config ...`. These settings are stored in `<world>/bowlingplayergame.json`.
2. Run `/bpg team auto` to assign unassigned players using the configured red:blue ratio (3:1 by default). Assign staff with `/bpg team set <player> staff`.
3. Run `/bpg start`. Red players deploy immediately and blue players wait as spectators. By default, blue deploys after 30 seconds. Everyone sees and hears the final 10-second countdown.
4. A captured red player respawns in jail. An uncaptured red teammate can rescue them. A dead blue player waits as a spectator and respawns at the blue spawn.
5. Blue wins when every red player is captured. Red wins when the time limit expires (30 minutes by default). The result and scores appear at the end, and everyone becomes a spectator.
6. Run `/bpg stop` to reset the round before starting another one.

## 2. Operator guide

Placeholders in `<angle brackets>` must be replaced. Parameters in `[square brackets]` are optional.

### First setup for each world

Spawn and jail commands save **the command sender's current position and dimension**. Move to each location before running the command.

1. At the red spawn, run `/bpg config reddeploy 5`. The number is the random spawn radius; omit it for a radius of zero.
2. At the blue spawn, run `/bpg config bluedeploy`.
3. At each jail location, run `/bpg config jail add`. Multiple locations are allowed.
4. Adjust settings if needed, for example `/bpg config timelimit 1200` for 20 minutes or `/bpg config ratio 3 1`.
5. Run `/bpg config show` and check that both spawns and the jail count are set.

Configuration survives server restarts. The initial setup need only be done once per world.

### Each round

1. Have all participants join the server.
2. Assign any organizers first: `/bpg team set <player> staff`.
3. Assign the remaining players: `/bpg team auto`.
4. Check counts with `/bpg status`.
5. Start with `/bpg start`. Red deploys immediately; blue deploys after `bluedelay` seconds (30 by default).
6. The winner is determined automatically. Run `/bpg score` to inspect every player's score.
7. Reset with `/bpg stop`.

The next `/bpg start` clears captures, rescues, and jail state. Check the previous scores before starting again if you need them.

### Play another round

- Keep the teams: `/bpg stop` → `/bpg start`.
- Swap red and blue: `/bpg stop` → `/bpg team swap` → `/bpg start`.
- Shuffle all non-staff players: `/bpg stop` → `/bpg team auto force` → `/bpg start`.

### Stop or change setup

`/bpg stop` can interrupt a round without declaring a winner. It removes the boss bar and placed cobwebs and returns participants to NORMAL mode. To change a jail, stop the game, run `/bpg config jail clear`, add each jail again, then check `/bpg config show` before starting.

### Troubleshooting

| Symptom | Action |
|---|---|
| `/bpg start` reports incomplete setup | Check red spawn, blue spawn, and jail count with `/bpg config show`. |
| A player has the wrong mode or cannot move | Run `/bpg stop`, correct their team, and start again. |
| A player joining mid-round has no team | New players are not automatically assigned. Stop and run `/bpg team auto`, or assign the player with `/bpg team set <player> red\|blue`. |
| Only one player needs a new team | Use `/bpg team set <player> red\|blue\|staff\|none`. |

Quick start:

```text
# Once per world: stand at each location
/bpg config reddeploy 5
/bpg config bluedeploy
/bpg config jail add

# Each round
/bpg team auto
/bpg start

# After the round
/bpg stop
```

## 3. Commands

All commands below begin with `/bpg`.

| Command | Purpose |
|---|---|
| `team auto` | Assign unassigned players to red and blue according to the team ratio. |
| `team auto force` | Reassign every non-staff player. |
| `team set <players> red\|blue\|staff\|none` | Assign or remove a player's team. |
| `team swap` | Swap online red and blue players. |
| `team clear` | Remove every team assignment. |
| `start` | Start a round if setup is complete. |
| `stop` | End the round and reset its state. |
| `status` | Show the phase and team counts. |
| `score` | Show blue captures and red rescues. |
| `config show` | Show the current configuration. |
| `config <key> <value>` | Set a numeric option from the table below. |
| `config ratio <red> <blue>` | Set the red:blue player ratio. |
| `config reddeploy [radius]` | Set the red spawn at your position, optionally with a scatter radius. |
| `config bluedeploy` | Set the blue spawn at your position. |
| `config jail add` | Add your position as a jail spawn. |
| `config jail clear` | Remove all jail positions. |
| `config items <red\|blue> steak <0-64>` | Set the team's starting steak count. |
| `config items <red\|blue> cobwebs <0-64>` | Set the starting and respawn cobweb count. |
| `config items <red\|blue> detector <true\|false>` | Enable or disable the team's detector. |
| `config items <red\|blue> speed <true\|false>` | Enable or disable the team's speed feather. |
| `config items <red\|blue> add <item> <count>` | Add a vanilla item to the starting kit, for example `minecraft:golden_apple 2`. |
| `config items <red\|blue> removeextra <index>` | Remove an extra item by its index from `config show`. |
| `config items <red\|blue> clearextra` | Remove all extra starting items. |

Numeric keys for `config <key> <value>` are `timelimit`, `bluedelay`, `itemcooldown`, `speedduration`, `speedlevel`, `detectrange`, `cobwebdespawn`, `bluespectator`, `ballsize`, `pinsize`, and `damage`.

## 4. Settings and defaults

| Setting | Key | Default |
|---|---|---|
| Round time limit | `timelimit` | 1,800 seconds (30 minutes) |
| Red:blue team ratio | `ratio` | 3:1 |
| Delay before blue deploys | `bluedelay` | 30 seconds |
| Item cooldown | `itemcooldown` | 120 seconds |
| Speed duration | `speedduration` | 15 seconds |
| Speed level | `speedlevel` | 2 (Speed II) |
| Detection range | `detectrange` | 60 blocks |
| Detection glow duration | `detectionGlowSeconds` in the JSON file | 10 seconds |
| Placed cobweb lifetime | `cobwebdespawn` | 10 seconds |
| Blue spectator time after death | `bluespectator` | 15 seconds |
| Blue ball size | `ballsize` | 0.95 |
| Red pin size | `pinsize` | 1.0 |
| Blue-to-red contact damage | `damage` | 1024 |
| Team kits | `items <red\|blue> ...` | Both: 64 steaks; red: 5 cobwebs; blue: detector and speed feather |
| Red spawn and scatter radius | `reddeploy` | Unset; radius 0 |
| Blue spawn | `bluedeploy` | Unset |
| Jail locations | `jail add` | Unset |

## 5. Items

Items are given when players deploy. `keepInventory` preserves them on death, and cobwebs are replenished on respawn. Red and blue kits can be configured separately.

| Item | Vanilla item | Default | Effect |
|---|---|---|---|
| Steak | Cooked beef | 64 for each team | Food. Set the count to zero to omit it. |
| Placeable cobweb | Cobweb | 5 for red | Right-click to place it in the empty block before the block you are looking at, up to five blocks away. It disappears after the configured lifetime. |
| Detector | Echo shard | Blue only | Highlights nearby red and blue players. Only the user sees the glow. It has a cooldown and is not consumed. |
| Speed feather | Feather | Blue only | Grants a timed speed effect. It has a cooldown and is not consumed. |

Special items use the `BpgItem` NBT tag and require no registered custom items or data pack. The distributed items occupy fixed hotbar slots: steak 0, cobweb 1, detector 2, and speed feather 3. Empty slots remain empty when items are disabled. Extra vanilla items can be added with `config items <red|blue> add <item> <count>` and are placed after the fixed slots.

## 6. Behavior details

1. Commands require operator permission level 2. Spawn and jail commands save the command sender's location and dimension.
2. Starting a round sets `keepInventory=true`, `fallDamage=false`, and `doImmediateRespawn=true`.
3. Participants cannot drop items during a round. Fabric cancels `Player#drop`; Forge cancels `ItemTossEvent` and returns the item to the inventory.
4. Distributed items return to their fixed hotbar slots each tick. Other inventory slots are free to use.
5. Cobweb placement uses a ray trace and requires an empty or non-colliding target block.
6. Everyone hears an anvil placement sound at the start and an anvil use sound at the end. Blue's final countdown uses experience orb sounds; deployment uses a level-up sound.
7. Captured red players remain marked as captured until rescued, even though they can move in Adventure mode. Multiple jail spawns are used in rotation.
8. An uncaptured red player rescues a captured red teammate by hitting them. The captive moves to the rescuer and the rescuer gains one rescue.
9. Blue players who die spectate for `bluespectator` seconds and return to the blue spawn in Adventure mode.
10. Vanilla scoreboard objectives `bpg_kills` and `bpg_rescues` record captures and rescues. Both are recreated at the start of each round. The sidebar is shown only after the round, alternating objectives every five seconds. The end summary shows each team's top five; `/bpg score` shows all players.
11. Detector glow packets are sent only to the detector user.
12. At `Game End`, everyone becomes a spectator and returns to NORMAL bowling mode. Spectating players use vanilla translucent rendering.
13. Vanilla scoreboard teams `bpg_red`, `bpg_blue`, and `bpg_staff` provide colors and disable friendly fire. Staff are spectators. Red uses PIN mode and blue uses BALL mode during play.
14. A single action bar combines the remaining time, blue deploy or respawn countdown, and short notifications. Survivor count is on the boss bar; start and end messages use titles.
15. Running game state is saved to `<world>/bowlingplayergame_state.json` so a round can resume after a server restart.

## 7. Recovery after a server crash

An interrupted round resumes automatically when the server starts again.

- Saved state includes the phase, remaining time, blue deployment, jailed players, capture and rescue scores, item cooldowns, blue respawn countdowns, and placed cobweb positions and lifetimes.
- State is saved at game start, blue deployment, death or rescue, every five seconds, and clean shutdown. Recovery may rewind by up to about five seconds.
- Elapsed and remaining ticks are rebased against the new server tick count on restart.
- The state file is written atomically. It is deleted when the round ends normally or `/bpg stop` is used.
- Rejoining participants receive their boss bar and PIN or BALL mode again. Team assignments, game modes, game rules, scoreboard scores, and placed cobweb blocks are also saved by Minecraft.

Run `/bpg stop` if you want to abandon an interrupted round completely.

## 8. Manual test checklist

Test in a single-player world or on a server, for both Fabric and Forge.

### Setup and teams

- [ ] Red spawn, blue spawn, and multiple jail positions save and appear in `/bpg config show` after restart.
- [ ] Numeric settings, team ratio, and each team's item settings take effect; disabled items leave their slots empty.
- [ ] `team auto` assigns only unassigned players; `team auto force` shuffles all non-staff players.
- [ ] Staff are spectators; `team swap` swaps red and blue; team colors and friendly fire rules work.

### Round and scoring

- [ ] Starting without all required positions fails. With valid setup, red spawns immediately and blue deploys after `bluedelay` seconds.
- [ ] Everyone receives the final ten-second countdown, titles, and sounds; the red and blue default kits contain the expected items.
- [ ] The boss bar tracks survivors; the action bar tracks time and turns red in the final minute.
- [ ] A captured red player immediately respawns in jail; jail positions rotate; the blue player's capture score rises.
- [ ] An uncaptured red player can rescue a jailed teammate, increasing the rescue score.
- [ ] Capturing every red player ends the game with a blue win; reaching the time limit ends it with a red win.
- [ ] End titles, scores, and spectator mode appear for everyone. `/bpg stop` clears the boss bar, cobwebs, sidebar, and bowling modes.

### Items and respawn

- [ ] Cobweb placement consumes one cobweb and the block disappears after `cobwebdespawn` seconds.
- [ ] Detector glow is visible only to its user; detector and speed cooldowns work.
- [ ] Dropping or moving a fixed-slot item fails; death does not drop items; cobwebs are replenished on respawn.
- [ ] A dead blue player respawns at the blue spawn after `bluespectator` seconds.

### Restart recovery

- [ ] Restart during a round and confirm that the round, timer, jail state, scores, cooldowns, and cobweb lifetimes continue.
- [ ] Rejoining participants regain their boss bar and mode. A round that ended or was stopped does not resume.
