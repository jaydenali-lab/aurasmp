package com.aurasmp.ruin.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Helpers for rendering the custom resource-pack icons. The glyphs live in the
 * {@code ruin:icons} font (assets/ruin/font/icons.json); rendering them in white
 * lets their own colours show through (Minecraft tints font glyphs by text colour).
 *
 * <p>Without the resource pack applied these characters render as blank/boxes —
 * the icons require the pack.
 */
public final class Glyphs {

    public static final Key FONT = Key.key("ruin", "icons");
    /** The Ruin sigil (U+E000) — used as a message prefix in place of an emoji. */
    public static final String SIGIL = "\uE000";

    private Glyphs() {}

    public static Component of(String glyph) {
        return Component.text(glyph)
                .font(FONT)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component sigil() {
        return of(SIGIL);
    }
}
