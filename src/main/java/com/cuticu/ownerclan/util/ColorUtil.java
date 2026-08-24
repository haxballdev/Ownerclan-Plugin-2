package com.cuticu.ownerclan.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.awt.Color;

/**
 * Turns a plain string + a "color" argument (named color, hex, or "rainbow")
 * into a styled Adventure Component, used for owner tags, personal tags and clan tags.
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static boolean isRainbow(String colorArg) {
        return colorArg != null && colorArg.equalsIgnoreCase("rainbow");
    }

    public static Component render(String text, String colorArg) {
        if (isRainbow(colorArg)) {
            return rainbow(text);
        }
        return Component.text(text).color(parseColor(colorArg));
    }

    /**
     * Colors each character of the text across the hue spectrum, giving a
     * smooth rainbow effect that also works fine on a single-character string.
     */
    public static Component rainbow(String text) {
        Component result = Component.empty();
        int len = Math.max(text.length(), 1);
        for (int i = 0; i < text.length(); i++) {
            float hue = (float) i / len;
            Color awt = Color.getHSBColor(hue, 0.9f, 1f);
            TextColor color = TextColor.color(awt.getRed(), awt.getGreen(), awt.getBlue());
            result = result.append(Component.text(String.valueOf(text.charAt(i))).color(color));
        }
        return result;
    }

    public static TextColor parseColor(String colorArg) {
        if (colorArg == null || colorArg.isBlank()) {
            return NamedTextColor.WHITE;
        }
        if (colorArg.startsWith("#")) {
            TextColor hex = TextColor.fromHexString(colorArg);
            return hex != null ? hex : NamedTextColor.WHITE;
        }
        NamedTextColor named = NamedTextColor.NAMES.value(colorArg.toLowerCase());
        return named != null ? named : NamedTextColor.WHITE;
    }

    public static boolean isValidColorArg(String colorArg) {
        if (isRainbow(colorArg)) return true;
        if (colorArg.startsWith("#")) return TextColor.fromHexString(colorArg) != null;
        return NamedTextColor.NAMES.value(colorArg.toLowerCase()) != null;
    }
}
