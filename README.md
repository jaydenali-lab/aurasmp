# Ruin

A Deepwoken-style kill-to-level progression plugin for **Paper 1.21.x** (built and tested
against the 1.21.x API; runs on 1.21.11). Kill mobs and players to earn XP, level up, and
draft **Talents** (passive perks) and **Manifestations** (active casts).

## How it works

- **Earn XP by killing.** Weak/passive mobs give little, hostile mobs give more, strong
  mobs and bosses give a lot, and **players give the most**.
- **Level up for Skill Points.** Every level-up grants **3 SP** (+3 bonus at max
  level 10 — **30 SP** for a full build). No more random drafts.
- **Spend them in the Skill Tree** — `/skilltree` (or `/st`, `/tree`):
  - Five branches: **Melee**, **Ranged**, **AoE**, **Support**, **Status**.
  - Every talent and manifestation is a node. **Better nodes cost more SP**
    (Common 1 · Rare 2 · Epic 3 · Legendary 5; manifestations 3–6 by tier).
  - **Deeper tiers unlock by investing in the branch**: Tier 2 needs 4 SP spent
    there, Tier 3 needs 9, Tier 4 needs 15 — specialise or spread wide.
  - You can carry at most **4 manifestations** at once.
- **Casting**: every manifestation you unlock becomes **its own item in your
  inventory** with its glyph icon — **right-click it to cast**. Cast items are
  soulbound: can't be dropped, stashed, crafted, or lost on death.
- **Weapon talents** (Melee branch) reward committing to one weapon: Fencer
  (swords), Spearhead + Skewer (the 1.21.11 **Spear**), and Concussive Blows —
  every 5th **axe** hit stuns like Zelkova for 1.5s.
- **Mirror Shard** = full respec: refunds every skill point so you can rebuild.
  (A default recipe is registered — swap in your own; see below.)

### XP values (base)

| Category | Examples | XP |
|----------|----------|----|
| Player | another player | 50 |
| Boss | Ender Dragon, Wither, Warden, Elder Guardian | 150 |
| Strong | Enderman, Ravager, Blaze, Iron Golem, Piglin Brute… | 20 |
| Common hostile | Zombie, Skeleton, Creeper, Spider… | 8 |
| Passive | Cow, Pig, Horse… | 2 |
| Weak | Chicken, Rabbit, Bat, Fish… | 1 |

Level thresholds: `100 → 250 → 500 → 900 → 1500 → 2300 → 3300 → 4500 → 6000`.

## The Skill Tree — 45 Talents · 35 Manifestations

Curated set: every node is mechanically unique. Organized by branch and tier below.

### Melee

| Tier | Node | Type | Effect |
|------|------|------|--------|
| 1 | Fencer | Talent | +7% damage while holding a sword. |
| 1 | First Strike | Talent | +6% melee damage to full-health targets. |
| 1 | Frenzy | Talent | +10% attack speed. |
| 1 | Spearhead | Talent | +10% damage while holding a spear. |
| 2 | Berserker | Talent | +7% damage while below 30% health. |
| 2 | Combo | Talent | Consecutive hits on the same target stack +2% damage (up to +6%). |
| 2 | Haymaker | Talent | Your melee hits knock enemies back much further. |
| 2 | Onslaught | Talent | +7% melee damage. |
| 2 | Skewer | Talent | Spear hits from 3.5+ blocks away deal +1.5 bonus damage. |
| 2 | Spine Cutter | Talent | Backstabs deal +1.5 damage. |
| 2 | Flame Grab | Manifestation (13s cd) | Lunge, seize a foe in flame and slam them. |
| 2 | Mook | Manifestation (12s cd) | Dash in and slash rapidly for 8 damage. |
| 2 | Phase Strike | Manifestation (12s cd) | Blink to the enemy you face and strike. |
| 3 | Executioner | Talent | +30% damage to targets below 20% health. |
| 3 | Glass Cannon | Talent | +15% melee damage, but take 20% more damage. |
| 3 | Rampage | Talent | Kills grant +2% damage for 20s, stacking 3 times. |
| 3 | Unyielding Inferno | Talent | +1.5 hearts to hits on burning foes. |
| 3 | Guillotine | Manifestation (15s cd) | Heavy blow: +20% of the target's missing health. |
| 3 | Riposte | Manifestation (16s cd) | 1.5s stance: parry the next hit and counter it. |
| 4 | Concussive Blows | Talent | Every 5th axe hit stuns the target for 1.5s. |
| 4 | Berserk | Manifestation (30s cd) | Strength + Speed + Resistance for 6s. |

