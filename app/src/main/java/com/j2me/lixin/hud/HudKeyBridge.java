package com.j2me.lixin.hud;

import javax.microedition.lcdui.Canvas;

/**
 * Bridges HUD actions to J2ME (MIDP) key codes.
 *
 * The Anland HUD stores Linux evdev scancodes in {@link HudAction#code} because it
 * feeds a desktop input pipeline. In J2ME Loader we author layouts with MIDP key
 * codes directly, so a code that already is a valid MIDP key passes through
 * unchanged; evdev codes are only translated for layouts imported from Anland.
 */
public final class HudKeyBridge {

    /** Returned when a code cannot be translated to a J2ME key. */
    public static final int UNMAPPED = Integer.MIN_VALUE;

    private HudKeyBridge() {
    }

    /** True when {@code code} is a MIDP key this emulator can deliver. */
    public static boolean isJ2meKey(int code) {
        return (code >= Canvas.KEY_NUM0 && code <= Canvas.KEY_NUM9)
                || code == Canvas.KEY_STAR || code == Canvas.KEY_POUND
                || (code >= Canvas.KEY_RIGHT && code <= Canvas.KEY_UP) // -4..-1
                || code == Canvas.KEY_FIRE
                || code == Canvas.KEY_SOFT_LEFT || code == Canvas.KEY_SOFT_RIGHT
                || code == Canvas.KEY_CLEAR || code == Canvas.KEY_SEND || code == Canvas.KEY_END;
    }

    /** Translates a Linux evdev scancode to a MIDP key, or {@link #UNMAPPED}. */
    public static int fromEvdev(int evdev) {
        switch (evdev) {
            case 2: return Canvas.KEY_NUM1;
            case 3: return Canvas.KEY_NUM2;
            case 4: return Canvas.KEY_NUM3;
            case 5: return Canvas.KEY_NUM4;
            case 6: return Canvas.KEY_NUM5;
            case 7: return Canvas.KEY_NUM6;
            case 8: return Canvas.KEY_NUM7;
            case 9: return Canvas.KEY_NUM8;
            case 10: return Canvas.KEY_NUM9;
            case 11: return Canvas.KEY_NUM0;
            case 103: return Canvas.KEY_UP;
            case 108: return Canvas.KEY_DOWN;
            case 105: return Canvas.KEY_LEFT;
            case 106: return Canvas.KEY_RIGHT;
            case 28: case 96: return Canvas.KEY_FIRE;
            case 55: case 227: return Canvas.KEY_STAR;
            case 83: return Canvas.KEY_POUND;
            case 158: return Canvas.KEY_CLEAR;
            case 1: return Canvas.KEY_SOFT_RIGHT;
            case 139: case 127: return Canvas.KEY_SOFT_LEFT;
            default: return UNMAPPED;
        }
    }

    /** Resolves a stored HUD action code to a MIDP key code. */
    public static int resolve(int code) {
        if (isJ2meKey(code)) {
            return code;
        }
        return fromEvdev(code);
    }

    /** Human readable label for a MIDP key, used by pickers and presets. */
    public static String label(int code) {
        if (code >= Canvas.KEY_NUM0 && code <= Canvas.KEY_NUM9) {
            return Integer.toString(code - Canvas.KEY_NUM0);
        }
        switch (code) {
            case Canvas.KEY_STAR: return "*";
            case Canvas.KEY_POUND: return "#";
            case Canvas.KEY_UP: return "▲";
            case Canvas.KEY_DOWN: return "▼";
            case Canvas.KEY_LEFT: return "◀";
            case Canvas.KEY_RIGHT: return "▶";
            case Canvas.KEY_FIRE: return "OK";
            case Canvas.KEY_SOFT_LEFT: return "L";
            case Canvas.KEY_SOFT_RIGHT: return "R";
            case Canvas.KEY_CLEAR: return "C";
            case Canvas.KEY_SEND: return "S";
            case Canvas.KEY_END: return "E";
            default: return "?";
        }
    }

    /** The 21 keys a J2ME game can receive, in picker order. */
    public static final int[] ALL_KEYS = {
            Canvas.KEY_NUM1, Canvas.KEY_NUM2, Canvas.KEY_NUM3,
            Canvas.KEY_NUM4, Canvas.KEY_NUM5, Canvas.KEY_NUM6,
            Canvas.KEY_NUM7, Canvas.KEY_NUM8, Canvas.KEY_NUM9,
            Canvas.KEY_NUM0, Canvas.KEY_STAR, Canvas.KEY_POUND,
            Canvas.KEY_UP, Canvas.KEY_DOWN, Canvas.KEY_LEFT, Canvas.KEY_RIGHT,
            Canvas.KEY_FIRE, Canvas.KEY_SOFT_LEFT, Canvas.KEY_SOFT_RIGHT,
            Canvas.KEY_CLEAR, Canvas.KEY_SEND,
    };
}
