# Ruin

A Deepwoken-style kill-to-level progression plugin for **Paper 1.21.x** (built and tested
against the 1.21.x API; runs on 1.21.11). Kill mobs and players to earn XP, level up, and
draft **Talents** (passive perks) and **Manifestations** (active casts).

## How it works

- **Earn XP by killing.** Weak/passive mobs give little, hostile mobs give more, strong
  mobs and bosses give a lot, and **players give the most**.
- **Level up to draft.** Each level-up opens a draft menu — **5 talents** or **4
  manifestations** to choose from. You're **invincible while it's open**, and you
  get **5 rerolls per build** (resets on a Mirror Shard wipe).
  - Levels **1–10** (max level is **10**).
  - Most levels offer a **Talent** (passive).
  - **Every 5th level (5 and 10)** offers a **Manifestation** (active cast) instead.
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

Level thresholds: `100 → 250 → 500 → 900 → 1500 → 2300 → 3300 → 4500 → 6000`.

## The 47 Talents (passive)

**Attribute talents (10)** — applied as persistent attribute modifiers:

| Talent | Effect |
|--------|--------|
| Vitality | +2 hearts of max health |
| Endurance | +3 hearts of max health |
| Bulwark | +3 armor |
| Tough Skin | +4 armor toughness |
| Swiftness | +15% movement speed |
| Brutality | +10% melee damage |
| Onslaught | +20% melee damage |
| Frenzy | +10% attack speed |
| Steadfast | +40% knockback resistance |
| Reach | +1 block of attack reach |

**Effect talents (10)** — resolved in the combat listeners:

| Talent | Effect |
|--------|--------|
| Lifesteal | Heal 10% of melee damage you deal |
| Leech | Heal 1 heart on every melee kill |
| Ghost | 15% chance when hit to fully vanish (armor too) + Speed II for 3s |
| Berserker | +30% damage while below 30% health |
| Executioner | +50% damage to targets below 20% health |
| Feather | Immune to fall damage |
| Adrenaline | Speed II + Regen I for 4s after a kill |
| Sharpshooter | +25% projectile damage |
| Juggernaut | Take 15% less damage from all sources |
| Scavenger | +50% Ruin XP from kills |
| Ghost | 15% chance when hit to fully vanish (armor too) + Speed II for 3s |

**Expansion — attribute (3):**

| Talent | Effect |
|--------|--------|
| Sure-Footed | Step up full blocks without jumping |
| Deep Lungs | Hold your breath far longer underwater |
| Lucky | +3 Luck (better loot rolls) |

**Expansion — aura (7):** kept refreshed automatically.

| Talent | Effect |
|--------|--------|
| Night Owl | Permanent Night Vision |
| Leaper | Permanent Jump Boost |
| Haste | Permanent Haste |
| Aquatic | Breathe underwater freely |
| Fire Walker | Permanent Fire Resistance |
| Regenerator | Constantly regenerate health |
| Barrier | A constant 2-heart absorption shield |

**Expansion — combat triggers (7):**

| Talent | Effect |
|--------|--------|
| Ignite | Melee hits set the target on fire |
| Venom | Melee hits apply Poison |
| Frostbite | Melee hits slow the target |
| Cleave | Melee hits splash 30% damage to nearby enemies |
| Crit | 25% chance for melee hits to deal +50% |
| Bloodlust | Strength I for 5s after a kill |
| Retribution | Every 5 hits taken, your next melee hit deals +1.5 hearts of true damage |

**Deepwoken talents (10):**

| Talent | Effect |
|--------|--------|
| Steady Feet | +50% knockback resistance |
| Thresher's Reach | +1.5 blocks of attack reach |
| Quickdraw | +20% attack speed |
| Endurance Runner | +8% movement speed |
| Kick Off | +6% speed and no short-fall damage |
| Conditioned Runner | Regenerate while sprinting hurt |
| Pack Leader | Resistance while an ally is near |
| Unyielding Inferno | +2 hearts to hits on burning foes |
| Spine Cutter | Backstabs deal +3 damage |
| Risky Moves | 20% chance to fully negate an incoming hit |

(Note: the original **Thorns** was replaced by **Ghost**.)

## The 28 Manifestations (active)

Cast via the Ruin Catalyst — each on its own cooldown. **Damaging manifestations
deal normal damage** (reduced by armour), scaled to cooldown (≈ cooldown ÷ 5
hearts); ones that also apply a debuff deal 1/5 of that. The **Catalyst can't be
dropped** and is **restored when you respawn**.

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
| Fireball | Hurl a fireball that ignites foes on impact | 11s |
| Updraft | Fling nearby enemies into the air | 13s |
| Launch | Fire an arrow and ride it through the air | 8s |
| Berserk | Strength II + Speed + Resistance for 6s | 30s |
| Smoke Bomb | Blind nearby foes; turn invisible for 3s | 22s |
| Lightning Storm | Strike up to 3 nearby enemies with lightning | 18s |
| Sanctuary | Regen + Resistance + Absorption for 5s | 28s |
| Plague | Poison II + Nausea to nearby enemies for 6s | 16s |
| Meteor | Call down a meteor where you look after 1s | 20s |
| Dash | Dash forward with a burst of speed | 7s |
| Flame Grab | Lunge, seize a foe in flame and slam them | 13s |
| Wildfire | Erupt a cone of fire that ignites enemies | 15s |
| Frostdraw Spikes | Ice spikes impale and chill foes ahead | 12s |
| Tundra | Freeze a wide zone, slowing all within | 18s |
| Shock Sword | A lightning slash that stuns | 11s |
| Galvanize | Haste + Speed + Strength for 6s | 24s |
| Wind Slam | Blast wind, launching nearby foes back | 13s |
| Gale Step | Blink-dash on a burst of wind | 8s |

*The last eight are real Deepwoken mantras.*

## HUD & custom icons

A live HUD sits **on top of the hotbar** (the action bar), refreshed twice a
second. Each learned Manifestation shows as `{icon} Name {status}`, entries
joined by a dark-grey pipe:

```
[icon] Blink READY  |  [icon] Vanish ACTIVE
```

- **READY** (green) — castable now
- **Ns** (red) — seconds left on cooldown
- **ACTIVE** (yellow) — its self-buff is currently running (Vanish, Leap, Bloodthirst)

Level/XP isn't shown here — that's what the XP boss bar is for.

The icons are custom glyphs from the **`ruin:icons`** resource-pack font
(`assets/ruin/font/icons.json` + `assets/ruin/textures/font/*.png`) — one per
manifestation, plus a Ruin sigil used in place of the old `✦` emoji. **They only
render with the resource pack applied;** without it you'll see blank boxes in the
HUD text (the rest still works).

### XP boss bar

On every kill that grants XP, a per-player **boss bar** — styled like the Ender
Dragon's health bar (pink, solid) — pops up at the top of the screen showing your
progress to the next level (`Level 4 — 320 / 900 XP`). It **auto-hides 5 seconds**
after your last XP gain; a kill streak keeps refreshing it. It's per-player
(client-side), so other players don't see yours.

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

Output: `build/libs/Ruin-1.2.0.jar` → drop into your server's `plugins/` folder.
Requires Java 21.
