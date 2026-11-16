/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2018 Nikita Shakarun
 * Copyright 2021 Yury Kharchenko
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
package javax.microedition.lcdui.keyboard;

import static javax.microedition.lcdui.keyboard.KeyMapper.SE_KEY_SPECIAL_GAMING_A;
import static javax.microedition.lcdui.keyboard.KeyMapper.SE_KEY_SPECIAL_GAMING_B;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.graphics.CanvasWrapper;
import javax.microedition.lcdui.overlay.Overlay;
import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

import com.j2me.lixin.config.Config;
import com.j2me.lixin.config.ProfileModel;
import com.j2me.lixin.config.ProfilesManager;

public class VirtualKeyboard implements Overlay, Runnable {
	private static final String TAG = VirtualKeyboard.class.getSimpleName();

	private static final String ARROW_LEFT = "\u2190";
	private static final String ARROW_UP = "\u2191";
	private static final String ARROW_RIGHT = "\u2192";
	private static final String ARROW_DOWN = "\u2193";
	private static final String ARROW_UP_LEFT = "\u2196";
	private static final String ARROW_UP_RIGHT = "\u2197";
	private static final String ARROW_DOWN_LEFT = "\u2199";
	private static final String ARROW_DOWN_RIGHT = "\u2198";

	private static final int LAYOUT_SIGNATURE = 0x564B4C00;
	private static final int LAYOUT_VERSION = 3;
	public static final int LAYOUT_EOF = -1;
	public static final int LAYOUT_KEYS = 0;
	public static final int LAYOUT_SCALES = 1;
	@SuppressWarnings("unused")
	public static final int LAYOUT_COLORS = 2;
	public static final int LAYOUT_TYPE = 3;
	public static final int LAYOUT_KEY_SCALES = 4;

	private static final int OVAL_SHAPE = 0;
	private static final int RECT_SHAPE = 1;
	public static final int ROUND_RECT_SHAPE = 2;

	public static final int TYPE_CUSTOM = 0;
	private static final int TYPE_PHONE = 1;
	private static final int TYPE_PHONE_ARROWS = 2;
	private static final int TYPE_NUM_ARR = 3;
	private static final int TYPE_ARR_NUM = 4;
	private static final int TYPE_NUMBERS = 5;
	private static final int TYPE_ARROWS = 6;
	public static final int TYPE_ANLAND = 7;

	private static final float PHONE_KEY_ROWS = 5;
	private static final float PHONE_KEY_SCALE_X = 2.0f;
	private static final float PHONE_KEY_SCALE_Y = 0.75f;
	private static final long[] REPEAT_INTERVALS = {200, 400, 128, 128, 128, 128, 128};

	private static final int SCREEN = -1;
	private static final int KEY_NUM1 = 0;
	private static final int KEY_NUM2 = 1;
	private static final int KEY_NUM3 = 2;
	private static final int KEY_NUM4 = 3;
	private static final int KEY_NUM5 = 4;
	private static final int KEY_NUM6 = 5;
	private static final int KEY_NUM7 = 6;
	private static final int KEY_NUM8 = 7;
	private static final int KEY_NUM9 = 8;
	private static final int KEY_NUM0 = 9;
	private static final int KEY_STAR = 10;
	private static final int KEY_POUND = 11;
	private static final int KEY_SOFT_LEFT = 12;
	private static final int KEY_SOFT_RIGHT = 13;
	private static final int KEY_D = 14;
	private static final int KEY_C = 15;
	private static final int KEY_UP_LEFT = 16;
	private static final int KEY_UP = 17;
	private static final int KEY_UP_RIGHT = 18;
	private static final int KEY_LEFT = 19;
	private static final int KEY_RIGHT = 20;
	private static final int KEY_DOWN_LEFT = 21;
	private static final int KEY_DOWN = 22;
	private static final int KEY_DOWN_RIGHT = 23;
	private static final int KEY_FIRE = 24;
	private static final int KEY_A = 25;
	private static final int KEY_B = 26;
	private static final int KEY_MENU = 27;
	private static final int KEYBOARD_SIZE = 28;

	private static final float SCALE_SNAP_RADIUS = 0.05f;

	private static final int FEEDBACK_DURATION = 50;

	private final float[] keyScales = {
			1, 1,
			1, 1,
			1, 1,
			1, 1,
			1, 1,
			1, 1,
	};
	private final int[][] keyScaleGroups = {{
			KEY_UP_LEFT,
			KEY_UP,
			KEY_UP_RIGHT,
			KEY_LEFT,
			KEY_RIGHT,
			KEY_DOWN_LEFT,
			KEY_DOWN,
			KEY_DOWN_RIGHT
	}, {
			KEY_SOFT_LEFT,
			KEY_SOFT_RIGHT
	}, {
			KEY_A,
			KEY_B,
			KEY_C,
			KEY_D,
	}, {
			KEY_NUM1,
			KEY_NUM2,
			KEY_NUM3,
			KEY_NUM4,
			KEY_NUM5,
			KEY_NUM6,
			KEY_NUM7,
			KEY_NUM8,
			KEY_NUM9,
			KEY_NUM0,
			KEY_STAR,
			KEY_POUND
	}, {
			KEY_FIRE
	}, {
			KEY_MENU
	}};
	private final VirtualKey[] keypad = new VirtualKey[KEYBOARD_SIZE];
	// the average user usually has no more than 10 fingers...
	private final VirtualKey[] associatedKeys = new VirtualKey[10];
	private final int[] snapStack = new int[KEYBOARD_SIZE];

	private final Handler handler;
	private final File saveFile;
	private final ProfileModel settings;
	private final RectF virtualScreen = new RectF();

	private Canvas target;
	private View overlayView;
	private boolean obscuresVirtualScreen;
	private boolean visible = true;
	private int layoutEditMode = LAYOUT_EOF;
	private int editedIndex;
	private float offsetX;
	private float offsetY;
	private float prevScaleX;
	private float prevScaleY;
	private RectF screen;
	private final RectF toolbarRect = new RectF();
	private final RectF btnMinusRect = new RectF();
	private final RectF btnPlusRect = new RectF();
	private final RectF btnVisRect = new RectF();
	private final RectF btnResetRect = new RectF();
	private final RectF btnDoneRect = new RectF();
	private float keySize =
			Math.min(ContextHolder.getDisplayWidth(), ContextHolder.getDisplayHeight()) / 6.0f;
	private float snapRadius;
	private int layoutVariant;

