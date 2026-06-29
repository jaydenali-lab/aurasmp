# Ruin Resource Pack

Custom textures for Ruin's custom items. The plugin tags each item with an
`item_model` component, and this pack supplies the matching model + texture:

| Item | `item_model` | Texture |
|------|-------------|---------|
| Mirror Shard | `ruin:mirror_shard` | `assets/ruin/textures/item/mirror_shard.png` |
| Ruin Catalyst | `ruin:catalyst` | `assets/ruin/textures/item/catalyst.png` |

## Using your own textures

Replace the two PNGs (16×16, RGBA/transparent background) with your own art —
**keep the file names the same**. That's it; the models already point at them.
The current PNGs are placeholders (a cyan shard and a purple orb).

## Applying the pack

**Single player / local:** drop the whole `resourcepack` folder (zipped or not)
into `.minecraft/resourcepacks/` and enable it in Options → Resource Packs.

**On a server:** zip the *contents* of this folder (so `pack.mcmeta` is at the
root of the zip, not inside a subfolder), host the zip somewhere with a direct
download URL, then set in `server.properties`:

```
resource-pack=https://your-host/ruin.zip
resource-pack-sha1=<sha1 of the zip>
```

## pack_format note

`pack.mcmeta` uses `pack_format: 46` with a wide `supported_formats` range so it
loads across 1.21.x. If your client warns the pack is "made for a newer/older
version," bump `pack_format` to match your exact version (each Minecraft release
nudges this number) — the textures still work regardless of the warning.
