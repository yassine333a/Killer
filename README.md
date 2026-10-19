# Killer

A Fabric mod for Minecraft 26.2 that adds a **Killer Hub**.

## Usage

1. Press **K** in game (you can rebind it in Controls → Killer) to open the hub.
2. Press **START**. Every matching mob in range gets hit many times per tick,
   and many mobs get hit at once. Press **STOP** to turn it off.

## Hub settings

| Setting          | What it does                                     | Range   |
|------------------|--------------------------------------------------|---------|
| Range            | How far away mobs get hit (blocks)               | 1 – 6   |
| Attacks per tick | Hits sent to each target every tick              | 1 – 10  |
| Max targets      | How many mobs get hit at the same time           | 1 – 50  |
| Villagers        | Villagers and wandering traders                  | on/off  |
| Blazes           | Blazes                                           | on/off  |
| Hostile          | Every hostile mob (zombies, skeletons, ...)      | on/off  |
| Animals          | Passive mobs (cows, pigs, fish, ...)             | on/off  |
| All mobs         | Every living mob (golems included)               | on/off  |
| Players          | Other players                                    | on/off  |

Settings are saved to `config/killer.json` when the hub is closed.

## Build

```
./gradlew build
```

The jar ends up in `build/libs/`. GitHub Actions also builds it on every push.
