/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2021 Nikita Shakarun
 * Copyright 2019-2023 Yury Kharchenko
 * Copyright 2022-2024 Arman Jussupgaliyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.shell;

import static com.j2me.lixin.util.Constants.*;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

import org.acra.ACRA;
import org.acra.ErrorReporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.ViewHandler;
import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.lcdui.keyboard.VirtualKeyboard;
import javax.microedition.location.LocationProviderImpl;
import javax.microedition.util.ContextHolder;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import com.j2me.lixin.BuildConfig;
import com.j2me.lixin.ForegroundService;
import com.j2me.lixin.R;
import com.j2me.lixin.applist.AppItem;
import com.j2me.lixin.config.Config;
import com.j2me.lixin.databinding.ActivityMicroBinding;
import com.j2me.lixin.util.AccountManager;
import com.j2me.lixin.util.AppUtils;
import com.j2me.lixin.util.Constants;
import com.j2me.lixin.util.LogUtils;

public class MicroActivity extends AppCompatActivity implements J2meContext.UiHandler {
	private static final int ORIENTATION_DEFAULT = 0;
	private static final int ORIENTATION_AUTO = 1;
	private static final int ORIENTATION_PORTRAIT = 2;
	private static final int ORIENTATION_LANDSCAPE = 3;

	private Displayable current;
	private boolean visible;
	private boolean pauseSuppressed;
	private boolean pauseDisabled;
	private boolean statusBarEnabled;
	private boolean cutoutsEnabled;
	private MicroLoader microLoader;
	private J2meContext context;
	private String appName;
	private InputMethodManager inputMethodManager;
	private boolean imeVisible;
	private int statusBarInset;
	private int menuKey;
	private String appPath;
	private final List<Tab> tabs = new ArrayList<>();
	private int activeIndex = -1;
	private TextBox activeTextBox;
	private Canvas lastActiveCanvas;
	private TextWatcher invisibleInputWatcher;

	public ActivityMicroBinding binding;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (sp.getBoolean(PREF_BLACK_BACKGROUND, false)) {
			getTheme().applyStyle(R.style.ThemeOverlay_App_Dark, true);
		}
		lockNightMode();

		super.onCreate(savedInstanceState);
		ContextHolder.setCurrentActivity(this);
		ContextCompat.startForegroundService(this, new Intent(this, ForegroundService.class));

		binding = ActivityMicroBinding.inflate(getLayoutInflater());
		View view = binding.getRoot();
		setContentView(view);
		binding.btnMenu.setOnClickListener(v -> showMenu());
		binding.btnAddTab.setOnClickListener(v -> addNewTab());

		statusBarEnabled = sp.getBoolean(PREF_STATUSBAR, false);
		cutoutsEnabled = sp.getBoolean(PREF_ADD_CUTOUT_AREA, false) &&
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
		setupInsets();
		if (sp.getBoolean(PREF_KEEP_SCREEN, true)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		ContextHolder.setVibration(sp.getBoolean(PREF_VIBRATION, true));
		Canvas.setScreenshotRawMode(sp.getBoolean(PREF_SCREENSHOT_SWITCH, false));
		Intent intent = getIntent();
		if (BuildConfig.FULL_EMULATOR) {
			appName = intent.getStringExtra(KEY_MIDLET_NAME);
			Uri data = intent.getData();
			if (data == null) {
				showErrorDialog("Invalid intent: app path is null");
				return;
			}
			appPath = data.toString();
		} else {
			appName = getTitle().toString();
			appPath = getApplicationInfo().dataDir + "/files/converted/midlet";
			File dir = new File(appPath);
			if (!dir.exists() && !dir.mkdirs()) {
				throw new RuntimeException("Can't access file system");
			}
		}
		String arguments = intent.getStringExtra(KEY_START_ARGUMENTS);
		if (arguments != null) {
			MidletSystem.setProperty("com.nokia.mid.cmdline", arguments);
			String[] arr = arguments.split(";");
			for (String s: arr) {
				if (s.length() == 0) {
					continue;
				}
				if (s.contains("=")) {
					int i = s.indexOf('=');
					String k = s.substring(0, i);
					String v = s.substring(i + 1);
					MidletSystem.setProperty(k, v);
				} else {
					MidletSystem.setProperty(s, "");
				}
			}
		}
		MidletSystem.setProperty("com.nokia.mid.cmdline.instance", "1");
		microLoader = new MicroLoader(this, appPath);
		if (!microLoader.init()) {
			Config.startApp(this, appName, appPath, true, arguments);
			finish();
			return;
		}
		inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		showAccountPicker(appName, appPath, true);
	}

	private void launchFirstTab(AccountManager.Account account) {
		String accName = account != null ? account.getName() : AccountManager.DEFAULT_ACCOUNT;
		String accDir = account != null ? account.getDir() : "";
		context = new J2meContext(appName);
		context.setAccountName(accName);
		context.setAccountDir(accDir);
		context.setUiHandler(this);
		context.register();
		tabs.add(new Tab(context, microLoader, appName, appPath, accName));
		activeIndex = 0;
		addTabPill(0);
		microLoader.applyConfiguration();
		setupVirtualKeyboard();
		pauseDisabled = microLoader.isPauseDisabled();
		menuKey = microLoader.getMenuKeyCode();

		try {
			loadMIDlet();
			MidletThread.resumeApp();
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog(e.toString());
		}
	}

