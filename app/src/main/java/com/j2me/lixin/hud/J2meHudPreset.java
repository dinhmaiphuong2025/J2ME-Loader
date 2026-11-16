package com.j2me.lixin.hud;

import android.content.SharedPreferences;

import com.j2me.lixin.hud.design.HudCornerShape;

import org.json.JSONObject;

import java.util.UUID;

import javax.microedition.lcdui.Canvas;

/**
 * Seeds a J2ME-shaped HUD layout: analogue pad on the left, fire on the right,
 * a row of skill digits and the two soft keys. Both orientations get the same
 * widget set so a game that forces landscape never shows an empty layer.
 *
 * Only applied when the main slot has no widgets yet, so a layout the user has
 * already authored is never overwritten.
 */
final class J2meHudPreset {

    private static final String PREF_PROFILE = "hud_layout_profile_v2";

    private J2meHudPreset() {
    }

    static void ensureDefaults(SharedPreferences prefs) {
        if (!isEmptyMainSlot(prefs.getString(PREF_PROFILE, null))) {
            return;
        }
        HudLayoutProfile profile = new HudLayoutProfile();
        profile.portraitLayout = build();
        profile.landscapeLayout = build();
        try {
            prefs.edit().putString(PREF_PROFILE, profile.toJSON().toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private static boolean isEmptyMainSlot(String json) {
        if (json == null || json.isEmpty()) {
            return true;
        }
        try {
            JSONObject root = new JSONObject(json);
            return empty(root.optJSONObject("portrait_layout"))
                    && empty(root.optJSONObject("landscape_layout"));
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean empty(JSONObject layout) {
        return layout == null
                || layout.optJSONArray("floating_buttons") == null
                || layout.optJSONArray("floating_buttons").length() == 0;
    }

    private static HudLayout build() {
        HudLayout layout = new HudLayout();
        layout.dockEnabled = false;

        layout.floatingButtons.add(pad());
        layout.floatingButtons.add(fire());
        layout.floatingButtons.add(digits());
        layout.floatingButtons.add(soft("L", Canvas.KEY_SOFT_LEFT, 0.07f, 0.10f));
        layout.floatingButtons.add(soft("R", Canvas.KEY_SOFT_RIGHT, 0.93f, 0.10f));
        return layout;
    }

    private static HudButton base(String type, String label, float x, float y, int w, int h) {
        HudButton b = new HudButton();
        b.id = UUID.randomUUID().toString();
        b.widgetType = type;
        b.label = label;
        b.posXPercent = x;
        b.posYPercent = y;
        b.widthDp = w;
        b.heightDp = h;
        b.opacity = 0.55f;
        b.cornerRadiusDp = 16;
        b.cornerShape = HudCornerShape.ALL;
        b.borderStrokeWidthDp = 1;
        return b;
    }

    /** Analogue pad: four held directions, so diagonal thumb drags work. */
    private static HudButton pad() {
        HudButton b = base(HudButton.WIDGET_DPAD, "PAD", 0.17f, 0.68f, 150, 150);
        b.cornerRadiusDp = 75;
        b.dpadUpAction = HudAction.key(Canvas.KEY_UP);
        b.dpadDownAction = HudAction.key(Canvas.KEY_DOWN);
        b.dpadLeftAction = HudAction.key(Canvas.KEY_LEFT);
        b.dpadRightAction = HudAction.key(Canvas.KEY_RIGHT);
        return b;
    }

    private static HudButton fire() {
        HudButton b = base(HudButton.WIDGET_STANDARD, "OK", 0.86f, 0.66f, 78, 78);
        b.cornerRadiusDp = 39;
        b.cornerShape = HudCornerShape.ALL;
        b.action = HudAction.key(Canvas.KEY_FIRE);
        return b;
    }

    /** Skill row 1..9 — one tap per digit, same as the phone keypad. */
    private static HudButton digits() {
        HudButton group = base(HudButton.WIDGET_GROUP, "", 0.50f, 0.90f, 380, 44);
        group.columns = 9;
        group.rows = 1;
        for (int i = 1; i <= 9; i++) {
            HudButton child = base(HudButton.WIDGET_STANDARD, Integer.toString(i), 0.5f, 0.9f, 40, 38);
            child.cornerRadiusDp = 8;
            child.action = HudAction.key(Canvas.KEY_NUM0 + i);
            child.gridSlot = i - 1;
            group.children.add(child);
        }
        return group;
    }

    private static HudButton soft(String label, int keyCode, float x, float y) {
        HudButton b = base(HudButton.WIDGET_STANDARD, label, x, y, 58, 36);
        b.cornerRadiusDp = 10;
        b.action = HudAction.key(keyCode);
        return b;
    }
}
