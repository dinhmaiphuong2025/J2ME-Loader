package ru.playsoftware.j2meloader.hud;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import ru.playsoftware.j2meloader.hud.design.HudColors;
import ru.playsoftware.j2meloader.hud.design.HudType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NroIconPickerDialog {

    public interface OnIconSelectedListener {
        void onIconSelected(String assetPath);
    }

    private NroIconPickerDialog() {}

    public static void show(Context context, OnIconSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_DARK);

        float density = context.getResources().getDisplayMetrics().density;
        int pad = Math.round(14 * density);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16 * density);
        bg.setColor(HudColors.surface);
        bg.setStroke(Math.round(1 * density), HudColors.primarySoft);
        root.setBackground(bg);

        TextView title = new TextView(context);
        title.setText("Kho Icon Kỹ Năng Ngọc Rồng (NRO)");
        title.setTextColor(Color.WHITE);
        title.setTextSize(HudType.BODY_SP);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, Math.round(10 * density));
        root.addView(title);

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(true);

        GridLayout grid = new GridLayout(context);
        int cols = context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 8 : 5;
        grid.setColumnCount(cols);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        AlertDialog dialog = builder.setView(root).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        // List assets from hud_presets/nro/skills
        List<String> files = new ArrayList<>();
        try {
            String[] list = context.getAssets().list("hud_presets/nro/skills");
            if (list != null) {
                for (String f : list) {
                    if (f.endsWith(".png")) files.add("hud_presets/nro/skills/" + f);
                }
            }
        } catch (IOException ignored) {}
        Collections.sort(files);

        int itemSize = Math.round(44 * density);
        int iconPad = Math.round(6 * density);

        for (String assetPath : files) {
            ImageView iv = new ImageView(context);
            Bitmap bm = HudAssetManager.getBitmap(context, assetPath);
            if (bm != null) {
                iv.setImageBitmap(bm);
            }
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setPadding(iconPad, iconPad, iconPad, iconPad);

            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setCornerRadius(8 * density);
            itemBg.setColor(HudColors.surfaceTint);
            itemBg.setStroke(1, HudColors.ripple);
            iv.setBackground(itemBg);

            iv.setClickable(true);
            iv.setFocusable(true);
            iv.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onIconSelected(assetPath);
            });

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = itemSize;
            lp.height = itemSize;
            lp.setMargins(Math.round(3 * density), Math.round(3 * density), Math.round(3 * density), Math.round(3 * density));
            grid.addView(iv, lp);
        }

        scroll.addView(grid, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int scrollH = Math.round(260 * density);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scrollH));

        dialog.show();
        int dialogW = Math.min(Math.round(420 * density), context.getResources().getDisplayMetrics().widthPixels - Math.round(32 * density));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(dialogW, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
