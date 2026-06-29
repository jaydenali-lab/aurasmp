# Ruin

A Deepwoken-style kill-to-level progression plugin for **Paper 1.21.x** (built and tested
against the 1.21.x API; runs on 1.21.11). Kill mobs and players to earn XP, level up, and
draft **Talents** (passive perks) and **Manifestations** (active casts).

## How it works

- **Earn XP by killing.** Weak/passive mobs give little, hostile mobs give more, strong
  mobs and bosses give a lot, and **players give the most**.
- **Level up to draft.** Each level-up opens a draft menu with 3 random options — pick one.
  - Levels **1–6** (max level is **6**).
  - Most levels offer a **Talent** (passive).
  - **Every 3rd level (3 and 6)** offers a **Manifestation** (active cast) instead.
  - If you gain several levels at once, the drafts open **one after another**.
- **Cast Manifestations** with the **Ruin Catalyst** (given when you learn your first one):
  - Right-click = first manifestation
  - Shift + Right-click = second manifestation
- **Mirror Shard** resets your level back to 1 and clears all talents/manifestations so you
  can re-draft a new build. (A default recipe is registered — swap in your own; see below.)

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

## The 20 Talents (passive)

**Attribute talents (10)** — applied as persistent attribute modifiers:

| Talent | Effect |
|--------|--------|
| Vitality | +2 hearts of max health |
| Endurance | +3 hearts of max health |
| Bulwark | +3 armor |
| Tough Skin | +4 armor toughness |
| Swiftness | +15% movement speed |
| Brutality | +20% melee damage |
| Onslaught | +35% melee damage |
| Frenzy | +25% attack speed |
| Steadfast | +40% knockback resistance |
| Reach | +1 block of attack reach |

**Effect talents (10)** — resolved in the combat listeners:

| Talent | Effect |
|--------|--------|
| Lifesteal | Heal 10% of melee damage you deal |
| Leech | Heal 1 heart on every melee kill |
| Thorns | Reflect 30% of melee damage to attackers |
| Berserker | +30% damage while below 30% health |
| Executioner | +50% damage to targets below 20% health |
| Feather | Immune to fall damage |
| Adrenaline | Speed II + Regen I for 4s after a kill |
| Sharpshooter | +25% projectile damage |
| Juggernaut | Take 15% less damage from all sources |
| Scavenger | +50% Ruin XP from kills |

## The 10 Manifestations (active)

Cast via the Ruin Catalyst — each on its own cooldown:

| Manifestation | Effect | Cooldown |
|---------------|--------|----------|
| Blink | Teleport ~8 blocks where you look | 9s |
| Shockwave | Damage + knock back nearby enemies | 12s |
| Vanish | Invisibility + Speed II for 5s | 25s |
| Bloodthirst | Instantly heal 3 hearts + Regen II | 18s |
| Smite | Strike lightning at the enemy you face | 14s |
| Leap | Launch into the air, no fall damage | 8s |
| Frost Nova | Slow + damage all enemies near you | 15s |
| Wither Touch | Wither II to nearby enemies for 6s | 16s |
| Aura Burst | AoE blast around you (no block damage) | 20s |
| Magnetize | Pull nearby enemies toward you | 13s |

## HUD & custom icons

A live HUD sits **on top of the hotbar** (the action bar), refreshed twice a second:

```
[sigil] Lv4 320/900   [icon] Blink READY   [icon] Vanish ACTIVE 3s
```

For each learned Manifestation it shows a **custom icon**, the **name**, and the status:

- **READY** (green) — castable now
- **Ns** (red) — seconds left on cooldown
- **ACTIVE Ns** (gold) — its self-buff is currently running (Vanish, Leap, Bloodthirst)

The icons are custom glyphs from the **`ruin:icons`** resource-pack font
(`assets/ruin/font/icons.json` + `assets/ruin/textures/font/*.png`) — one per
manifestation, plus a Ruin sigil used in place of the old `✦` emoji. **They only
render with the resource pack applied;** without it you'll see blank boxes in the
HUD text (the rest still works).

## Commands

`/ruin` (alias for `/ruin level`)

| Command | Permission | Description |
|---------|-----------|-------------|
| `/ruin level` | `ruin.use` | Show your level, XP, talents and manifestations |
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
