package ru.playsoftware.j2meloader.hud;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Memory-efficient asset bitmap cache for custom HUD icons (NRO, etc.).
 */
public final class HudAssetManager {
    private static final Map<String, Bitmap> sCache = new HashMap<>();

    private HudAssetManager() {}

    public static Bitmap getBitmap(Context context, String assetPath) {
        if (assetPath == null || assetPath.isEmpty()) return null;
        synchronized (sCache) {
            Bitmap bm = sCache.get(assetPath);
            if (bm != null && !bm.isRecycled()) return bm;
            try (InputStream is = context.getAssets().open(assetPath)) {
                bm = BitmapFactory.decodeStream(is);
                if (bm != null) {
                    sCache.put(assetPath, bm);
                }
                return bm;
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static void clear() {
        synchronized (sCache) {
            sCache.clear();
        }
    }
}