### Ranged

| Tier | Node | Type | Effect |
|------|------|------|--------|
| 1 | Scavenger | Talent | +50% Ruin XP from kills. |
| 1 | Sharpshooter | Talent | +5% projectile damage. |
| 1 | Swiftness | Talent | +10% movement speed. |
| 1 | Blink | Manifestation (9s cd) | Teleport ~8 blocks where you look. |
| 1 | Fireball | Manifestation (11s cd) | Hurl a fireball that explodes on impact. |
| 1 | Volley | Manifestation (12s cd) | Loose a fan of five arrows. |
| 2 | Adrenaline | Talent | Speed II + Regen I for 4s after a kill. |
| 2 | Deflection | Talent | Take 20% less projectile damage. |
| 2 | Ember Mine | Manifestation (18s cd) | Bury a fire mine that erupts when stepped on. |
| 2 | Gale Step | Manifestation (8s cd) | Blink-dash on wind; no fall damage after. |
| 2 | Shadow Lance | Manifestation (14s cd) | Pierce all enemies in a line of shadow. |
| 2 | Smite | Manifestation (14s cd) | Strike lightning at the enemy you face. |
| 3 | Meteor | Manifestation (20s cd) | Call down a meteor where you look after 1s. |
| 3 | True Sight | Manifestation (20s cd) | Reveal every player within 20 blocks for 5s. |

### AoE

| Tier | Node | Type | Effect |
|------|------|------|--------|
| 1 | Ignite | Talent | Melee hits set the target on fire. |
| 1 | Shockwave | Manifestation (12s cd) | Damage + knock back nearby enemies. |
| 2 | Cleave | Talent | Melee hits splash 30% damage to nearby enemies. |
| 2 | Fissure | Manifestation (15s cd) | Crack the ground in a line that erupts under foes. |
| 2 | Wildfire | Manifestation (15s cd) | Erupt a cone of fire that ignites enemies. |
| 2 | Wind Slam | Manifestation (13s cd) | Leap up and slam down, blasting foes back. |
| 3 | Singularity | Manifestation (16s cd) | Collapse a point that drags enemies in for 3s. |
| 3 | Tundra | Manifestation (18s cd) | Freeze a wide zone, slowing all within. |
| 4 | Zelkova | Manifestation (20s cd) | Two ground slams (true dmg); the 2nd stuns and locks the camera. |

### Support

| Tier | Node | Type | Effect |
|------|------|------|--------|
| 1 | Bulwark | Talent | +3 armor. |
| 1 | Steadfast | Talent | +40% knockback resistance. |
| 1 | Vitality | Talent | +2 hearts of max health. |
| 1 | Bloodthirst | Manifestation (18s cd) | Instantly heal 3 hearts + Regen II. |
| 1 | Ice Barrier | Manifestation (15s cd) | Raise a wall of ice in front of you for 5s. |
| 2 | Clarity | Talent | Blindness, nausea and darkness never stick to you. |
| 2 | Feather | Talent | Immune to fall damage. |
| 2 | Lifesteal | Talent | Heal 10% of melee damage you deal. |
| 2 | Medic | Talent | Nearby hurt allies slowly regenerate. |
| 2 | Pack Leader | Talent | Resistance while an ally fights beside you. |
| 2 | Vanish | Manifestation (25s cd) | Invisibility + Speed II for 5s. |
| 3 | Overheal | Talent | Healing past full health becomes absorption (up to 2 hearts). |
| 3 | Regenerator | Talent | Constantly regenerate health. |
| 3 | Titan | Talent | +4 hearts of max health, but 20% slower. |
| 3 | Aegis | Manifestation (20s cd) | 4s ward that stops every projectile aimed at you. |
| 3 | Blood Pact | Manifestation (45s cd) | Sacrifice 2 hearts; reset your other cooldowns. |
| 3 | Cryostasis | Manifestation (25s cd) | Encase yourself in ice: 2.5s invulnerable but frozen. |
| 3 | Rewind | Manifestation (25s cd) | Mark yourself; 3s later snap back, restoring health. |
| 4 | Ghost | Talent | 15% chance when hit to fully vanish (armor too) + Speed II for 3s. |
| 4 | Juggernaut | Talent | Take 10% less damage from all sources. |
| 4 | Risky Moves | Talent | 15% chance to fully negate an incoming hit. |
| 4 | Second Wind | Talent | Below 20% HP: Regen II + Absorption (45s cd). |
| 4 | Undying | Talent | Once per 90s, a killing blow leaves you at 1 HP instead. |