	public void lockNightMode() {
		int current = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		if (current == Configuration.UI_MODE_NIGHT_YES) {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
		} else {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public void onResume() {
		super.onResume();
		visible = true;
		if (context != null) {
			context.register();
			MidletThread.resumeApp();
		}
	}

	@Override
	public void onPause() {
		hideSoftInput();
		visible = false;
		super.onPause();
	}

	private void hideSoftInput() {
		if (inputMethodManager != null) {
			IBinder windowToken = binding.displayableContainer.getWindowToken();
			inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && current instanceof Canvas) {
			hideSystemUI();
		}
	}

	@SuppressLint("SourceLockedOrientationActivity")
	private void setOrientation(int orientation) {
		switch (orientation) {
			case ORIENTATION_AUTO:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
				break;
			case ORIENTATION_PORTRAIT:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
				break;
			case ORIENTATION_LANDSCAPE:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
				break;
			case ORIENTATION_DEFAULT:
			default:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				break;
		}
	}

	private void loadMIDlet() throws Exception {
		LinkedHashMap<String, String> midlets = microLoader.loadMIDletList();
		int size = midlets.size();
		String[] midletsNameArray = midlets.values().toArray(new String[0]);
		String[] midletsClassArray = midlets.keySet().toArray(new String[0]);
		if (size == 0) {
			throw new Exception("No MIDlets found");
		} else if (size == 1) {
			MidletThread.create(microLoader, midletsClassArray[0], context);
		} else {
			showMidletDialog(midletsNameArray, midletsClassArray);
		}
	}

	private void showMidletDialog(String[] names, final String[] classes) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.select_dialog_title)
				.setItems(names, (d, n) -> {
					String clazz = classes[n];
					ErrorReporter errorReporter = ACRA.getErrorReporter();
					String report = errorReporter.getCustomData(Constants.KEY_APPCENTER_ATTACHMENT);
					StringBuilder sb = new StringBuilder();
					if (report != null) {
						sb.append(report).append("\n");
					}
					sb.append("Begin app: ").append(names[n]).append(", ").append(clazz);
					errorReporter.putCustomData(Constants.KEY_APPCENTER_ATTACHMENT, sb.toString());
					MidletThread.create(microLoader, clazz, context);
					MidletThread.resumeApp();
				})
				.setOnCancelListener(d -> {
					d.dismiss();
					MidletThread.notifyDestroyed();
				});
		builder.show();
	}

