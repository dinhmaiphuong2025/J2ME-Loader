package com.j2me.lixin.config;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.j2me.lixin.R;
import com.j2me.lixin.util.Constants;

import java.io.File;

import javax.microedition.shell.MicroActivity;

/**
 * The screen a player actually sees before a game starts: one big Play button and
 * a handful of obvious choices. Everything technical lives behind "Nâng cao".
 */
public class QuickConfigActivity extends AppCompatActivity {

    private ProfileModel params;
    private File configDir;
    private String appPath;
    private String appName;

    private MaterialButtonToggleGroup controlGroup;
    private MaterialButtonToggleGroup qualityGroup;
    private MaterialButtonToggleGroup orientationGroup;
    private MaterialSwitch fullscreenSwitch;
    private MaterialSwitch smoothSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        appPath = intent.getDataString();
        appName = intent.getStringExtra(Constants.KEY_MIDLET_NAME);
        if (appPath == null) {
            finish();
            return;
        }
        File appDir = new File(appPath);
        File workDir = appDir.getParentFile() != null ? appDir.getParentFile().getParentFile() : null;
        if (workDir == null) {
            finish();
            return;
        }
        configDir = new File(workDir + Config.MIDLET_CONFIGS_DIR + appDir.getName());
        //noinspection ResultOfMethodCallIgnored
        configDir.mkdirs();
        params = ProfilesManager.loadConfig(configDir);
        if (params == null) {
            params = new ProfileModel(configDir);
        }
        setTitle(appName != null ? appName : getString(R.string.app_name));
        buildUi();
        syncFromParams();
    }

    private SharedPreferences hudPrefs() {
        return getSharedPreferences("hud_" + Integer.toHexString(appPath.hashCode()), MODE_PRIVATE);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        MaterialButton play = new MaterialButton(this);
        play.setText(R.string.quick_play);
        play.setTextSize(20);
        play.setOnClickListener(v -> {
            saveAll();
            startGame();
        });
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        playLp.bottomMargin = dp(12);
        root.addView(play, playLp);

        controlGroup = toggleRow(root, getString(R.string.quick_section_control),
                new String[]{getString(R.string.input_mode_none),
                        getString(R.string.input_mode_keypad),
                        getString(R.string.input_mode_hud)}, control -> {
                    // handled in saveAll()
                });

        qualityGroup = toggleRow(root, getString(R.string.quick_section_quality),
                new String[]{getString(R.string.quick_quality_smooth),
                        getString(R.string.quick_quality_pretty),
                        getString(R.string.quick_quality_battery)}, q -> {
                    if (q == 0) {
                        params.screenFilter = true;
                        params.fpsLimit = 60;
                    } else if (q == 1) {
                        params.screenFilter = true;
                        params.fpsLimit = 0;
                        params.forceFullscreen = true;
                    } else {
                        params.screenFilter = false;
                        params.fpsLimit = 30;
                    }
                    syncFromParams();
                    saveAll();
                });

        orientationGroup = toggleRow(root, getString(R.string.quick_section_orientation),
                new String[]{getString(R.string.pref_orientation_auto),
                        getString(R.string.pref_orientation_land),
                        getString(R.string.pref_orientation_port)}, o -> {
                    params.orientation = o == 0 ? 1 : (o == 1 ? 3 : 2);
                    saveAll();
                });

        fullscreenSwitch = switchRow(root, getString(R.string.quick_fullscreen), checked -> {
            params.forceFullscreen = checked;
            saveAll();
        });
        smoothSwitch = switchRow(root, getString(R.string.quick_smooth), checked -> {
            params.screenFilter = checked;
            saveAll();
        });

        MaterialButton advanced = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        advanced.setText(R.string.quick_advanced);
        advanced.setOnClickListener(v -> {
            saveAll();
            Intent i = new Intent(this, ConfigActivity.class);
            i.setAction(Constants.ACTION_EDIT);
            i.setData(getIntent().getData());
            i.putExtra(Constants.KEY_MIDLET_NAME, appName);
            startActivity(i);
        });
        LinearLayout.LayoutParams advLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        advLp.topMargin = dp(16);
        root.addView(advanced, advLp);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private MaterialButtonToggleGroup toggleRow(LinearLayout parent, String title,
                                                String[] labels, IntConsumer onPick) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextSize(15);
        header.setPadding(0, 0, 0, dp(8));
        inner.addView(header);

        MaterialButtonToggleGroup group = new MaterialButtonToggleGroup(this);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        for (int i = 0; i < labels.length; i++) {
            MaterialButton b = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(labels[i]);
            b.setId(View.generateViewId());
            b.setTag(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            group.addView(b, lp);
        }
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return;
            View v = g.findViewById(checkedId);
            if (v != null && v.getTag() instanceof Integer) {
                onPick.accept((Integer) v.getTag());
            }
        });
        inner.addView(group);
        card.addView(inner);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(12);
        parent.addView(card, cardLp);
        return group;
    }

    private MaterialSwitch switchRow(LinearLayout parent, String title, BoolConsumer onChange) {
        MaterialCardView card = new MaterialCardView(this);
        MaterialSwitch sw = new MaterialSwitch(this);
        sw.setText(title);
        sw.setPadding(dp(14), dp(12), dp(14), dp(12));
        sw.setOnCheckedChangeListener((b, checked) -> onChange.accept(checked));
        card.addView(sw);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(12);
        parent.addView(card, cardLp);
        return sw;
    }

    private void syncFromParams() {
        boolean hud = hudPrefs().getBoolean("use_hud_overlay", false);
        int control = hud ? 2 : (params.showKeyboard ? 1 : 0);
        check(controlGroup, control);

        int quality = params.fpsLimit == 0 ? 1 : (params.fpsLimit <= 30 ? 2 : 0);
        check(qualityGroup, quality);

        int orientation = params.orientation == 2 ? 2 : (params.orientation == 3 ? 1 : 0);
        check(orientationGroup, orientation);

        fullscreenSwitch.setChecked(params.forceFullscreen);
        smoothSwitch.setChecked(params.screenFilter);
    }

    private void check(MaterialButtonToggleGroup group, int index) {
        if (group.getChildCount() > index) {
            group.check(group.getChildAt(index).getId());
        }
    }

    private int selected(MaterialButtonToggleGroup group) {
        View v = group.findViewById(group.getCheckedButtonId());
        return v != null && v.getTag() instanceof Integer ? (Integer) v.getTag() : 0;
    }

    private void saveAll() {
        if (params == null) {
            return;
        }
        int control = selected(controlGroup);
        params.showKeyboard = control == 1;
        hudPrefs().edit().putBoolean("use_hud_overlay", control == 2).apply();
        ProfilesManager.saveConfig(params);
    }

    private void startGame() {
        Intent i = new Intent(this, MicroActivity.class);
        i.setData(getIntent().getData());
        i.putExtra(Constants.KEY_MIDLET_NAME, appName);
        startActivity(i);
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private interface BoolConsumer {
        void accept(boolean value);
    }
}
