/*
 * Copyright 2025 Yury Kharchenko
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

package com.j2me.lixin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/**
 * Keeps the game process alive while the app is in the background, so tabs do
 * not get killed when the user swipes home.
 */
public class ForegroundService extends Service {
	private static final String CHANNEL_ID = "game_running";
	private static final int NOTIFICATION_ID = 1;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		createChannel();
		startForeground(NOTIFICATION_ID, buildNotification());
		return START_STICKY;
	}

	private void createChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager nm = getSystemService(NotificationManager.class);
			NotificationChannel channel = new NotificationChannel(
					CHANNEL_ID,
					getString(R.string.notif_running_title),
					NotificationManager.IMPORTANCE_LOW);
			channel.setShowBadge(false);
			nm.createNotificationChannel(channel);
		}
	}

	private Notification buildNotification() {
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
				? new Notification.Builder(this, CHANNEL_ID)
				: new Notification.Builder(this);
		return builder
				.setSmallIcon(R.drawable.ic_more_vert)
				.setContentTitle(getString(R.string.notif_running_title))
				.setContentText(getString(R.string.notif_running_text))
				.setOngoing(true)
				.setContentIntent(pi)
				.build();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
