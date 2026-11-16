package com.j2me.lixin.hud;

import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * One-time repair for HUD profiles saved by the unmodified Anland editor.
 *
 * Anland keeps four layout slots and defaults to slot 2, so a layout the user
 * authored ended up under {@code layout_3} while the game renders slot 0
 * (the main portrait/landscape layouts). This copies the first non-empty extra
 * slot into slot 0 when slot 0 is still empty, so existing edits are not lost.
 */
final class HudPrefsMigration {

    private static final String PREF_PROFILE = "hud_layout_profile_v2";

    private HudPrefsMigration() {
    }

    static void migrateExtraSlotToMain(SharedPreferences prefs) {
        String json = prefs.getString(PREF_PROFILE, null);
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JSONObject root = new JSONObject(json);
            if (!isLayoutEmpty(root.optJSONObject("portrait_layout"))
                    || !isLayoutEmpty(root.optJSONObject("landscape_layout"))) {
                return;
            }
            for (String key : new String[]{"layout_2", "layout_3", "layout_4"}) {
                JSONObject extra = root.optJSONObject(key);
                if (extra == null) {
                    continue;
                }
                JSONObject portrait = extra.optJSONObject("portrait");
                JSONObject landscape = extra.optJSONObject("landscape");
                if (isLayoutEmpty(portrait) && isLayoutEmpty(landscape)) {
                    continue;
                }
                if (portrait != null) {
                    root.put("portrait_layout", portrait);
                }
                if (landscape != null) {
                    root.put("landscape_layout", landscape);
                }
                prefs.edit().putString(PREF_PROFILE, root.toString()).apply();
                mirrorOrientation(root, prefs);
                return;
            }
            mirrorOrientation(root, prefs);
        } catch (Exception ignored) {
            // A malformed profile is replaced by the defaults on load anyway.
        }
    }

    /**
     * A game that forces landscape showed an empty HUD because the authored widgets
     * only existed in the portrait layout. Mirror whichever side has content.
     */
    private static void mirrorOrientation(JSONObject root, SharedPreferences prefs) {
        JSONObject portrait = root.optJSONObject("portrait_layout");
        JSONObject landscape = root.optJSONObject("landscape_layout");
        boolean portraitEmpty = isLayoutEmpty(portrait);
        boolean landscapeEmpty = isLayoutEmpty(landscape);
        if (portraitEmpty == landscapeEmpty) {
            return;
        }
        try {
            if (landscapeEmpty && portrait != null) {
                root.put("landscape_layout", new JSONObject(portrait.toString()));
            } else if (portraitEmpty && landscape != null) {
                root.put("portrait_layout", new JSONObject(landscape.toString()));
            }
            prefs.edit().putString(PREF_PROFILE, root.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** Bumps widgets saved with Anland's 10% ghost opacity to a readable level. */
    static void repairOpacity(SharedPreferences prefs) {
        String json = prefs.getString(PREF_PROFILE, null);
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JSONObject root = new JSONObject(json);
            boolean changed = false;
            for (String key : new String[]{"portrait_layout", "landscape_layout",
                    "keyboard_portrait_layout", "keyboard_landscape_layout"}) {
                changed |= bump(root.optJSONObject(key));
            }
            for (String key : new String[]{"layout_2", "layout_3", "layout_4"}) {
                JSONObject extra = root.optJSONObject(key);
                if (extra == null) continue;
                changed |= bump(extra.optJSONObject("portrait"));
                changed |= bump(extra.optJSONObject("landscape"));
            }
            if (changed) {
                prefs.edit().putString(PREF_PROFILE, root.toString()).apply();
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean bump(JSONObject layout) {
        if (layout == null) return false;
        org.json.JSONArray arr = layout.optJSONArray("floating_buttons");
        if (arr == null) return false;
        boolean changed = false;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject b = arr.optJSONObject(i);
            changed |= bumpOne(b);
            if (b != null) {
                org.json.JSONArray kids = b.optJSONArray("children");
                if (kids != null) {
                    for (int j = 0; j < kids.length(); j++) {
                        changed |= bumpOne(kids.optJSONObject(j));
                    }
                }
            }
        }
        return changed;
    }

    private static boolean bumpOne(JSONObject b) {
        if (b == null || !b.has("opacity")) return false;
        double op = b.optDouble("opacity", 0.55);
        if (op > 0.05 && op < 0.20) {
            try {
                b.put("opacity", 0.55);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * Converts PC (evdev) key codes saved by Anland's picker into J2ME key codes.
     * Without this, most buttons carry scancodes the emulator can never deliver.
     */
    static void normalizeKeyCodes(SharedPreferences prefs) {
        if (prefs.getBoolean("code_space_midp_v1", false)) {
            return;
        }
        prefs.edit().putBoolean("code_space_midp_v1", true).apply();
        String json = prefs.getString(PREF_PROFILE, null);
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JSONObject root = new JSONObject(json);
            boolean changed = false;
            for (String key : new String[]{"portrait_layout", "landscape_layout",
                    "keyboard_portrait_layout", "keyboard_landscape_layout"}) {
                changed |= walk(root.optJSONObject(key));
            }
            for (String key : new String[]{"layout_2", "layout_3", "layout_4"}) {
                JSONObject extra = root.optJSONObject(key);
                if (extra == null) continue;
                changed |= walk(extra.optJSONObject("portrait"));
                changed |= walk(extra.optJSONObject("landscape"));
            }
            if (changed) {
                prefs.edit().putString(PREF_PROFILE, root.toString()).apply();
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean walk(JSONObject layout) {
        if (layout == null) return false;
        org.json.JSONArray arr = layout.optJSONArray("floating_buttons");
        if (arr == null) return false;
        boolean changed = false;
        for (int i = 0; i < arr.length(); i++) {
            changed |= walkButton(arr.optJSONObject(i));
        }
        return changed;
    }

    private static boolean walkButton(JSONObject b) {
        if (b == null) return false;
        boolean changed = false;
        changed |= fixAction(b.optJSONObject("action"));
        changed |= fixAction(b.optJSONObject("long_press_action"));
        changed |= fixAction(b.optJSONObject("popup_action"));
        for (String key : new String[]{"swipe_left", "swipe_right", "swipe_up", "swipe_down",
                "dpad_up", "dpad_down", "dpad_left", "dpad_right"}) {
            changed |= fixAction(b.optJSONObject(key));
        }
        org.json.JSONArray kids = b.optJSONArray("children");
        if (kids != null) {
            for (int j = 0; j < kids.length(); j++) {
                changed |= walkButton(kids.optJSONObject(j));
            }
        }
        return changed;
    }

    private static boolean fixAction(JSONObject a) {
        if (a == null) return false;
        String type = a.optString("type");
        boolean changed = false;
        if ("key".equals(type)) {
            if (!a.has("space")) {
                int code = a.optInt("code", 0);
                int mapped = HudKeyBridge.fromEvdev(code);
                try {
                    if (mapped != HudKeyBridge.UNMAPPED) {
                        a.put("code", mapped);
                        a.put("space", "midp");
                        changed = true;
                    } else if (code >= 48 && code <= 57) {
                        // 48..57 is B/N/M/,/./\/RSHIFT/KP*/LALT/SPACE on a PC but
                        // digits 0..9 in J2ME. Guessing wrong types numbers as letters,
                        // so leave the button unassigned instead.
                        a.put("code", 0);
                        changed = true;
                    }
                } catch (Exception ignored) {
                }
            }
        } else if ("combo".equals(type)) {
            org.json.JSONArray keys = a.optJSONArray("combo_keys");
            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    int code = keys.optInt(i, Integer.MIN_VALUE);
                    int mapped = HudKeyBridge.fromEvdev(code);
                    if (mapped != HudKeyBridge.UNMAPPED) {
                        try {
                            keys.put(i, mapped);
                            changed = true;
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        return changed;
    }

    private static int remap(int code) {
        if (HudKeyBridge.isJ2meKey(code) || code == 0) return code;
        int mapped = HudKeyBridge.fromEvdev(code);
        return mapped == HudKeyBridge.UNMAPPED ? code : mapped;
    }

    private static boolean isLayoutEmpty(JSONObject layout) {
        if (layout == null) {
            return true;
        }
        return layout.optJSONArray("floating_buttons") == null
                || layout.optJSONArray("floating_buttons").length() == 0;
    }
}