### Status

| Tier | Node | Type | Effect |
|------|------|------|--------|
| 1 | Frostbite | Talent | Melee hits slow the target. |
| 1 | Hex | Manifestation (15s cd) | Curse the enemy you face with Weakness II. |
| 2 | Mangle | Talent | Enemies you hit heal 50% less for 5s. |
| 2 | Cocoon | Manifestation (16s cd) | Wrap the enemy you face in cobwebs. |
| 2 | Grasping Vines | Manifestation (14s cd) | Root the enemy you face in place for 2s. |
| 2 | Sunder | Manifestation (15s cd) | Crack their guard: target takes +15% damage for 5s. |
| 3 | Attunement | Talent | Your manifestation cooldowns are 15% shorter. |
| 3 | Battle Rush | Talent | Player kills refund half your manifestation cooldowns. |
| 3 | Escape Artist | Talent | Slowness never sticks to you. |
| 3 | Headhunter | Talent | Player kills grant 2 absorption hearts for 30s. |
| 3 | Retribution | Talent | Every 5 hits taken, your next melee hit deals +1 heart of true damage. |
| 3 | Lifedrain | Manifestation (18s cd) | Channel 3s: siphon health from the enemy you face. |
| 3 | Silence | Manifestation (10s cd) | Seal the enemy you face: no manifestations for 4s. |

## Icon credits

Talent and manifestation GUI icons are built from [game-icons.net](https://game-icons.net)
artwork (by Lorc, Delapouite, and contributors), used under
[CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) — recolored and framed for Ruin.

## Allies — /trust

By default every other player counts as an **enemy**: your damaging
manifestations hit them, and Sixth Sense / True Sight reveal them. Marking
someone as an ally changes that:

- `/trust <player>` — your manifestations **stop hitting them**, and your
  support effects (**Rally**, **Medic**, **Pack Leader**) start working on them.
- `/trust remove <player>` (or `/untrust <player>`) — back to enemy.
- `/trust list` — who you trust.

Trust is **one-way**: it only changes what *your* abilities do. For full
teamwork, both players trust each other. (Vanilla melee/arrows still hit
allies — trust governs Ruin abilities, not Minecraft itself.)

## Commands

`/ruin` (alias for `/ruin level`)

| Command | Permission | Description |
|---------|-----------|-------------|
| `/ruin build` | `ruin.use` | Open your build menu (level, talents, manifestations, rerolls) |
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

## Configuration

`config.yml` is generated on first run. Edit it and run **`/ruin reload`** (or
restart) to apply — no recompile needed:

```yaml
xp:                 # XP per kill by victim category
  player: 50
  boss: 150
  strong: 20
  common: 8
  passive: 2
  weak: 1
thresholds: [100, 250, 500, 900, 1500, 2300, 3300, 4500, 6000]   # 1->2 … 9->10
ghost:
  chance: 0.15      # Ghost talent vanish chance (0.0–1.0)
scavenger:
  multiplier: 1.5   # Scavenger talent XP multiplier
boss-bar:
  hide-seconds: 5   # how long the XP boss bar stays after a gain
```

## Building

```bash
gradle build
```

Output: `build/libs/Ruin-2.2.0.jar` → drop into your server's `plugins/` folder.
Requires Java 21.