	public VirtualKeyboard(ProfileModel settings) {
		this.settings = settings;
		this.saveFile = new File(settings.dir + Config.MIDLET_KEY_LAYOUT_FILE);

		for (int i = KEY_NUM1; i < 9; i++) {
			keypad[i] = new VirtualKey(Canvas.KEY_NUM1 + i, Integer.toString(1 + i));
		}

		keypad[KEY_NUM0] = new VirtualKey(Canvas.KEY_NUM0, "0");
		keypad[KEY_STAR] = new VirtualKey(Canvas.KEY_STAR, "*");
		keypad[KEY_POUND] = new VirtualKey(Canvas.KEY_POUND, "#");

		keypad[KEY_SOFT_LEFT] = new VirtualKey(Canvas.KEY_SOFT_LEFT, "L");
		keypad[KEY_SOFT_RIGHT] = new VirtualKey(Canvas.KEY_SOFT_RIGHT, "R");

		keypad[KEY_A] = new VirtualKey(SE_KEY_SPECIAL_GAMING_A, "A");
		keypad[KEY_B] = new VirtualKey(SE_KEY_SPECIAL_GAMING_B, "B");
		keypad[KEY_C] = new VirtualKey(Canvas.KEY_END, "C");
		keypad[KEY_D] = new VirtualKey(Canvas.KEY_SEND, "D");

		keypad[KEY_UP_LEFT] = new DualKey(Canvas.KEY_UP, Canvas.KEY_LEFT, ARROW_UP_LEFT);
		keypad[KEY_UP] = new VirtualKey(Canvas.KEY_UP, ARROW_UP);
		keypad[KEY_UP_RIGHT] = new DualKey(Canvas.KEY_UP, Canvas.KEY_RIGHT, ARROW_UP_RIGHT);

		keypad[KEY_LEFT] = new VirtualKey(Canvas.KEY_LEFT, ARROW_LEFT);
		keypad[KEY_RIGHT] = new VirtualKey(Canvas.KEY_RIGHT, ARROW_RIGHT);

		keypad[KEY_DOWN_LEFT] = new DualKey(Canvas.KEY_DOWN, Canvas.KEY_LEFT, ARROW_DOWN_LEFT);
		keypad[KEY_DOWN] = new VirtualKey(Canvas.KEY_DOWN, ARROW_DOWN);
		keypad[KEY_DOWN_RIGHT] = new DualKey(Canvas.KEY_DOWN, Canvas.KEY_RIGHT, ARROW_DOWN_RIGHT);

		keypad[KEY_FIRE] = new VirtualKey(Canvas.KEY_FIRE, "F");
		keypad[KEY_MENU] = new MenuKey();

		layoutVariant = readLayoutType();

		if (layoutVariant == -1) {
			layoutVariant = settings.vkType;
			if (layoutVariant == TYPE_CUSTOM) {
				layoutVariant = TYPE_NUM_ARR;
			}
		}
		resetLayout(layoutVariant);
		if (layoutVariant == TYPE_CUSTOM) {
			try {
				readLayout();
			} catch (IOException e) {
				e.printStackTrace();
				resetLayout(TYPE_NUM_ARR);
				layoutVariant = TYPE_NUM_ARR;
				saveLayout();
			}
		}
		HandlerThread thread = new HandlerThread("MidletVirtualKeyboard");
		thread.start();
		handler = new Handler(thread.getLooper());
	}

	public void onLayoutChanged(int variant) {
		if (variant == TYPE_CUSTOM && isPhone()) {
			float min = screen.width();
			float max = screen.height();
			if (min > max) {
				float tmp = max;
				max = min;
				min = tmp;
			}

			float oldSize = min / 6.0f;
			float newSize = Math.min(oldSize, max / 12.0f);
			float s = oldSize / newSize;
			for (int i = 0; i < keyScales.length; i++) {
				keyScales[i] *= s;
			}
		}
		layoutVariant = variant;
		saveLayout();
		if (target != null && target.isShown()) {
			target.updateSize();
		}
	}

	private void resetLayout(int variant) {
		for (VirtualKey key : keypad) {
			key.scale = 1.0f;
		}
		switch (variant) {
			case TYPE_PHONE:
				for (int j = 0, len = keyScales.length; j < len;) {
					keyScales[j++] = PHONE_KEY_SCALE_X;
					keyScales[j++] = PHONE_KEY_SCALE_Y;
				}

				setSnap(KEY_NUM0, SCREEN, RectSnap.INT_SOUTH, true);
				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST, true);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM8, KEY_NUM7, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM9, KEY_NUM8, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM4, KEY_NUM7, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM5, KEY_NUM4, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM6, KEY_NUM5, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM1, KEY_NUM4, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM2, KEY_NUM1, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST, true);
				setSnap(KEY_SOFT_LEFT, KEY_NUM1, RectSnap.EXT_NORTH, true);
				setSnap(KEY_FIRE, KEY_NUM2, RectSnap.EXT_NORTH, true);
				setSnap(KEY_SOFT_RIGHT, KEY_NUM3, RectSnap.EXT_NORTH, true);

				setSnap(KEY_A, SCREEN, RectSnap.INT_NORTHWEST, false);
				setSnap(KEY_B, SCREEN, RectSnap.INT_NORTHEAST, false);
				setSnap(KEY_C, KEY_A, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_D, KEY_B, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_UP_LEFT, KEY_C, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_UP, KEY_C, RectSnap.EXT_SOUTHEAST, false);
				setSnap(KEY_UP_RIGHT, KEY_D, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_LEFT, KEY_UP_LEFT, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_MENU, KEY_UP, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_RIGHT, KEY_UP_RIGHT, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_DOWN_LEFT, KEY_LEFT, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_DOWN, KEY_MENU, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_DOWN_RIGHT, KEY_RIGHT, RectSnap.EXT_SOUTH, false);
				break;
			case TYPE_PHONE_ARROWS:
				for (int j = 0, len = keyScales.length; j < len;) {
					keyScales[j++] = PHONE_KEY_SCALE_X;
					keyScales[j++] = PHONE_KEY_SCALE_Y;
				}

