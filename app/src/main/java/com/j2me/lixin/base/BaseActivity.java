package com.j2me.lixin.base;

import static com.j2me.lixin.util.Constants.PREF_BLACK_BACKGROUND;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.j2me.lixin.R;

public class BaseActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (sp.getBoolean(PREF_BLACK_BACKGROUND, false)) {
			getTheme().applyStyle(R.style.ThemeOverlay_App_Dark, true);
		}
		super.onCreate(savedInstanceState);
		// System bars follow the app surface, not the accent colour: the strip above the
		// content used to be painted with colorPrimary, which read as a purple band.
		int systemBar = com.google.android.material.color.MaterialColors.getColor(this,
				android.R.attr.colorBackground, Color.BLACK);
		boolean night = (getResources().getConfiguration().uiMode
				& android.content.res.Configuration.UI_MODE_NIGHT_MASK)
				== android.content.res.Configuration.UI_MODE_NIGHT_YES;
		EdgeToEdge.enable(
				this,
				night ? SystemBarStyle.dark(systemBar) : SystemBarStyle.light(systemBar, systemBar),
				night ? SystemBarStyle.dark(systemBar) : SystemBarStyle.light(systemBar, systemBar)
		);
		View view = findViewById(android.R.id.content);
		ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
			v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
			return WindowInsetsCompat.CONSUMED;
		});
	}
}