	void showErrorDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.error)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, (d, w) -> MidletThread.notifyDestroyed());
		builder.setOnCancelListener(dialogInterface -> MidletThread.notifyDestroyed());
		builder.show();
	}

	private void setupInsets() {
		EdgeToEdge.enable(
				this,
				SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primary)),
				SystemBarStyle.dark(Color.BLACK)
		);

		ViewCompat.setOnApplyWindowInsetsListener(binding.midletFrame, (v, windowInsets) -> {
			int typeMask = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime();
			if (!cutoutsEnabled) {
				typeMask |= WindowInsetsCompat.Type.displayCutout();
			}
			Insets insets = windowInsets.getInsets(typeMask);
			statusBarInset = insets.top;

			ViewGroup.LayoutParams params = binding.statusbarView.getLayoutParams();
			params.height = insets.top;
			binding.statusbarView.setLayoutParams(params);

			v.setPadding(insets.left, 0, insets.right, insets.bottom);
			updateOverlayLocation();
			return WindowInsetsCompat.CONSUMED;
		});

		// On API < 30 the IME insets are not dispatched
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
			getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
				WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(binding.midletFrame);
				boolean visible = rootInsets != null
						&& rootInsets.isVisible(WindowInsetsCompat.Type.ime());
				if (visible == imeVisible) {
					return;
				}
				imeVisible = visible;
				if (current instanceof Canvas) {
					if (visible) {
						showSystemUI();
					} else {
						hideSystemUI();
					}
				} else { ViewCompat.requestApplyInsets(binding.midletFrame); }
			});
		}
	}

	private void updateOverlayLocation() {
		binding.overlayView.setLocation(0, statusBarInset + binding.topBar.getHeight());
	}

	private WindowInsetsControllerCompat getWindowInsetsController() {
		Window window = getWindow();
		return WindowCompat.getInsetsController(window, window.getDecorView());
	}

	private void hideSystemUI() {
		int typeMask = WindowInsetsCompat.Type.navigationBars();
		if (!statusBarEnabled) {
			typeMask |= WindowInsetsCompat.Type.statusBars();
		}

		WindowInsetsControllerCompat windowInsetsController = getWindowInsetsController();
		windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
		windowInsetsController.hide(typeMask);
		// For API < 30
		ViewCompat.requestApplyInsets(binding.midletFrame);
	}

	private void showSystemUI() {
		WindowInsetsControllerCompat windowInsetsController = getWindowInsetsController();
		windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
		// For API < 30
		ViewCompat.requestApplyInsets(binding.midletFrame);
	}

	public void setCurrent(Displayable displayable) {
		ViewHandler.postEvent(new SetCurrentEvent(current, displayable));
		current = displayable;
	}

	public Displayable getCurrent() {
		return current;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setPauseSuppressed(boolean pauseSuppressed) {
		this.pauseSuppressed = pauseSuppressed;
	}

	public void showExitConfirmation() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(R.string.CONFIRMATION_REQUIRED)
				.setMessage(R.string.FORCE_CLOSE_CONFIRMATION)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					hideSoftInput();
					MidletThread.destroyApp();
				})
				.setNeutralButton(R.string.action_settings, (d, w) -> {
					hideSoftInput();
					Config.startApp(this, appName, appPath, true);
					MidletThread.destroyApp();
				})
				.setNegativeButton(android.R.string.cancel, null);
		alertBuilder.create().show();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_MENU)
			if (current instanceof Canvas && binding.displayableContainer.dispatchKeyEvent(event)) {
				return true;
			} else if (event.getAction() == KeyEvent.ACTION_DOWN) {
				if (event.getRepeatCount() == 0) {
					event.startTracking();
					return true;
				} else if (event.isLongPress()) {
					return onKeyLongPress(event.getKeyCode(), event);
				}
			} else if (event.getAction() == KeyEvent.ACTION_UP) {
				return onKeyUp(event.getKeyCode(), event);
			}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void openOptionsMenu() {
		showMenu();
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == menuKey || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
			showExitConfirmation();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if ((keyCode == menuKey || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)
				&& (event.getFlags() & (KeyEvent.FLAG_LONG_PRESS | KeyEvent.FLAG_CANCELED)) == 0) {
			openOptionsMenu();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		// Intentionally overridden by empty due to support for back-key remapping.
	}

	private void showMenu() {
		PopupMenu popup = new PopupMenu(this, binding.btnMenu);
		popup.inflate(R.menu.midlet_displayable);
		Menu menu = popup.getMenu();
		menu.findItem(R.id.action_lock_orientation).setVisible(true);
		if (inputMethodManager == null) {
			menu.findItem(R.id.action_ime_keyboard).setVisible(false);
		}
		if (ContextHolder.getVk() == null) {
			menu.findItem(R.id.action_submenu_vk).setVisible(false);
		}
		if (current instanceof Canvas) {
			menu.setGroupVisible(R.id.action_group_canvas, true);
			VirtualKeyboard vk = ContextHolder.getVk();
			if (vk != null) {
				boolean visible = vk.getLayoutEditMode() != VirtualKeyboard.LAYOUT_EOF;
				menu.findItem(R.id.action_layout_edit_finish).setVisible(visible);
			}
		} else {
			menu.setGroupVisible(R.id.action_group_canvas, false);
		}
		popup.setOnMenuItemClickListener(this::onOptionsItemSelected);
		popup.show();
	}

	private void addNewTab() {
		new Thread(() -> {
			List<AppItem> apps = AppUtils.getInstalledApps();
			runOnUiThread(() -> showGamePicker(apps));
		}, "GamePicker").start();
	}

	private void showGamePicker(List<AppItem> apps) {
		if (apps.isEmpty()) {
			return;
		}
		String[] names = new String[apps.size()];
		for (int i = 0; i < apps.size(); i++) {
			names[i] = apps.get(i).getTitle();
		}
		new AlertDialog.Builder(this)
				.setTitle(R.string.select_dialog_title)
				.setItems(names, (d, which) -> {
					AppItem item = apps.get(which);
					showAccountPicker(item.getTitle(), item.getPathExt(), false);
				})
				.show();
	}

	private void showAccountPicker(String name, String path, boolean isFirstTab) {
		List<AccountManager.Account> allAccounts = AccountManager.getAccountList(path);
		Set<String> runningAccounts = new HashSet<>();
		for (Tab t : tabs) {
			if (path.equals(t.appPath)) {
				runningAccounts.add(t.accountName != null ? t.accountName : AccountManager.DEFAULT_ACCOUNT);
			}
		}

		List<AccountManager.Account> available = new ArrayList<>();
		for (AccountManager.Account acc : allAccounts) {
			if (!runningAccounts.contains(acc.getName())) {
				available.add(acc);
			}
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.select_account_title) + " - " + name);

		ListView listView = new ListView(this);
		listView.setDivider(null);
		listView.setDividerHeight(0);

		BaseAdapter adapter = new BaseAdapter() {
			@Override
			public int getCount() {
				return available.size();
			}

			@Override
			public Object getItem(int position) {
				return available.get(position);
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = getLayoutInflater().inflate(R.layout.item_dialog_account, parent, false);
				}
				AccountManager.Account acc = available.get(position);
				TextView tvName = convertView.findViewById(R.id.account_name);
				ImageButton btnEdit = convertView.findViewById(R.id.btn_edit_account);

				tvName.setText(acc.getName());

				btnEdit.setFocusable(false);
				btnEdit.setOnClickListener(v -> {
					showRenameAccountDialog(path, acc.getName(), () -> {
						available.clear();
						List<AccountManager.Account> updated = AccountManager.getAccountList(path);
						for (AccountManager.Account a : updated) {
							if (!runningAccounts.contains(a.getName())) {
								available.add(a);
							}
						}
						notifyDataSetChanged();
					});
				});

				return convertView;
			}
		};

		listView.setAdapter(adapter);
		builder.setView(listView);

		AlertDialog dialog = builder.create();

		listView.setOnItemClickListener((parent, view, position, id) -> {
			AccountManager.Account chosen = available.get(position);
			dialog.dismiss();
			if (isFirstTab) {
				launchFirstTab(chosen);
			} else {
				addNewGameTab(name, path, chosen);
			}
		});

		dialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.create_new_account), (d, which) -> {
			showCreateAccountDialog(name, path, isFirstTab);
		});

		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), (d, which) -> {
			if (isFirstTab) {
				finish();
			}
		});

		if (isFirstTab) {
			dialog.setOnCancelListener(d -> finish());
		}
		dialog.show();
	}

	private void showRenameAccountDialog(String appPath, String currentName, Runnable onRenamed) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit);

		final EditText input = new EditText(this);
		input.setText(currentName);
		input.setSelection(currentName.length());
		input.setSingleLine(true);
		float density = getResources().getDisplayMetrics().density;
		int padding = (int) (16 * density);
		FrameLayout container = new FrameLayout(this);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = padding;
		params.rightMargin = padding;
		input.setLayoutParams(params);
		container.addView(input);
		builder.setView(container);

		builder.setPositiveButton(android.R.string.ok, (d, which) -> {
			String newName = input.getText().toString().trim();
			if (!newName.isEmpty() && !newName.equals(currentName)) {
				AccountManager.renameAccount(appPath, currentName, newName);
				if (onRenamed != null) {
					onRenamed.run();
				}
			}
		});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}

	private void showCreateAccountDialog(String name, String path, boolean isFirstTab) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.new_account_title);

		final EditText input = new EditText(this);
		input.setHint(R.string.account_name_hint);
		input.setSingleLine(true);
		float density = getResources().getDisplayMetrics().density;
		int padding = (int) (16 * density);
		FrameLayout container = new FrameLayout(this);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = padding;
		params.rightMargin = padding;
		input.setLayoutParams(params);
		container.addView(input);
		builder.setView(container);

		builder.setPositiveButton(android.R.string.ok, (d, which) -> {
			String accName = input.getText().toString().trim();
			accName = AccountManager.sanitizeAccountName(accName);
			if (accName.isEmpty()) {
				accName = "acc_" + (System.currentTimeMillis() % 10000);
			}
			AccountManager.Account created = AccountManager.addAccount(path, accName);
			if (isFirstTab) {
				launchFirstTab(created);
			} else {
				addNewGameTab(name, path, created);
			}
		});
		builder.setNegativeButton(android.R.string.cancel, (d, which) -> {
			if (isFirstTab) {
				finish();
			}
		});
		if (isFirstTab) {
			builder.setOnCancelListener(d -> finish());
		}
		builder.show();
	}

	private void addNewGameTab(String name, String path, AccountManager.Account account) {
		String accName = account != null ? account.getName() : AccountManager.DEFAULT_ACCOUNT;
		String accDir = account != null ? account.getDir() : "";
		J2meContext newContext = new J2meContext(name);
		newContext.setAccountName(accName);
		newContext.setAccountDir(accDir);
		newContext.setUiHandler(this);
		newContext.register();
		MicroLoader loader = new MicroLoader(this, path);
		if (!loader.init()) {
			newContext.unregister();
			Config.startApp(this, name, path, true);
			return;
		}
		tabs.add(new Tab(newContext, loader, name, path, accName));
		int newIndex = tabs.size() - 1;
		addTabPill(newIndex);
		switchTo(newIndex);
		try {
			loadMIDlet();
			MidletThread.resumeApp();
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog(e.toString());
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_exit_midlet) {
			showExitConfirmation();
		} else if (id == R.id.action_save_log) {
			saveLog();
		} else if (id == R.id.action_lock_orientation) {
			if (item.isChecked()) {
				VirtualKeyboard vk = ContextHolder.getVk();
				int orientation = vk != null && vk.isPhone() ? ORIENTATION_PORTRAIT : microLoader.getOrientation();
				setOrientation(orientation);
				item.setChecked(false);
			} else {
				item.setChecked(true);
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
			}
		} else if (id == R.id.action_ime_keyboard) {
			if (current instanceof Canvas) {
				binding.floatingInputBar.setVisibility(View.VISIBLE);
				InvisibleInputView edit = binding.floatingInputEdit;
				edit.setInputType(InputType.TYPE_CLASS_TEXT);
				edit.setSingleLine(true);
				edit.setText("");
				binding.btnFloatingOk.setText(android.R.string.ok);
				binding.btnFloatingCancel.setText(R.string.cancel);
				binding.btnFloatingCancel.setOnClickListener(v -> {
					hideSoftInput();
					binding.floatingInputBar.setVisibility(View.GONE);
				});
				binding.btnFloatingOk.setOnClickListener(v -> {
					if (binding.floatingInputBar.getVisibility() != View.VISIBLE) return;
					binding.floatingInputBar.setVisibility(View.GONE);
					hideSoftInput();
					String text = edit.getText() != null ? edit.getText().toString() : "";
					Canvas c = (Canvas) current;
					for (int i = 0; i < text.length(); i++) {
						int cp = text.charAt(i);
						c.postKeyPressed(cp);
						c.postKeyReleased(cp);
					}
					c.postKeyPressed(Canvas.KEY_FIRE);
					c.postKeyReleased(Canvas.KEY_FIRE);
					edit.setText("");
				});
				edit.setOnEditorActionListener((v, actionId, event) -> {
					binding.btnFloatingOk.performClick();
					return true;
				});
				edit.setOnCommitListener(() -> binding.btnFloatingOk.performClick());
				edit.setOnBackListener(() -> binding.btnFloatingCancel.performClick());
				edit.requestFocus();
				edit.post(() -> {
					if (inputMethodManager != null) {
						inputMethodManager.showSoftInput(edit, InputMethodManager.SHOW_FORCED);
					}
				});
			} else {
				inputMethodManager.toggleSoftInputFromWindow(binding.displayableContainer.getWindowToken(),
						InputMethodManager.SHOW_FORCED, 0);
			}
		} else if (id == R.id.action_take_screenshot) {
			takeScreenshot();
		} else if (id == R.id.action_limit_fps) {
			showLimitFpsDialog();
		} else if (ContextHolder.getVk() != null) {
			// Handled only when virtual keyboard is enabled
			handleVkOptions(id);
		}
		return true;
	}

	private void handleVkOptions(int id) {
		VirtualKeyboard vk = ContextHolder.getVk();
		if (id == R.id.action_layout_edit_mode) {
			vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_KEYS);
			Toast.makeText(this, R.string.layout_edit_mode, Toast.LENGTH_SHORT).show();
		} else if (id == R.id.action_layout_scale_mode) {
			vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_SCALES);
			Toast.makeText(this, R.string.layout_scale_mode, Toast.LENGTH_SHORT).show();
		} else if (id == R.id.action_layout_edit_finish) {
			vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_EOF);
			Toast.makeText(this, R.string.layout_edit_finished, Toast.LENGTH_SHORT).show();
			showSaveVkAlert(false);
		} else if (id == R.id.action_layout_switch) {
			showSetLayoutDialog();
		} else if (id == R.id.action_hide_buttons) {
			showHideButtonDialog();
		}
	}

	@SuppressLint("CheckResult")
	private void takeScreenshot() {
		microLoader.takeScreenshot((Canvas) current, new SingleObserver<String>() {
			@Override
			public void onSubscribe(@NonNull Disposable d) {
			}

			@Override
			public void onSuccess(@NonNull String s) {
				Toast.makeText(MicroActivity.this, getString(R.string.screenshot_saved)
						+ " " + s, Toast.LENGTH_LONG).show();
				MediaScannerConnection.scanFile(MicroActivity.this, new String[]{s}, null, null);
			}

			@Override
			public void onError(@NonNull Throwable e) {
				e.printStackTrace();
				Toast.makeText(MicroActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void saveLog() {
		try {
			LogUtils.writeLog();
			Toast.makeText(this, R.string.log_saved, Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
		}
	}

	private void showHideButtonDialog() {
		final VirtualKeyboard vk = ContextHolder.getVk();
		boolean[] states = vk.getKeysVisibility();
		boolean[] changed = states.clone();
		new AlertDialog.Builder(this)
				.setTitle(R.string.hide_buttons)
				.setMultiChoiceItems(vk.getKeyNames(), changed, (dialog, which, isChecked) -> {})
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					if (!Arrays.equals(states, changed)) {
						vk.setKeysVisibility(changed);
						showSaveVkAlert(true);
					}
				}).show();
	}

	private void showSaveVkAlert(boolean keepScreenPreferred) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.CONFIRMATION_REQUIRED);
		builder.setMessage(R.string.pref_vk_save_alert);
		builder.setNegativeButton(android.R.string.no, null);
		AlertDialog dialog = builder.create();

		final VirtualKeyboard vk = ContextHolder.getVk();
		if (vk.isPhone()) {
			AppCompatCheckBox cb = new AppCompatCheckBox(this);
			cb.setText(R.string.opt_save_screen_params);
			cb.setChecked(keepScreenPreferred);

			TypedValue out = new TypedValue();
			getTheme().resolveAttribute(androidx.appcompat.R.attr.dialogPreferredPadding, out, true);
			int paddingH = getResources().getDimensionPixelOffset(out.resourceId);
			int paddingT = getResources().getDimensionPixelOffset(androidx.appcompat.R.dimen.abc_dialog_padding_top_material);
			dialog.setView(cb, paddingH, paddingT, paddingH, 0);

			dialog.setButton(dialog.BUTTON_POSITIVE, getText(android.R.string.yes), (d, w) -> {
				if (cb.isChecked()) {
					vk.saveScreenParams();
				}
				vk.onLayoutChanged(VirtualKeyboard.TYPE_CUSTOM);
			});
		} else {
			dialog.setButton(dialog.BUTTON_POSITIVE, getText(android.R.string.yes), (d, w) ->
					ContextHolder.getVk().onLayoutChanged(VirtualKeyboard.TYPE_CUSTOM));
		}
		dialog.show();
	}

	private void showSetLayoutDialog() {
		final VirtualKeyboard vk = ContextHolder.getVk();
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.layout_switch)
				.setSingleChoiceItems(R.array.PREF_VK_TYPE_ENTRIES, vk.getLayout(), null)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					vk.setLayout(((AlertDialog) d).getListView().getCheckedItemPosition());
					if (vk.isPhone()) {
						setOrientation(ORIENTATION_PORTRAIT);
					} else {
						setOrientation(microLoader.getOrientation());
					}
				});
		builder.show();
	}

	private void showLimitFpsDialog() {
		EditText editText = new EditText(this);
		editText.setHint(R.string.unlimited);
		editText.setInputType(InputType.TYPE_CLASS_NUMBER);
		editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
		editText.setMaxLines(1);
		editText.setSingleLine(true);
		float density = getResources().getDisplayMetrics().density;
		LinearLayout linearLayout = new LinearLayout(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		int margin = (int) (density * 20);
		params.setMargins(margin, 0, margin, 0);
		linearLayout.addView(editText, params);
		int paddingVertical = (int) (density * 16);
		int paddingHorizontal = (int) (density * 8);
		editText.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
		new AlertDialog.Builder(this)
				.setTitle(R.string.PREF_LIMIT_FPS)
				.setView(linearLayout)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					Editable text = editText.getText();
					int fps = 0;
					try {
						fps = TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text.toString().trim());
					} catch (NumberFormatException ignored) {
					}
					microLoader.setLimitFps(fps);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.setNeutralButton(R.string.reset, ((d, which) -> microLoader.setLimitFps(-1)))
				.show();
	}

	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (current instanceof Form) {
			((Form) current).contextMenuItemSelected(item);
		}

		return super.onContextItemSelected(item);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		ContextHolder.notifyOnActivityResult(requestCode, resultCode, data);
	}

	public String getAppName() {
		return appName;
	}

	public J2meContext getContext() {
		return context;
	}

	@Override
	public void onDisplayableChanged(Displayable displayable) {
		setCurrent(displayable);
	}

	@Override
	public void onMidletExit(J2meContext context) {
		runOnUiThread(() -> closeTab(context));
	}

	@Override
	public void showError(String message) {
		showErrorDialog(message);
	}

	private void addTabPill(int index) {
		TextView pill = new TextView(this);
		Tab tab = tabs.get(index);
		String title = (tab.accountName != null && !tab.accountName.isEmpty()) ? tab.accountName : tab.name;
		pill.setText(title);
		pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		pill.setSingleLine(true);
		pill.setEllipsize(TextUtils.TruncateAt.END);
		pill.setGravity(Gravity.CENTER_VERTICAL);
		pill.setTag(index);
		pill.setOnClickListener(v -> switchTo((Integer) v.getTag()));
		pill.setOnLongClickListener(v -> {
			int idx = (Integer) v.getTag();
			if (idx >= 0 && idx < tabs.size()) {
				showCloseTabDialog(tabs.get(idx));
			}
			return true;
		});
		float density = getResources().getDisplayMetrics().density;

		File iconFile = new File(tab.appPath, Config.MIDLET_ICON_FILE);
		Bitmap bm = null;
		if (iconFile.exists()) {
			bm = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
		}
		if (bm == null) {
			bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
		}
		if (bm != null) {
			RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(getResources(), bm);
			rbd.setCornerRadius(4 * density);
			int iconSize = (int) (18 * density);
			rbd.setBounds(0, 0, iconSize, iconSize);
			pill.setCompoundDrawablesRelative(rbd, null, null, null);
			pill.setCompoundDrawablePadding((int) (6 * density));
		}

		pill.setPadding((int) (10 * density), (int) (6 * density), (int) (12 * density), (int) (6 * density));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.setMarginEnd((int) (6 * density));
		pill.setLayoutParams(lp);
		binding.tabBar.addView(pill);
		updatePills();
	}

	private void showCloseTabDialog(Tab tab) {
		String title = tab.name;
		if (tab.accountName != null && !tab.accountName.isEmpty()) {
			title = tab.name + " (" + tab.accountName + ")";
		}
		new AlertDialog.Builder(this)
				.setTitle(R.string.close_tab_title)
				.setMessage(getString(R.string.close_tab_confirm, title))
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					closeTab(tab.context);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void updatePills() {
		TypedValue onSurface = new TypedValue();
		TypedValue onPrimaryContainer = new TypedValue();
		getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, onSurface, true);
		getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimaryContainer, onPrimaryContainer, true);
		int count = binding.tabBar.getChildCount();
		for (int i = 0; i < count; i++) {
			TextView pill = (TextView) binding.tabBar.getChildAt(i);
			pill.setTag(i);
			boolean active = i == activeIndex;
			pill.setBackgroundResource(active ? R.drawable.bg_tab_pill_active : R.drawable.bg_tab_pill);
			pill.setTextColor(active ? onPrimaryContainer.data : onSurface.data);
		}
	}

	private void switchTo(int index) {
		if (index < 0 || index >= tabs.size() || index == activeIndex) {
			return;
		}
		activeIndex = index;
		activateActiveTab();
	}

	private void activateActiveTab() {
		Tab tab = tabs.get(activeIndex);
		context = tab.context;
		microLoader = tab.loader;
		appName = tab.name;
		appPath = tab.appPath;
		context.register();
		microLoader.applyConfiguration();
		setupVirtualKeyboard();
		Displayable displayable = context.getDisplay() != null ? context.getDisplay().getCurrent() : null;
		if (displayable != null) {
			setCurrent(displayable);
		}
		if (tab.context.getMidletThread() != null) {
			MidletThread.resumeApp();
		}
		updatePills();
	}

	private void setupVirtualKeyboard() {
		VirtualKeyboard vk = ContextHolder.getVk();
		binding.overlayView.removeLayers(VirtualKeyboard.class);
		if (vk != null) {
			vk.setView(binding.overlayView);
			binding.overlayView.addLayer(vk);
			if (current instanceof Canvas) {
				vk.setTarget((Canvas) current);
				((Canvas) current).updateSize();
			}
			int orientation = microLoader.getOrientation();
			if (vk.isPhone()) {
				orientation = ORIENTATION_PORTRAIT;
			}
			setOrientation(orientation);
			binding.overlayView.setVisibility(true);
			binding.overlayView.postInvalidate();
		}
	}

	private void closeTab(J2meContext context) {
		if (context == null) {
			finish();
			return;
		}
		int index = -1;
		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).context == context) {
				index = i;
				break;
			}
		}
		if (index < 0) {
			finish();
			return;
		}
		Tab closedTab = tabs.remove(index);
		binding.tabBar.removeViewAt(index);
		if (closedTab.context != null) {
			if (closedTab.context.getMidletThread() != null) {
				closedTab.context.register();
				MidletThread.destroyApp();
			}
			closedTab.context.unregister();
		}
		if (tabs.isEmpty()) {
			finish();
			return;
		}
		if (index < activeIndex) {
			activeIndex--;
		} else if (index == activeIndex) {
			activeIndex = Math.min(index, tabs.size() - 1);
		}
		activateActiveTab();
	}

	public void toast(@StringRes int message) {
		runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
	}

	private static class Tab {
		final J2meContext context;
		final MicroLoader loader;
		final String name;
		final String appPath;
		final String accountName;

		Tab(J2meContext context, MicroLoader loader, String name, String appPath, String accountName) {
			this.context = context;
			this.loader = loader;
			this.name = name;
			this.appPath = appPath;
			this.accountName = accountName;
		}
	}

	private Command activeOkCommand;
	private Command activeCancelCommand;

	private void startFloatingInput(TextBox textBox) {
		this.activeTextBox = textBox;
		if (binding.floatingInputBar == null || binding.floatingInputEdit == null) return;

		InvisibleInputView input = binding.floatingInputEdit;

		if (invisibleInputWatcher != null) {
			input.removeTextChangedListener(invisibleInputWatcher);
		}

		int constraints = textBox.getConstraints();
		int inputType = InputType.TYPE_CLASS_TEXT;
		switch (constraints & TextField.CONSTRAINT_MASK) {
			case TextField.NUMERIC:
				inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
				break;
			case TextField.DECIMAL:
				inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
				break;
			case TextField.PHONENUMBER:
				inputType = InputType.TYPE_CLASS_PHONE;
				break;
			case TextField.EMAILADDR:
				inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
				break;
			case TextField.URL:
				inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
				break;
			default:
				inputType = InputType.TYPE_CLASS_TEXT;
				break;
		}

		if ((constraints & TextField.PASSWORD) != 0) {
			if ((inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER) {
				inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD;
			} else {
				inputType |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
			}
		}

		input.setInputType(inputType);
		input.setSingleLine(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

		int max = textBox.getMaxSize();
		if (max > 0) {
			input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
		} else {
			input.setFilters(new InputFilter[0]);
		}

		String initial = textBox.getString();
		input.setText(initial != null ? initial : "");
		if (input.getText() != null) {
			input.setSelection(input.getText().length());
		}

		invisibleInputWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				if (activeTextBox != null && s != null) {
					try {
						String text = s.toString();
						int m = activeTextBox.getMaxSize();
						if (m > 0 && text.length() > m) {
							text = text.substring(0, m);
						}
						activeTextBox.setString(text);
					} catch (Exception ignored) {}
				}
			}
		};
		input.addTextChangedListener(invisibleInputWatcher);

		this.activeOkCommand = findOkCommand(textBox);
		this.activeCancelCommand = findCancelCommand(textBox);

		if (activeOkCommand != null) {
			binding.btnFloatingOk.setText(sanitizeButtonLabel(activeOkCommand.getAndroidLabel(), android.R.string.ok));
		} else {
			binding.btnFloatingOk.setText(android.R.string.ok);
		}

		if (activeCancelCommand != null) {
			binding.btnFloatingCancel.setText(sanitizeButtonLabel(activeCancelCommand.getAndroidLabel(), R.string.cancel));
		} else {
			binding.btnFloatingCancel.setText(R.string.cancel);
		}

		binding.btnFloatingOk.setOnClickListener(v -> commitFloatingInput());
		binding.btnFloatingCancel.setOnClickListener(v -> cancelFloatingInput());

		input.setOnEditorActionListener((v, actionId, event) -> {
			commitFloatingInput();
			return true;
		});

		input.setOnCommitListener(this::commitFloatingInput);
		input.setOnBackListener(this::cancelFloatingInput);

		binding.floatingInputBar.setVisibility(View.VISIBLE);

		input.requestFocus();
		input.post(() -> {
			if (inputMethodManager != null) {
				inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
			}
		});
	}

	private String sanitizeButtonLabel(String label, int defaultResId) {
		if (label == null || label.trim().isEmpty()) {
			return getString(defaultResId);
		}
		label = label.trim();
		if (label.length() > 10) {
			return getString(defaultResId);
		}
		int weirdChars = 0;
		for (int i = 0; i < label.length(); i++) {
			char ch = label.charAt(i);
			if (ch == 'I' || ch == 'l' || ch == '|' || ch == '1') {
				weirdChars++;
			}
		}
		if (weirdChars > label.length() / 2 && label.length() > 3) {
			return getString(defaultResId);
		}
		return label;
	}

	private Command findOkCommand(TextBox tb) {
		Command[] cmds = tb.getCommands();
		if (cmds == null || cmds.length == 0) return null;
		if (cmds.length == 1) return cmds[0];

		// 1. Tìm Command có type == Command.OK
		for (Command cmd : cmds) {
			if (cmd.getCommandType() == Command.OK) {
				return cmd;
			}
		}

		// 2. Lọc bỏ các command là Cancel/Back/Close
		List<Command> candidates = new ArrayList<>();
		for (Command cmd : cmds) {
			int type = cmd.getCommandType();
			if (type == Command.CANCEL || type == Command.BACK || type == Command.STOP || type == Command.EXIT) {
				continue;
			}
			String label = cmd.getLabel() != null ? cmd.getLabel().trim().toLowerCase() : "";
			if (label.contains("hủy") || label.contains("huỷ") || label.contains("đóng")
					|| label.contains("trở về") || label.contains("quay lại") || label.contains("bỏ qua")
					|| label.contains("cancel") || label.contains("back") || label.contains("exit") || label.contains("close")) {
				continue;
			}
			candidates.add(cmd);
		}

		if (!candidates.isEmpty()) {
			return candidates.get(0);
		}

		// 3. Fallback: command đầu tiên
		return cmds[0];
	}

	private Command findCancelCommand(TextBox tb) {
		Command[] cmds = tb.getCommands();
		if (cmds == null || cmds.length <= 1) return null;

		for (Command cmd : cmds) {
			int type = cmd.getCommandType();
			if (type == Command.CANCEL || type == Command.BACK || type == Command.STOP || type == Command.EXIT) {
				return cmd;
			}
		}

		for (Command cmd : cmds) {
			String label = cmd.getLabel() != null ? cmd.getLabel().trim().toLowerCase() : "";
			if (label.contains("hủy") || label.contains("huỷ") || label.contains("đóng")
					|| label.contains("trở về") || label.contains("quay lại") || label.contains("bỏ qua")
					|| label.contains("cancel") || label.contains("back") || label.contains("exit") || label.contains("close")) {
				return cmd;
			}
		}

		if (cmds.length >= 2) {
			return cmds[1];
		}
		return null;
	}

	private void commitFloatingInput() {
		if (activeTextBox == null) return;
		TextBox tb = activeTextBox;
		activeTextBox = null;

		String text = "";
		if (binding.floatingInputEdit != null) {
			Editable editable = binding.floatingInputEdit.getText();
			if (editable != null) {
				text = editable.toString();
			}
		}

		hideSoftInput();
		if (binding.floatingInputBar != null) {
			binding.floatingInputBar.setVisibility(View.GONE);
		}

		if (binding.floatingInputEdit != null && invisibleInputWatcher != null) {
			binding.floatingInputEdit.removeTextChangedListener(invisibleInputWatcher);
			invisibleInputWatcher = null;
		}

		try {
			int max = tb.getMaxSize();
			if (max > 0 && text.length() > max) {
				text = text.substring(0, max);
			}
			tb.setString(text);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Command okCommand = activeOkCommand != null ? activeOkCommand : findOkCommand(tb);
		activeOkCommand = null;
		activeCancelCommand = null;

		if (okCommand != null) {
			tb.fireCommandAction(okCommand);
		}
	}

	private void cancelFloatingInput() {
		if (activeTextBox == null) return;
		TextBox tb = activeTextBox;
		activeTextBox = null;

		hideSoftInput();
		if (binding.floatingInputBar != null) {
			binding.floatingInputBar.setVisibility(View.GONE);
		}

		if (binding.floatingInputEdit != null && invisibleInputWatcher != null) {
			binding.floatingInputEdit.removeTextChangedListener(invisibleInputWatcher);
			invisibleInputWatcher = null;
		}

		Command cancelCommand = activeCancelCommand != null ? activeCancelCommand : findCancelCommand(tb);
		activeOkCommand = null;
		activeCancelCommand = null;

		if (cancelCommand != null) {
			tb.fireCommandAction(cancelCommand);
		} else if (lastActiveCanvas != null && context != null && context.getDisplay() != null) {
			context.getDisplay().setCurrent(lastActiveCanvas);
		}
	}

	private void finishFloatingInput() {
		activeTextBox = null;
		activeOkCommand = null;
		activeCancelCommand = null;
		hideSoftInput();
		if (binding.floatingInputBar != null) {
			binding.floatingInputBar.setVisibility(View.GONE);
		}
		if (binding.floatingInputEdit != null && invisibleInputWatcher != null) {
			binding.floatingInputEdit.removeTextChangedListener(invisibleInputWatcher);
			invisibleInputWatcher = null;
		}
	}

	private class SetCurrentEvent extends SimpleEvent {
		private final Displayable current;
		private final Displayable next;

		private SetCurrentEvent(Displayable current, Displayable next) {
			this.current = current;
			this.next = next;
		}

		@Override
		public void process() {
			if (next instanceof Alert) {
				return;
			}

			if (next instanceof TextBox) {
				// Floating Input Mode: Không gỡ bỏ CanvasView, chỉ bật thanh nhập nổi + IME
				if (current instanceof Canvas) {
					lastActiveCanvas = (Canvas) current;
				}
				startFloatingInput((TextBox) next);
				return;
			}

			// Nếu từ TextBox quay về Canvas
			if (current instanceof TextBox && next instanceof Canvas) {
				finishFloatingInput();
				Canvas nextCanvas = (Canvas) next;
				lastActiveCanvas = nextCanvas;
				View canvasView = nextCanvas.getDisplayableView();
				if (binding.displayableContainer.indexOfChild(canvasView) == -1) {
					binding.displayableContainer.removeAllViews();
					binding.displayableContainer.addView(canvasView);
				}
				hideSystemUI();
				VirtualKeyboard vk = ContextHolder.getVk();
				if (vk != null) {
					vk.setTarget(nextCanvas);
					nextCanvas.updateSize();
					binding.overlayView.setVisibility(true);
					binding.overlayView.postInvalidate();
				}
				return;
			}

			if (current != null) {
				current.clearDisplayableView();
			}
			binding.displayableContainer.removeAllViews();
			if (next instanceof Canvas) {
				hideSystemUI();
				lastActiveCanvas = (Canvas) next;
				VirtualKeyboard vk = ContextHolder.getVk();
				if (vk != null) {
					vk.setTarget((Canvas) next);
					((Canvas) next).updateSize();
					binding.overlayView.setVisibility(true);
					binding.overlayView.postInvalidate();
				}
			} else {
				showSystemUI();
			}
			if (next != null) {
				binding.displayableContainer.addView(next.getDisplayableView());
			}
		}
	}

	@Override
	protected void onDestroy() {
		// Stop the MIDlet thread explicitly (fixes stuck audio)
		MidletThread.destroyApp();
		stopService(new Intent(this, ForegroundService.class));
		binding = null;
		super.onDestroy();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1) {
			synchronized (LocationProviderImpl.permissionLock) {
				LocationProviderImpl.permissionLock.notify();
			}
			LocationProviderImpl.permissionResult = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
		}
	}
}
