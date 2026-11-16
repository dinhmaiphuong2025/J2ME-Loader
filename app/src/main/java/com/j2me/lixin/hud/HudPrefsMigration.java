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
                return;
            }
        } catch (Exception ignored) {
            // A malformed profile is replaced by the defaults on load anyway.
        }
    }

    private static boolean isLayoutEmpty(JSONObject layout) {
        if (layout == null) {
            return true;
        }
        return layout.optJSONArray("floating_buttons") == null
                || layout.optJSONArray("floating_buttons").length() == 0;
    }
}