				setSnap(KEY_NUM0, SCREEN, RectSnap.INT_SOUTH, true);
				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST, true);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH, true);
				setSnap(KEY_DOWN, KEY_NUM7, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM9, KEY_DOWN, RectSnap.EXT_EAST, true);
				setSnap(KEY_LEFT, KEY_NUM7, RectSnap.EXT_NORTH, true);
				setSnap(KEY_FIRE, KEY_LEFT, RectSnap.EXT_EAST, true);
				setSnap(KEY_RIGHT, KEY_FIRE, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM1, KEY_LEFT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_UP, KEY_NUM1, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM3, KEY_UP, RectSnap.EXT_EAST, true);
				setSnap(KEY_SOFT_LEFT, KEY_NUM1, RectSnap.EXT_NORTH, true);
				setSnap(KEY_MENU, KEY_UP, RectSnap.EXT_NORTH, true);
				setSnap(KEY_SOFT_RIGHT, KEY_NUM3, RectSnap.EXT_NORTH, true);

				setSnap(KEY_A, SCREEN, RectSnap.INT_NORTHWEST, false);
				setSnap(KEY_B, SCREEN, RectSnap.INT_NORTHEAST, false);
				setSnap(KEY_C, KEY_A, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_D, KEY_B, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_UP_LEFT, KEY_C, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM2, KEY_C, RectSnap.EXT_SOUTHEAST, false);
				setSnap(KEY_UP_RIGHT, KEY_D, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM4, KEY_UP_LEFT, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM5, KEY_NUM2, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM6, KEY_UP_RIGHT, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_DOWN_LEFT, KEY_NUM4, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_DOWN_RIGHT, KEY_NUM6, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM8, KEY_NUM5, RectSnap.EXT_SOUTH, false);
				break;
			case TYPE_NUM_ARR:
			default:
				Arrays.fill(keyScales, 1.0f);

				setSnap(KEY_DOWN_RIGHT, SCREEN, RectSnap.INT_SOUTHEAST, true);
				setSnap(KEY_DOWN, KEY_DOWN_RIGHT, RectSnap.EXT_WEST, true);
				setSnap(KEY_DOWN_LEFT, KEY_DOWN, RectSnap.EXT_WEST, true);
				setSnap(KEY_LEFT, KEY_DOWN_LEFT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_RIGHT, KEY_DOWN_RIGHT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_UP_RIGHT, KEY_RIGHT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_UP, KEY_UP_RIGHT, RectSnap.EXT_WEST, true);
				setSnap(KEY_UP_LEFT, KEY_UP, RectSnap.EXT_WEST, true);
				setSnap(KEY_FIRE, KEY_DOWN_RIGHT, RectSnap.EXT_NORTHWEST, true);
				setSnap(KEY_SOFT_LEFT, KEY_UP_LEFT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_SOFT_RIGHT, KEY_UP_RIGHT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_STAR, SCREEN, RectSnap.INT_SOUTHWEST, true);
				setSnap(KEY_NUM0, KEY_STAR, RectSnap.EXT_EAST, true);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM8, KEY_NUM7, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM9, KEY_NUM8, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM4, KEY_NUM7, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM5, KEY_NUM4, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM6, KEY_NUM5, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM1, KEY_NUM4, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM2, KEY_NUM1, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST, true);

				setSnap(KEY_D, KEY_NUM1, RectSnap.EXT_NORTH, false);
				setSnap(KEY_C, KEY_NUM3, RectSnap.EXT_NORTH, false);
				setSnap(KEY_A, SCREEN, RectSnap.INT_NORTHWEST, false);
				setSnap(KEY_B, SCREEN, RectSnap.INT_NORTHEAST, false);
				setSnap(KEY_MENU, KEY_UP, RectSnap.EXT_NORTH, false);
				break;
			case TYPE_ARR_NUM:
				Arrays.fill(keyScales, 1);

				setSnap(KEY_DOWN_LEFT, SCREEN, RectSnap.INT_SOUTHWEST, true);
				setSnap(KEY_DOWN, KEY_DOWN_LEFT, RectSnap.EXT_EAST, true);
				setSnap(KEY_DOWN_RIGHT, KEY_DOWN, RectSnap.EXT_EAST, true);
				setSnap(KEY_LEFT, KEY_DOWN_LEFT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_RIGHT, KEY_DOWN_RIGHT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_UP_RIGHT, KEY_RIGHT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_UP, KEY_UP_RIGHT, RectSnap.EXT_WEST, true);
				setSnap(KEY_UP_LEFT, KEY_UP, RectSnap.EXT_WEST, true);
				setSnap(KEY_FIRE, KEY_DOWN_RIGHT, RectSnap.EXT_NORTHWEST, true);
				setSnap(KEY_SOFT_LEFT, KEY_UP_LEFT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_SOFT_RIGHT, KEY_UP_RIGHT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_POUND, SCREEN, RectSnap.INT_SOUTHEAST, true);
				setSnap(KEY_NUM0, KEY_POUND, RectSnap.EXT_WEST, true);
				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST, true);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM8, KEY_NUM7, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM9, KEY_NUM8, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM4, KEY_NUM7, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM5, KEY_NUM4, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM6, KEY_NUM5, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM1, KEY_NUM4, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM2, KEY_NUM1, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST, true);

				setSnap(KEY_D, KEY_NUM1, RectSnap.EXT_NORTH, false);
				setSnap(KEY_C, KEY_NUM3, RectSnap.EXT_NORTH, false);
				setSnap(KEY_A, SCREEN, RectSnap.INT_NORTHWEST, false);
				setSnap(KEY_B, SCREEN, RectSnap.INT_NORTHEAST, false);
				setSnap(KEY_MENU, KEY_UP, RectSnap.EXT_NORTH, false);
				break;
			case TYPE_NUMBERS:
				Arrays.fill(keyScales, 1);

				setSnap(KEY_NUM0, SCREEN, RectSnap.INT_SOUTH, true);
				setSnap(KEY_STAR, KEY_NUM0, RectSnap.EXT_WEST, true);
				setSnap(KEY_POUND, KEY_NUM0, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM7, KEY_STAR, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM8, KEY_NUM7, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM9, KEY_NUM8, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM4, KEY_NUM7, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM5, KEY_NUM4, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM6, KEY_NUM5, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM1, KEY_NUM4, RectSnap.EXT_NORTH, true);
				setSnap(KEY_NUM2, KEY_NUM1, RectSnap.EXT_EAST, true);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST, true);
				setSnap(KEY_SOFT_LEFT, KEY_NUM1, RectSnap.EXT_WEST, true);
				setSnap(KEY_SOFT_RIGHT, KEY_NUM3, RectSnap.EXT_EAST, true);

				setSnap(KEY_UP, SCREEN, RectSnap.INT_NORTH, false);
				setSnap(KEY_UP_LEFT, KEY_UP, RectSnap.EXT_WEST, false);
				setSnap(KEY_UP_RIGHT, KEY_UP, RectSnap.EXT_EAST, false);
				setSnap(KEY_FIRE, KEY_UP, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_LEFT, KEY_UP_LEFT, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_RIGHT, KEY_UP_RIGHT, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_DOWN_LEFT, KEY_LEFT, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_DOWN_RIGHT, KEY_RIGHT, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_DOWN, KEY_DOWN_LEFT, RectSnap.EXT_EAST, false);
				setSnap(KEY_A, KEY_NUM4, RectSnap.EXT_WEST, false);
				setSnap(KEY_B, KEY_NUM6, RectSnap.EXT_EAST, false);
				setSnap(KEY_C, KEY_NUM7, RectSnap.EXT_WEST, false);
				setSnap(KEY_D, KEY_NUM9, RectSnap.EXT_EAST, false);
				setSnap(KEY_MENU, SCREEN, RectSnap.INT_NORTHEAST, false);
				break;
			case TYPE_ARROWS:
				Arrays.fill(keyScales, 1);

				setSnap(KEY_DOWN, SCREEN, RectSnap.INT_SOUTH, true);
				setSnap(KEY_DOWN_RIGHT, KEY_DOWN, RectSnap.EXT_EAST, true);
				setSnap(KEY_DOWN_LEFT, KEY_DOWN, RectSnap.EXT_WEST, true);
				setSnap(KEY_LEFT, KEY_DOWN_LEFT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_RIGHT, KEY_DOWN_RIGHT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_UP_RIGHT, KEY_RIGHT, RectSnap.EXT_NORTH, true);
				setSnap(KEY_UP, KEY_UP_RIGHT, RectSnap.EXT_WEST, true);
				setSnap(KEY_UP_LEFT, KEY_UP, RectSnap.EXT_WEST, true);
				setSnap(KEY_FIRE, KEY_DOWN_RIGHT, RectSnap.EXT_NORTHWEST, true);
				setSnap(KEY_SOFT_LEFT, KEY_UP_LEFT, RectSnap.EXT_WEST, true);
				setSnap(KEY_SOFT_RIGHT, KEY_UP_RIGHT, RectSnap.EXT_EAST, true);

				setSnap(KEY_NUM1, KEY_NUM2, RectSnap.EXT_WEST, false);
				setSnap(KEY_NUM2, SCREEN, RectSnap.INT_NORTH, false);
				setSnap(KEY_NUM3, KEY_NUM2, RectSnap.EXT_EAST, false);
				setSnap(KEY_NUM4, KEY_NUM1, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM5, KEY_NUM2, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM6, KEY_NUM3, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM7, KEY_NUM4, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM8, KEY_NUM5, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM9, KEY_NUM6, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_STAR, KEY_NUM7, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_NUM0, KEY_NUM8, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_POUND, KEY_NUM9, RectSnap.EXT_SOUTH, false);
				setSnap(KEY_A, KEY_LEFT, RectSnap.EXT_WEST, false);
				setSnap(KEY_B, KEY_RIGHT, RectSnap.EXT_EAST, false);
				setSnap(KEY_C, KEY_DOWN_LEFT, RectSnap.EXT_WEST, false);
				setSnap(KEY_D, KEY_DOWN_RIGHT, RectSnap.EXT_EAST, false);
				setSnap(KEY_MENU, SCREEN, RectSnap.INT_NORTHEAST, false);
				break;
			case TYPE_ANLAND:
				Arrays.fill(keyScales, 1);
				if (screen != null) {
					setAnlandRadialLayout();
				}
				break;
		}
	}

	private void placeKeyAtCenter(int key, float cx, float cy, boolean visible) {
		VirtualKey vKey = keypad[key];
		vKey.snapOrigin = SCREEN;
		vKey.snapMode = RectSnap.INT_NORTHWEST;
		float left = cx - vKey.rect.width() / 2f;
		float top = cy - vKey.rect.height() / 2f;
		vKey.snapOffset.set(left, top);
		vKey.rect.offsetTo(left, top);
		vKey.snapValid = true;
		vKey.visible = visible;
	}

	private void setAnlandRadialLayout() {
		if (screen == null) {
			return;
		}
		float w = screen.width();
		float h = screen.height();
		float baseSize = getKeySize(w, h);

		keypad[KEY_FIRE].scale = 1.6f;
		keypad[KEY_NUM7].scale = 1.05f;
		keypad[KEY_NUM1].scale = 1.05f;
		keypad[KEY_NUM3].scale = 1.05f;
		keypad[KEY_NUM9].scale = 1.05f;
		keypad[KEY_STAR].scale = 0.9f;
		keypad[KEY_NUM0].scale = 0.9f;
		keypad[KEY_UP].scale = 1.15f;
		keypad[KEY_DOWN].scale = 1.15f;
		keypad[KEY_LEFT].scale = 1.15f;
		keypad[KEY_RIGHT].scale = 1.15f;
		keypad[KEY_SOFT_LEFT].scale = 0.9f;
		keypad[KEY_SOFT_RIGHT].scale = 0.9f;
		keypad[KEY_POUND].scale = 0.85f;

		for (int group = 0; group < keyScaleGroups.length; group++) {
			resizeKeyGroup(group);
		}

		// Right Cluster - Radial Arc around Fire:
		float fireCx = w - baseSize * 2.3f;
		float fireCy = h - baseSize * 2.0f;
		float R = baseSize * 1.55f;

		placeKeyAtCenter(KEY_FIRE, fireCx, fireCy, true);

		// 7 (Dau than): Top (90 deg)
		placeKeyAtCenter(KEY_NUM7, fireCx, fireCy - R, true);

		// 1 (Chieu 1): Upper-Left (150 deg)
		float cos150 = -0.866025f;
		float sin150 = 0.5f;
		placeKeyAtCenter(KEY_NUM1, fireCx + R * cos150, fireCy - R * sin150, true);

		// 3 (Chieu 2): Upper-Right (30 deg)
		float cos30 = 0.866025f;
		float sin30 = 0.5f;
		placeKeyAtCenter(KEY_NUM3, fireCx + R * cos30, fireCy - R * sin30, true);

		// 9 (Chieu 3): Lower-Left (235 deg)
		float cos235 = -0.573576f;
		float sin235 = -0.819152f;
		placeKeyAtCenter(KEY_NUM9, fireCx + R * cos235, fireCy - R * sin235, true);

		// * (Target): Above 3 (45 deg)
		float R2 = R * 1.55f;
		float cos45 = 0.7071f;
		float sin45 = 0.7071f;
		placeKeyAtCenter(KEY_STAR, fireCx + R2 * cos45, fireCy - R2 * sin45, true);

		// 0 (Near / Chat): Above 1 (135 deg)
		float cos135 = -0.7071f;
		float sin135 = 0.7071f;
		placeKeyAtCenter(KEY_NUM0, fireCx + R2 * cos135, fireCy - R2 * sin135, true);

		// Left Cluster - D-Pad Cross:
		float dpadCx = baseSize * 2.1f;
		float dpadCy = h - baseSize * 2.0f;
		float dpadR = baseSize * 1.25f;

		placeKeyAtCenter(KEY_UP, dpadCx, dpadCy - dpadR, true);
		placeKeyAtCenter(KEY_DOWN, dpadCx, dpadCy + dpadR, true);
		placeKeyAtCenter(KEY_LEFT, dpadCx - dpadR, dpadCy, true);
		placeKeyAtCenter(KEY_RIGHT, dpadCx + dpadR, dpadCy, true);

		// Corner Keys:
		placeKeyAtCenter(KEY_SOFT_LEFT, baseSize * 0.7f, h - baseSize * 0.6f, true);
		placeKeyAtCenter(KEY_SOFT_RIGHT, w - baseSize * 0.7f, h - baseSize * 0.6f, true);
		placeKeyAtCenter(KEY_POUND, w - baseSize * 0.7f, baseSize * 0.7f, true);

		// Hide all other unused keys:
		int[] unused = {
			KEY_NUM2, KEY_NUM4, KEY_NUM5, KEY_NUM6, KEY_NUM8,
			KEY_UP_LEFT, KEY_UP_RIGHT, KEY_DOWN_LEFT, KEY_DOWN_RIGHT,
			KEY_A, KEY_B, KEY_C, KEY_D, KEY_MENU
		};
		for (int key : unused) {
			keypad[key].visible = false;
			keypad[key].snapOrigin = SCREEN;
			keypad[key].snapMode = RectSnap.INT_NORTH;
			keypad[key].snapOffset.set(0, 0);
		}

		snapKeys();
	}

	public int getLayout() {
		return layoutVariant;
	}

	public float getPhoneKeyboardHeight(float w, float h) {
		return PHONE_KEY_ROWS * getKeySize(w, h) * PHONE_KEY_SCALE_Y;
	}

	public void setLayout(int variant) {
		resetLayout(variant);
		if (variant == TYPE_CUSTOM) {
			try {
				readLayout();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				resetLayout(layoutVariant);
				return;
			}
		}
		layoutVariant = variant;
		onLayoutChanged(variant);
		for (int group = 0; group < keyScaleGroups.length; group++) {
			resizeKeyGroup(group);
		}
		snapKeys();
		invalidateView();
		if (target != null && target.isShown()) {
			target.updateSize();
		}
	}

	private void saveLayout() {
		try (RandomAccessFile raf = new RandomAccessFile(saveFile, "rw")) {
			int variant = layoutVariant;
			if (variant != TYPE_CUSTOM && raf.length() > 16) {
				try {
					if (raf.readInt() != LAYOUT_SIGNATURE) {
						throw new IOException("file signature not found");
					}
					int version = raf.readInt();
					if (version < 1 || version > LAYOUT_VERSION) {
						throw new IOException("incompatible file version");
					}
					loop:while (true) {
						int block = raf.readInt();
						int length = raf.readInt();
						switch (block) {
							case LAYOUT_EOF:
								raf.seek(raf.getFilePointer() - 8);
								raf.writeInt(LAYOUT_TYPE);
								raf.writeInt(1);
								raf.write(variant);
								raf.writeInt(LAYOUT_EOF);
								raf.writeInt(0);
								return;
							case LAYOUT_TYPE:
								raf.write(variant);
								return;
							case LAYOUT_KEYS:
								if (version >= 2) {
									int count = raf.readInt();
									length = count * 21;
								}
							default:
								if (raf.skipBytes(length) != length) {
									break loop;
								}
								break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			raf.seek(0);
			raf.writeInt(LAYOUT_SIGNATURE);
			raf.writeInt(LAYOUT_VERSION);
			raf.writeInt(LAYOUT_TYPE);
			raf.writeInt(1);
			raf.write(variant);
			if (variant != TYPE_CUSTOM) {
				raf.writeInt(LAYOUT_EOF);
				raf.writeInt(0);
				raf.setLength(raf.getFilePointer());
				return;
			}
			raf.writeInt(LAYOUT_KEYS);
			raf.writeInt(keypad.length * 21 + 4);
			raf.writeInt(keypad.length);
			for (VirtualKey key : keypad) {
				raf.writeInt(key.hashCode());
				raf.writeBoolean(key.visible);
				raf.writeInt(key.snapOrigin);
				raf.writeInt(key.snapMode);
				PointF snapOffset = key.snapOffset;
				raf.writeFloat(snapOffset.x);
				raf.writeFloat(snapOffset.y);
			}
			raf.writeInt(LAYOUT_SCALES);
			raf.writeInt(keyScales.length * 4 + 4);
			raf.writeInt(keyScales.length);
			for (float keyScale : keyScales) {
				raf.writeFloat(keyScale);
			}
			raf.writeInt(LAYOUT_KEY_SCALES);
			raf.writeInt(keypad.length * 8 + 4);
			raf.writeInt(keypad.length);
			for (VirtualKey key : keypad) {
				raf.writeInt(key.hashCode());
				raf.writeFloat(key.scale);
			}
			raf.writeInt(LAYOUT_EOF);
			raf.writeInt(0);
			raf.setLength(raf.getFilePointer());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int readLayoutType() {
		try (DataInputStream dis = new DataInputStream(new FileInputStream(saveFile))) {
			if (dis.readInt() != LAYOUT_SIGNATURE) {
				throw new IOException("file signature not found");
			}
			int version = dis.readInt();
			if (version < 1 || version > LAYOUT_VERSION) {
				throw new IOException("incompatible file version");
			}
			int custom = 0;
			while (true) {
				int block = dis.readInt();
				int length = dis.readInt();
				switch (block) {
					case LAYOUT_EOF:
						return custom == 3 ? 0 : -1;
					case LAYOUT_TYPE:
						return dis.read();
					case LAYOUT_KEYS:
						if (version >= 2) {
							int count = dis.readInt();
							length = count * 21;
						}
						if (dis.skipBytes(length) != length) {
							return -1;
						}
						custom |= 1;
						break;
					case LAYOUT_SCALES:
						if (dis.skipBytes(length) != length) {
							return -1;
						}
						custom |= 2;
						break;
					default:
						if (dis.skipBytes(length) != length) {
							return -1;
						}
				}
			}
		} catch (FileNotFoundException e) {
			Log.w(TAG, "readLayoutType() threw an FileNotFoundException: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private void readLayout() throws IOException {
		try (DataInputStream dis = new DataInputStream(new FileInputStream(saveFile))) {
			if (dis.readInt() != LAYOUT_SIGNATURE) {
				throw new IOException("file signature not found");
			}
			int version = dis.readInt();
			if (version < 1 || version > LAYOUT_VERSION) {
				throw new IOException("incompatible file version");
			}
			while (true) {
				int block = dis.readInt();
				int length = dis.readInt();
				int count;
				switch (block) {
					case LAYOUT_EOF:
						return;
					case LAYOUT_KEYS:
						count = dis.readInt();
						for (int i = 0; i < count; i++) {
							int hash = dis.readInt();
							boolean found = false;
							for (VirtualKey key : keypad) {
								if (key.hashCode() == hash) {
									if (version >= 2) {
										key.visible = dis.readBoolean();
									}
									key.snapOrigin = dis.readInt();
									key.snapMode = dis.readInt();
									key.snapOffset.x = dis.readFloat();
									key.snapOffset.y = dis.readFloat();
									found = true;
									break;
								}
							}
							if (!found) {
								dis.skipBytes(version >= 2 ? 17 : 16);
							}
						}
						break;
					case LAYOUT_SCALES:
						count = dis.readInt();
						if (version >= 3) {
							for (int i = 0; i < count; i++) {
								keyScales[i] = dis.readFloat();
							}
						} else if (count * 2 <= keyScales.length) {
							for (int i = 0, len = count * 2; i < len;) {
								float v = dis.readFloat();
								keyScales[i++] = v;
								keyScales[i++] = v;
							}
						} else {
							dis.skipBytes(count * 4);
						}
						break;
					case LAYOUT_KEY_SCALES:
						count = dis.readInt();
						for (int i = 0; i < count; i++) {
							int hash = dis.readInt();
							float s = dis.readFloat();
							for (VirtualKey key : keypad) {
								if (key.hashCode() == hash) {
									key.scale = s;
									break;
								}
							}
						}
						break;
					default:
						dis.skipBytes(length);
						break;
				}
			}
		}
	}

	public String[] getKeyNames() {
		String[] names = new String[KEYBOARD_SIZE];
		for (int i = 0; i < KEYBOARD_SIZE; i++) {
			names[i] = keypad[i].label;
		}
		return names;
	}

	public boolean[] getKeysVisibility() {
		boolean[] states = new boolean[KEYBOARD_SIZE];
		for (int i = 0; i < KEYBOARD_SIZE; i++) {
			states[i] = !keypad[i].visible;
		}
		return states;
	}

	public void setKeysVisibility(boolean[] states) {
		for (int i = 0; i < KEYBOARD_SIZE; i++) {
			keypad[i].visible = !states[i];
		}
		invalidateView();
	}

	@Override
	public void setTarget(Canvas canvas) {
		target = canvas;
		highlightGroup(-1);
	}

	private void setSnap(int key, int origin, int mode, float offsetX, float offsetY, boolean visible) {
		VirtualKey vKey = keypad[key];
		vKey.snapOrigin = origin;
		vKey.snapMode = mode;
		vKey.snapOffset.set(offsetX, offsetY);
		vKey.snapValid = false;
		vKey.visible = visible;
	}

	private void setSnap(int key, int origin, int mode, boolean visible) {
		setSnap(key, origin, mode, 0, 0, visible);
	}

	private boolean findSnap(int target, int origin) {
		VirtualKey tk = keypad[target];
		VirtualKey ok = keypad[origin];
		tk.snapMode = RectSnap.getSnap(tk.rect, ok.rect, snapRadius, RectSnap.COARSE_MASK, true);
		if (tk.snapMode != RectSnap.NO_SNAP) {
			tk.snapOrigin = origin;
			tk.snapOffset.set(0, 0);
			for (int i = 0; i < keypad.length; i++) {
				origin = keypad[origin].snapOrigin;
				if (origin == SCREEN) {
					return true;
				}
			}
		}
		return false;
	}

	private void snapKey(int key, int level) {
		if (level >= snapStack.length) {
			Log.d(TAG, "Snap loop detected: ");
			for (int i = 1; i < snapStack.length; i++) {
				System.out.print(snapStack[i]);
				System.out.print(", ");
			}
			Log.d(TAG, String.valueOf(key));
			return;
		}
		snapStack[level] = key;
		VirtualKey vKey = keypad[key];
		if (vKey.snapOrigin == SCREEN) {
			RectSnap.snap(vKey.rect, screen, vKey.snapMode, vKey.snapOffset);
		} else {
			if (!keypad[vKey.snapOrigin].snapValid) {
				snapKey(vKey.snapOrigin, level + 1);
			}
			RectSnap.snap(vKey.rect, keypad[vKey.snapOrigin].rect,
					vKey.snapMode, vKey.snapOffset);
		}
		vKey.snapValid = true;
	}

	private void snapKeys() {
		obscuresVirtualScreen = false;
		boolean isPhone = isPhone();
		for (int i = 0; i < keypad.length; i++) {
			snapKey(i, 0);
			VirtualKey key = keypad[i];
			RectF rect = key.rect;
			key.corners = (int) (Math.min(rect.width(), rect.height()) * 0.25F);
			if (!isPhone && RectF.intersects(rect, virtualScreen)) {
				obscuresVirtualScreen = true;
				key.opaque = false;
			} else {
				key.opaque = true;
			}
		}
		boolean opaque = !obscuresVirtualScreen || settings.vkForceOpacity;
		for (VirtualKey key : keypad) {
			key.opaque &= opaque;
		}
	}

	public boolean isPhone() {
		return layoutVariant == TYPE_PHONE || layoutVariant == TYPE_PHONE_ARROWS;
	}

	private void highlightGroup(int group) {
		for (VirtualKey aKeypad : keypad) {
			aKeypad.selected = false;
		}
		if (group >= 0) {
			for (int key = 0; key < keyScaleGroups[group].length; key++) {
				keypad[keyScaleGroups[group][key]].selected = true;
			}
		}
	}

	public int getLayoutEditMode() {
		return layoutEditMode;
	}

	public void setLayoutEditMode(int mode) {
		layoutEditMode = mode;
		int group = -1;
		if (mode == LAYOUT_SCALES) {
			editedIndex = 0;
			group = 0;
		} else if (mode == LAYOUT_KEYS) {
			editedIndex = KEY_FIRE;
		}
		highlightGroup(group);
		handler.removeCallbacks(this);
		visible = true;
		invalidateView();
		hide();
	}

	private void resizeKey(int key, float w, float h) {
		VirtualKey vKey = keypad[key];
		vKey.resize(w * vKey.scale, h * vKey.scale);
		vKey.snapValid = false;
	}

	private void resizeKeyGroup(int group) {
		float sizeX = keySize * keyScales[group * 2];
		float sizeY = keySize * keyScales[group * 2 + 1];
		for (int key = 0; key < keyScaleGroups[group].length; key++) {
			resizeKey(keyScaleGroups[group][key], sizeX, sizeY);
		}
	}

	@Override
	public void resize(RectF screen, float left, float top, float right, float bottom) {
		this.screen = screen;
		virtualScreen.set(left, top, right, bottom);
		snapRadius = keyScales[0];
		for (int i = 1; i < keyScales.length; i++) {
			if (keyScales[i] < snapRadius) {
				snapRadius = keyScales[i];
			}
		}

		float keySize = getKeySize(screen.width(), screen.height());
		snapRadius = keySize * snapRadius / 4;
		this.keySize = keySize;
		if (layoutVariant == TYPE_ANLAND) {
			setAnlandRadialLayout();
		}
		for (int group = 0; group < keyScaleGroups.length; group++) {
			resizeKeyGroup(group);
		}
		snapKeys();
		invalidateView();
		int delay = settings.vkHideDelay;
		if (delay > 0 && obscuresVirtualScreen && layoutEditMode == LAYOUT_EOF) {
			for (VirtualKey key : associatedKeys) {
				if (key != null) {
					return;
				}
			}
			handler.postDelayed(this, delay);
		}
	}

	private float getKeySize(float screenWidth, float screenHeight) {
		float min = screenWidth;
		float max = screenHeight;
		boolean landscape = min > max;
		if (min > max) {
			float tmp = max;
			max = min;
			min = tmp;
		}

		boolean nonWide = max / min < 2;
		float keySize;
		if (isPhone()) {
			keySize = min / 6.0F;
		} else if (nonWide || landscape) {
			keySize = max / 12F;
		} else {
			keySize = min / 6.5F;
		}
		return keySize;
	}

	@Override
	public void paint(CanvasWrapper g) {
		if (visible) {
			for (int i = 0; i < keypad.length; i++) {
				VirtualKey key = keypad[i];
				if (key.visible) {
					key.paint(g);
				} else if (layoutEditMode == LAYOUT_KEYS) {
					int alpha = 0x55000000;
					g.setFillColor(alpha | 0x882222);
					g.setTextColor(alpha | 0xFFFFFF);
					g.setDrawColor(0x77FF4444);
					g.fillRoundRect(key.rect, key.corners, key.corners);
					g.drawRoundRect(key.rect, key.corners, key.corners);
					g.drawString(key.label, key.rect.centerX(), key.rect.centerY());
				}
				if (layoutEditMode == LAYOUT_KEYS && i == editedIndex) {
					g.setDrawColor(0xFF00E5FF);
					RectF r = new RectF(key.rect.left - 4, key.rect.top - 4,
							key.rect.right + 4, key.rect.bottom + 4);
					g.drawRoundRect(r, key.corners + 4, key.corners + 4);
				}
			}

			if (layoutEditMode == LAYOUT_KEYS) {
				paintEditorToolbar(g);
			}
		}
	}

	private void paintEditorToolbar(CanvasWrapper g) {
		if (screen == null) return;
		float sw = screen.width();
		float barW = Math.min(sw * 0.96f, 480f);
		float barH = 50f;
		float barL = (sw - barW) / 2f;
		float barT = 12f;
		toolbarRect.set(barL, barT, barL + barW, barT + barH);

		// Toolbar Background
		g.setFillColor(0xEE1E1E24);
		g.setDrawColor(0xFF5A5A6E);
		g.fillRoundRect(toolbarRect, 14, 14);
		g.drawRoundRect(toolbarRect, 14, 14);

		float pad = 5f;
		float btnH = barH - pad * 2f;
		float curX = barL + pad;

		// 1. Info Chip
		String info = editedIndex >= 0 ? "[" + keypad[editedIndex].label + "] " +
				String.format(java.util.Locale.US, "%.1fx", keypad[editedIndex].scale) : "Chạm chọn";
		float infoW = 95f;
		g.setTextColor(0xFF00E5FF);
		g.drawString(info, curX + infoW / 2f, barT + barH / 2f);
		curX += infoW + pad;

		// 2. Button [ - ]
		float wMinus = 44f;
		btnMinusRect.set(curX, barT + pad, curX + wMinus, barT + pad + btnH);
		g.setFillColor(0xFF333344);
		g.setDrawColor(0xFF666688);
		g.fillRoundRect(btnMinusRect, 8, 8);
		g.drawRoundRect(btnMinusRect, 8, 8);
		g.setTextColor(0xFFFFFFFF);
		g.drawString("-", btnMinusRect.centerX(), btnMinusRect.centerY());
		curX += wMinus + pad;

		// 3. Button [ + ]
		float wPlus = 44f;
		btnPlusRect.set(curX, barT + pad, curX + wPlus, barT + pad + btnH);
		g.setFillColor(0xFF333344);
		g.setDrawColor(0xFF666688);
		g.fillRoundRect(btnPlusRect, 8, 8);
		g.drawRoundRect(btnPlusRect, 8, 8);
		g.setTextColor(0xFFFFFFFF);
		g.drawString("+", btnPlusRect.centerX(), btnPlusRect.centerY());
		curX += wPlus + pad;

		// 4. Button [ Ẩn / Hiện ]
		float wVis = 68f;
		btnVisRect.set(curX, barT + pad, curX + wVis, barT + pad + btnH);
		boolean isVis = editedIndex >= 0 && keypad[editedIndex].visible;
		g.setFillColor(isVis ? 0xFF882222 : 0xFF227733);
		g.setDrawColor(isVis ? 0xFFFF4444 : 0xFF44CC66);
		g.fillRoundRect(btnVisRect, 8, 8);
		g.drawRoundRect(btnVisRect, 8, 8);
		g.setTextColor(0xFFFFFFFF);
		g.drawString(isVis ? "Ẩn" : "Hiện", btnVisRect.centerX(), btnVisRect.centerY());
		curX += wVis + pad;

		// 5. Button [ Đặt lại ]
		float wReset = 84f;
		btnResetRect.set(curX, barT + pad, curX + wReset, barT + pad + btnH);
		g.setFillColor(0xFF444455);
		g.setDrawColor(0xFF777799);
		g.fillRoundRect(btnResetRect, 8, 8);
		g.drawRoundRect(btnResetRect, 8, 8);
		g.setTextColor(0xFFEEEEEE);
		g.drawString("Đặt lại", btnResetRect.centerX(), btnResetRect.centerY());
		curX += wReset + pad;

		// 6. Button [ Xong ]
		float wDone = barL + barW - pad - curX;
		btnDoneRect.set(curX, barT + pad, curX + wDone, barT + pad + btnH);
		g.setFillColor(0xFF2E7D32);
		g.setDrawColor(0xFF4CAF50);
		g.fillRoundRect(btnDoneRect, 8, 8);
		g.drawRoundRect(btnDoneRect, 8, 8);
		g.setTextColor(0xFFFFFFFF);
		g.drawString("Xong", btnDoneRect.centerX(), btnDoneRect.centerY());
	}

	@Override
	public boolean pointerPressed(int pointer, float x, float y) {
		switch (layoutEditMode) {
			case LAYOUT_EOF:
				if (pointer > associatedKeys.length) {
					return false;
				}
				for (VirtualKey key : keypad) {
					if (key.contains(x, y)) {
						vibrate();
						associatedKeys[pointer] = key;
						key.onDown();
						invalidateView();
						break;
					}
				}
				break;
			case LAYOUT_KEYS:
				if (toolbarRect.contains(x, y)) {
					if (btnMinusRect.contains(x, y)) {
						if (editedIndex >= 0) {
							VirtualKey key = keypad[editedIndex];
							key.scale = Math.max(0.4f, Math.round((key.scale - 0.1f) * 10f) / 10f);
							key.resize(keySize * key.scale, keySize * key.scale);
							key.corners = (int) (Math.min(key.rect.width(), key.rect.height()) * 0.25F);
							invalidateView();
						}
					} else if (btnPlusRect.contains(x, y)) {
						if (editedIndex >= 0) {
							VirtualKey key = keypad[editedIndex];
							key.scale = Math.min(2.5f, Math.round((key.scale + 0.1f) * 10f) / 10f);
							key.resize(keySize * key.scale, keySize * key.scale);
							key.corners = (int) (Math.min(key.rect.width(), key.rect.height()) * 0.25F);
							invalidateView();
						}
					} else if (btnVisRect.contains(x, y)) {
						if (editedIndex >= 0) {
							keypad[editedIndex].visible = !keypad[editedIndex].visible;
							invalidateView();
						}
					} else if (btnResetRect.contains(x, y)) {
						setAnlandRadialLayout();
						invalidateView();
					} else if (btnDoneRect.contains(x, y)) {
						setLayoutEditMode(LAYOUT_EOF);
						onLayoutChanged(TYPE_CUSTOM);
						saveLayout();
						invalidateView();
					}
					return true;
				}

				editedIndex = -1;
				for (int i = keypad.length - 1; i >= 0; i--) {
					if (keypad[i].rect.contains(x, y)) {
						editedIndex = i;
						offsetX = x - keypad[i].rect.left;
						offsetY = y - keypad[i].rect.top;
						break;
					}
				}
				invalidateView();
				break;
			case LAYOUT_SCALES:
				int index = -1;
				for (int group = 0; group < keyScaleGroups.length && index < 0; group++) {
					for (int key = 0; key < keyScaleGroups[group].length && index < 0; key++) {
						if (keypad[keyScaleGroups[group][key]].contains(x, y)) {
							index = group;
						}
					}
				}
				if (index >= 0) {
					editedIndex = index;
					highlightGroup(index);
					invalidateView();
				}
				offsetX = x;
				offsetY = y;
				prevScaleX = keyScales[editedIndex * 2];
				prevScaleY = keyScales[editedIndex * 2 + 1];
				break;
		}
		return false;
	}

	@Override
	public boolean pointerDragged(int pointer, float x, float y) {
		switch (layoutEditMode) {
			case LAYOUT_EOF:
				if (pointer > associatedKeys.length) {
					return false;
				}
				VirtualKey aKey = associatedKeys[pointer];
				if (aKey == null) {
					pointerPressed(pointer, x, y);
				} else if (!aKey.contains(x, y)) {
					associatedKeys[pointer] = null;
					aKey.onUp();
					invalidateView();
					pointerPressed(pointer, x, y);
				}
				break;
			case LAYOUT_KEYS:
				if (editedIndex >= 0 && screen != null) {
					VirtualKey key = keypad[editedIndex];
					float newLeft = x - offsetX;
					float newTop = y - offsetY;
					newLeft = Math.max(0, Math.min(screen.width() - key.rect.width(), newLeft));
					newTop = Math.max(0, Math.min(screen.height() - key.rect.height(), newTop));
					key.snapOrigin = SCREEN;
					key.snapMode = RectSnap.INT_NORTHWEST;
					key.snapOffset.set(newLeft, newTop);
					key.rect.offsetTo(newLeft, newTop);
					key.snapValid = true;
					invalidateView();
					return true;
				}
				break;
			case LAYOUT_SCALES:
				float dx = x - offsetX;
				float dy = offsetY - y;
				float scale;
				int index = this.editedIndex * 2;
				if (Math.abs(dx) > Math.abs(dy)) {
					scale = prevScaleX + dx / Math.min(screen.centerX(), screen.centerY());
				} else {
					scale = prevScaleY + dy / Math.min(screen.centerX(), screen.centerY());
					index++;
				}
				if (Math.abs(1 - scale) <= SCALE_SNAP_RADIUS) {
					scale = 1;
				} else {
					for (int i = index % 2; i < keyScales.length; i += 2) {
						if (i != index && Math.abs(keyScales[i] - scale) <= SCALE_SNAP_RADIUS) {
							scale = keyScales[i];
							break;
						}
					}
				}
				keyScales[index] = scale;
				resizeKeyGroup(this.editedIndex);
				snapKeys();
				invalidateView();
				break;
		}
		return false;
	}

	@Override
	public boolean pointerReleased(int pointer, float x, float y) {
		if (layoutEditMode == LAYOUT_EOF) {
			if (pointer > associatedKeys.length) {
				return false;
			}
			VirtualKey key = associatedKeys[pointer];
			if (key != null) {
				associatedKeys[pointer] = null;
				key.onUp();
				invalidateView();
			}
		} else if (layoutEditMode == LAYOUT_KEYS) {
			if (editedIndex >= 0 && screen != null) {
				VirtualKey key = keypad[editedIndex];
				key.snapOrigin = SCREEN;
				key.snapMode = RectSnap.INT_NORTHWEST;
				key.snapOffset.set(key.rect.left, key.rect.top);
				key.snapValid = true;
				invalidateView();
			}
		}
		return false;
	}

	@Override
	public void show() {
		if (settings.vkHideDelay > 0 && obscuresVirtualScreen) {
			handler.removeCallbacks(this);
			if (!visible) {
				visible = true;
				invalidateView();
			}
		}
	}

	@Override
	public void hide() {
		long delay = settings.vkHideDelay;
		if (delay > 0 && obscuresVirtualScreen && layoutEditMode == LAYOUT_EOF) {
			handler.postDelayed(this, delay);
		}
	}

	@Override
	public void cancel() {
		for (VirtualKey key : keypad) {
			key.selected = false;
			handler.removeCallbacks(key);
		}
	}

	@Override
	public void run() {
		visible = false;
		invalidateView();
	}

	@Override
	public boolean keyPressed(int keyCode) {
		int hashCode = 31 * (31 + keyCode);
		for (VirtualKey key : keypad) {
			if (key.hashCode() == hashCode) {
				key.selected = true;
				invalidateView();
				break;
			}
		}
		return false;
	}

	@Override
	public boolean keyRepeated(int keyCode) {
		return false;
	}

	@Override
	public boolean keyReleased(int keyCode) {
		int hashCode = 31 * (31 + keyCode);
		for (VirtualKey key : keypad) {
			if (key.hashCode() == hashCode) {
				key.selected = false;
				invalidateView();
				break;
			}
		}
		return false;
	}

	private void vibrate() {
		if (settings.vkFeedback) ContextHolder.vibrateKey(FEEDBACK_DURATION);
	}

	public void setView(View view) {
		overlayView = view;
		invalidateView();
	}

	private void invalidateView() {
		if (overlayView != null) {
			overlayView.postInvalidate();
		}
	}

	public int getKeyStatesVodafone() {
		int keyStates = 0;
		for (int i = 0; i < keypad.length; i++) {
			VirtualKey key = keypad[i];
			if (key.selected) {
				keyStates |= getKeyBit(i);
			}
		}
		return keyStates;
	}

	private int getKeyBit(int vKey) {
		switch (vKey) {
			case KEY_NUM0      : return 1;       // 0 0 key         KEY_NUM0       = 9;
			case KEY_NUM1      : return 1 <<  1; // 1 1 key         KEY_NUM1       = 0;
			case KEY_NUM2      : return 1 <<  2; // 2 2 key         KEY_NUM2       = 1;
			case KEY_NUM3      : return 1 <<  3; // 3 3 key         KEY_NUM3       = 2;
			case KEY_NUM4      : return 1 <<  4; // 4 4 key         KEY_NUM4       = 3;
			case KEY_NUM5      : return 1 <<  5; // 5 5 key         KEY_NUM5       = 4;
			case KEY_NUM6      : return 1 <<  6; // 6 6 key         KEY_NUM6       = 5;
			case KEY_NUM7      : return 1 <<  7; // 7 7 key         KEY_NUM7       = 6;
			case KEY_NUM8      : return 1 <<  8; // 8 8 key         KEY_NUM8       = 7;
			case KEY_NUM9      : return 1 <<  9; // 9 9 key         KEY_NUM9       = 8;
			case KEY_STAR      : return 1 << 10; // 10 * key        KEY_STAR       = 10;
			case KEY_POUND     : return 1 << 11; // 11 # key        KEY_POUND      = 11;
			case KEY_UP        : return 1 << 12; // 12 Up key       KEY_UP         = 17;
			case KEY_LEFT      : return 1 << 13; // 13 Left key     KEY_LEFT       = 19;
			case KEY_RIGHT     : return 1 << 14; // 14 Right key    KEY_RIGHT      = 20;
			case KEY_DOWN      : return 1 << 15; // 15 Down key     KEY_DOWN       = 22;
			case KEY_FIRE      : return 1 << 16; // 16 Select key   KEY_FIRE       = 24;
			case KEY_SOFT_LEFT : return 1 << 17; // 17 Softkey 1    KEY_SOFT_LEFT  = 12;
			case KEY_SOFT_RIGHT: return 1 << 18; // 18 Softkey 2    KEY_SOFT_RIGHT = 13;
			// TODO: 05.08.2020 Softkey3 mapped to KEY_C
			case KEY_C         : return 1 << 19; // 19 Softkey 3    KEY_C          = 15;
			case KEY_UP_RIGHT  : return 1 << 20; // 20 Upper Right  KEY_UP_RIGHT   = 18;
			case KEY_UP_LEFT   : return 1 << 21; // 21 Upper Left   KEY_UP_LEFT    = 16;
			case KEY_DOWN_RIGHT: return 1 << 22; // 22 Lower Right  KEY_DOWN_RIGHT = 23;
			case KEY_DOWN_LEFT : return 1 << 23; // 23 Lower Left   KEY_DOWN_LEFT  = 21;
		}
		return 0;
	}

	public void saveScreenParams() {
		float scale = virtualScreen.width() / screen.width();
		settings.screenScaleRatio = Math.round(scale * 100);
		settings.screenGravity = 1;
		Canvas.setScale(settings.screenGravity, settings.screenScaleType, settings.screenScaleRatio);
		ProfilesManager.saveConfig(settings);
	}

	private class VirtualKey implements Runnable {
		final String label;
		final int keyCode;
		final RectF rect = new RectF();
		final PointF snapOffset = new PointF();
		float scale = 1.0f;
		int snapOrigin;
		int snapMode;
		boolean snapValid;
		boolean selected;
		boolean visible = true;
		boolean opaque = true;
		int corners;
		private final int hashCode;
		private int repeatCount;

		VirtualKey(int keyCode, String label) {
			this.keyCode = keyCode;
			this.label = label;
			hashCode = 31 * (31 + this.keyCode);
		}

		void resize(float width, float height) {
			rect.right = rect.left + width;
			rect.bottom = rect.top + height;
		}

		boolean contains(float x, float y) {
			return visible && rect.contains(x, y);
		}

		void paint(CanvasWrapper g) {
			int bgColor;
			int fgColor;
			if (selected) {
				bgColor = settings.vkBgColorSelected;
				fgColor = settings.vkFgColorSelected;
			} else {
				bgColor = settings.vkBgColor;
				fgColor = settings.vkFgColor;
			}
			int alpha = (opaque || layoutEditMode != LAYOUT_EOF ? 0xFF : settings.vkAlpha) << 24;
			g.setFillColor(alpha | bgColor);
			g.setTextColor(alpha | fgColor);
			g.setDrawColor(alpha | settings.vkOutlineColor);

			switch (settings.vkButtonShape) {
				case ROUND_RECT_SHAPE:
					g.fillRoundRect(rect, corners, corners);
					g.drawRoundRect(rect, corners, corners);
					break;
				case RECT_SHAPE:
					g.fillRect(rect);
					g.drawRect(rect);
					break;
				case OVAL_SHAPE:
					g.fillArc(rect, 0, 360);
					g.drawArc(rect, 0, 360);
					break;
			}
			g.drawString(label, rect.centerX(), rect.centerY());
		}

		@NonNull
		public String toString() {
			return "[" + label + ": " + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + "]";
		}

		public int hashCode() {
			return hashCode;
		}

		@Override
		public void run() {
			if (target == null) {
				selected = false;
				repeatCount = 0;
				return;
			}
			if (selected) {
				onRepeat();
			} else {
				repeatCount = 0;
			}
		}

		public void onRepeat() {
			handler.postDelayed(this, repeatCount > 6 ? 80 : REPEAT_INTERVALS[repeatCount++]);
			target.postKeyRepeated(keyCode);
		}

		protected void onDown() {
			selected = true;
			target.postKeyPressed(keyCode);
			handler.postDelayed(this, 400);
		}

		public void onUp() {
			selected = false;
			handler.removeCallbacks(this);
			target.postKeyReleased(keyCode);
		}
	}

	private class DualKey extends VirtualKey {

		final int secondKeyCode;
		private final int hashCode;

		DualKey(int keyCode, int secondKeyCode, String label) {
			super(keyCode, label);
			if (secondKeyCode == 0) throw new IllegalArgumentException();
			this.secondKeyCode = secondKeyCode;
			hashCode = 31 * (31 + this.keyCode) + this.secondKeyCode;
		}

		@Override
		protected void onDown() {
			super.onDown();
			target.postKeyPressed(secondKeyCode);
		}

		@Override
		public void onUp() {
			super.onUp();
			target.postKeyReleased(secondKeyCode);
		}

		@Override
		public void onRepeat() {
			super.onRepeat();
			target.postKeyRepeated(secondKeyCode);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}

	private class MenuKey extends VirtualKey {

		MenuKey() {
			super(KeyMapper.KEY_OPTIONS_MENU, "M");
		}

		@Override
		protected void onDown() {
			selected = true;
			handler.postDelayed(this, 500);
		}

		@Override
		public void onUp() {
			if (selected) {
				selected = false;
				handler.removeCallbacks(this);
				MicroActivity activity = ContextHolder.getActivity();
				if (activity != null) {
					activity.openOptionsMenu();
				}
			}
		}

		@Override
		public void run() {
			selected = false;
			MicroActivity activity = ContextHolder.getActivity();
			if (activity != null) {
				activity.runOnUiThread(activity::showExitConfirmation);
			}
		}
	}
}
