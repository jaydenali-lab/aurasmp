# Ruin

A Deepwoken-style kill-to-level progression plugin for **Paper 1.21.x** (built and tested
against the 1.21.x API; runs on 1.21.11). Kill mobs and players to earn XP, level up, and
draft **Talents** (passive perks) and **Mantras** (active casts).

## How it works

- **Earn XP by killing.** Weak/passive mobs give little, hostile mobs give more, strong
  mobs and bosses give a lot, and **players give the most**.
- **Level up to draft.** Each level-up opens a draft menu with 3 random options — pick one.
  - Levels **1–6** (max level is **6**).
  - Most levels offer a **Talent** (passive).
  - **Every 3rd level (3 and 6)** offers a **Mantra** (active ability) instead.
  - If you gain several levels at once, the drafts open **one after another**.
- **Cast Mantras** with the **Ruin Catalyst** (given when you learn your first mantra):
  - Right-click = first mantra
  - Shift + Right-click = second mantra
- **Mirror Shard** resets your level back to 1 and clears all talents/mantras so you can
  re-draft a new build. (A default recipe is registered — swap in your own; see below.)

### XP values (base)

| Category | Examples | XP |
|----------|----------|----|
| Player | another player | 50 |
| Boss | Ender Dragon, Wither, Warden, Elder Guardian | 150 |
| Strong | Enderman, Ravager, Blaze, Iron Golem, Piglin Brute… | 20 |
| Common hostile | Zombie, Skeleton, Creeper, Spider… | 8 |
| Passive | Cow, Pig, Horse… | 2 |
| Weak | Chicken, Rabbit, Bat, Fish… | 1 |

Level thresholds: `100 → 250 → 500 → 900 → 1500`.

## The 20 Talents

**Attribute talents:** Vitality (+2 hearts), Endurance (+3 hearts), Bulwark (+3 armor),
Tough Skin (+4 toughness), Swiftness (+15% speed), Brutality (+20% melee), Onslaught
(+35% melee), Frenzy (+25% attack speed), Steadfast (+40% knockback resist), Reach (+1 reach).

**Effect talents:** Lifesteal (heal 10% of melee dealt), Leech (heal 1 heart per kill),
Thorns (reflect 30%), Berserker (+30% under 30% HP), Executioner (+50% vs <20% HP targets),
Feather (no fall damage), Adrenaline (Speed II + Regen after a kill), Sharpshooter (+25%
projectile damage), Juggernaut (−15% damage taken), Scavenger (+50% Ruin XP).

## The 10 Mantras

Blink, Shockwave, Vanish, Bloodthirst, Smite (lightning), Leap, Frost Nova, Wither Touch,
Aura Burst, Magnetize — each on its own cooldown.

## Commands

`/ruin` (alias for `/ruin level`)

| Command | Permission | Description |
|---------|-----------|-------------|
| `/ruin level` | `ruin.use` | Show your level, XP, talents and mantras |
| `/ruin level add <amount> [player]` | `ruin.admin` | Add levels (drafts open in sequence) |
| `/ruin xp <amount> [player]` | `ruin.admin` | Grant raw XP |
| `/ruin give <mirror\|catalyst> [player]` | `ruin.admin` | Give a custom item |
| `/ruin reset [player]` | self / `ruin.admin` | Reset progression |

## Custom Mirror Shard recipe

A default recipe is registered in `RuinItems#registerRecipes()`:

```
A E A      A = Amethyst Shard
E N E      E = Echo Shard
A E A      N = Nether Star
```

To use **your own** recipe, edit that method — the only requirement is that the crafted
result is `RuinItems#mirrorShard()` (it carries the identifying NBT tag).

## Building

```bash
gradle build
```

Output: `build/libs/Ruin-1.0.0.jar` → drop into your server's `plugins/` folder.
Requires Java 21.
