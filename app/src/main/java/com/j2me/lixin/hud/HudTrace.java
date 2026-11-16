package com.j2me.lixin.hud;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * File-based diagnostics. MIUI drops third-party logcat entirely (even Log.e), so the
 * only reliable channel on those ROMs is a trace file the user can hand back.
 */
public final class HudTrace {

    private static final SimpleDateFormat TIME =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static File file;

    private HudTrace() {
    }

    public static void log(Context context, String message) {
        if (context == null) {
            return;
        }
        try {
            if (file == null) {
                File dir = context.getExternalFilesDir(null);
                if (dir == null) {
                    dir = context.getFilesDir();
                }
                file = new File(dir, "hud_trace.txt");
            }
            try (FileWriter w = new FileWriter(file, true)) {
                w.write(TIME.format(new Date()) + "  " + message + "\n");
            }
        } catch (IOException ignored) {
        }
    }

    /** Clears the trace once, at HUD setup, so one test run is easy to read. */
    public static void reset(Context context) {
        if (context == null) {
            return;
        }
        try {
            if (file == null) {
                File dir = context.getExternalFilesDir(null);
                if (dir == null) {
                    dir = context.getFilesDir();
                }
                file = new File(dir, "hud_trace.txt");
            }
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        } catch (Exception ignored) {
        }
    }
}
