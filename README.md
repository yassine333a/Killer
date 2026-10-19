# Killer

A Fabric mod for Minecraft 26.2 that adds a **Killer Hub**.

## Usage

1. Press **K** in game (you can rebind it in Controls → Killer) to open the hub.
2. Press **START** (or press **R** anywhere in game). Every matching mob in
   range gets hit, many at once. Press **STOP** (or **R** again) to turn it off.

## Hub settings

| Setting          | What it does                                     | Range   |
|------------------|--------------------------------------------------|---------|
| Range            | How far away mobs get hit (blocks)               | 1 – 6   |
| Max targets      | How many mobs get hit at the same time           | 1 – 50  |
| Smart timing     | Fastest timing that still deals damage: hits each mob the moment its hurt cooldown ends. Locks Attacks/tick and Hit delay (shown as Auto) | on/off |
| Attacks/tick     | Manual timing (smart timing off): hits per mob each time | 1 – 10 |
| Hit delay        | Manual timing (smart timing off): ticks to wait before hitting the same mob again | 0 – 20 |
| Full charge      | Only attack when the weapon is fully charged (max damage per hit) | on/off |
| Villagers        | Villagers and wandering traders                  | on/off  |
| Blazes           | Blazes                                           | on/off  |
| Hostile          | Every hostile mob (zombies, skeletons, ...)      | on/off  |
| Animals          | Passive mobs (cows, pigs, fish, ...)             | on/off  |
| All mobs         | Every living mob (golems included)               | on/off  |
| Players          | Other players                                    | on/off  |
| HUD              | Kill counter in the top-left corner              | on/off  |

## Auto repair

While the killer runs, the held sword/axe is watched:

- Below **15%** durability it sends `/repairer`, clicks the **golden sword** in
  the menu that opens, then closes the menu. Attacks pause while it does this.
  If the weapon is still low it clicks again (up to 3 times), then closes the
  menu and runs the command again, up to **5 tries**. Pressing START again
  gives it 5 new tries.
- Below **5%** durability the killer **stops right away**. Auto repair keeps
  trying, and once the weapon is repaired the killer starts again by itself.

Turn it on/off with **Auto repair** in the hub. The command and both
percentages can be changed in `config/killer.json` (`repairCommand`,
`repairAt`, `stopAt`, `repairTries`). The tracker panel shows the weapon's durability.

## Mob Tracker

The right side of the hub shows live stats: mobs in range right now, how many
are being hit right now (hit in the last second), total kills, kill speed (per minute and per second),
best and average speed, and kills per mob type. **Reset stats** clears them.
The **HUD** toggle shows a small live counter on screen: mobs in range per
type (yellow), hitting now (red) and kills (white).

Settings are saved to `config/killer.json` when the hub is closed.

## Build

```
./gradlew build
```

The jar ends up in `build/libs/`. GitHub Actions also builds it on every push.
