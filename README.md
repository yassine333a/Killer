# Killer

A Fabric mod for Minecraft 26.2 that adds a **Killer Hub**.

## Usage

1. Press **K** in game (you can rebind it in Controls → Killer) to open the hub.
2. Press **START** (or press **R** anywhere in game to start/stop quickly).
   and many mobs get hit at once. Press **STOP** to turn it off.

## Hub settings

| Setting          | What it does                                     | Range   |
|------------------|--------------------------------------------------|---------|
| Range            | How far away mobs get hit (blocks)               | 1 – 6   |
| Attacks per tick | Hits sent to each target every tick              | 1 – 10  |
| Max targets      | How many mobs get hit at the same time           | 1 – 50  |
| Smart timing     | Hit each mob once, then wait for its hurt cooldown (Hit delay) before hitting it again. Ignores Attacks per tick | on/off |
| Hit delay        | Ticks to wait between hits on the same mob (10 = vanilla hurt cooldown) | 0 – 20 |
| Full charge      | Only attack when the weapon is fully charged (max damage per hit) | on/off |
| Villagers        | Villagers and wandering traders                  | on/off  |
| Blazes           | Blazes                                           | on/off  |
| Hostile          | Every hostile mob (zombies, skeletons, ...)      | on/off  |
| Animals          | Passive mobs (cows, pigs, fish, ...)             | on/off  |
| All mobs         | Every living mob (golems included)               | on/off  |
| Players          | Other players                                    | on/off  |
| HUD              | Kill counter in the top-left corner              | on/off  |

## Mob Tracker

Press **Mob Tracker** in the hub to see total kills, kill speed (kills per
minute and per second), best and average speed, and kills per mob type.
**Reset** clears the stats. It also shows **In range now**: how many mobs the
current settings would hit right now (even while stopped). The **HUD** toggle shows a small live counter on
screen: mobs in range per type (yellow) and kills (white). It shows up
whenever the killer is running or there are mobs in range.

Settings are saved to `config/killer.json` when the hub is closed.

## Build

```
./gradlew build
```

The jar ends up in `build/libs/`. GitHub Actions also builds it on every push.
