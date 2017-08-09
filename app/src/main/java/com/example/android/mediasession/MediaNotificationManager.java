/*
 *
 *  * Copyright 2017 Nazmul Idris. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.example.android.mediasession;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

/**
 * Keeps track of a notification and updates it automatically for a given MediaSession. This is
 * required so that the music service don't get killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    private static final String ACTION_PAUSE = "com.example.android.musicplayercodelab.pause";
    private static final String ACTION_PLAY = "com.example.android.musicplayercodelab.play";
    private static final String ACTION_NEXT = "com.example.android.musicplayercodelab.next";
    private static final String ACTION_PREV = "com.example.android.musicplayercodelab.prev";
    private static final String TAG = "NotificationManager";

    private final MusicService mService;

    private final NotificationManager mNotificationManager;

    private final NotificationCompat.Action mPlayAction;
    private final NotificationCompat.Action mPauseAction;
    private final NotificationCompat.Action mNextAction;
    private final NotificationCompat.Action mPrevAction;

    private boolean mStarted;

    public MediaNotificationManager(MusicService service) {
        mService = service;

        String pkg = mService.getPackageName();
        PendingIntent playIntent =
                PendingIntent.getBroadcast(
                        mService,
                        REQUEST_CODE,
                        new Intent(ACTION_PLAY).setPackage(pkg),
                        PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pauseIntent =
                PendingIntent.getBroadcast(
                        mService,
                        REQUEST_CODE,
                        new Intent(ACTION_PAUSE).setPackage(pkg),
                        PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent nextIntent =
                PendingIntent.getBroadcast(
                        mService,
                        REQUEST_CODE,
                        new Intent(ACTION_NEXT).setPackage(pkg),
                        PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent prevIntent =
                PendingIntent.getBroadcast(
                        mService,
                        REQUEST_CODE,
                        new Intent(ACTION_PREV).setPackage(pkg),
                        PendingIntent.FLAG_CANCEL_CURRENT);

        mPlayAction =
                new NotificationCompat.Action(
                        R.drawable.ic_play_arrow_white_24dp,
                        mService.getString(R.string.label_play),
                        playIntent);
        mPauseAction =
                new NotificationCompat.Action(
                        R.drawable.ic_pause_white_24dp,
                        mService.getString(R.string.label_pause),
                        pauseIntent);
        mNextAction =
                new NotificationCompat.Action(
                        R.drawable.ic_skip_next_white_24dp,
                        mService.getString(R.string.label_next),
                        nextIntent);
        mPrevAction =
                new NotificationCompat.Action(
                        R.drawable.ic_skip_previous_white_24dp,
                        mService.getString(R.string.label_previous),
                        prevIntent);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PREV);

        mService.registerReceiver(this, filter);

        mNotificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case ACTION_PAUSE:
                mService.mCallback.onPause();
                break;
            case ACTION_PLAY:
                mService.mCallback.onPlay();
                break;
            case ACTION_NEXT:
                mService.mCallback.onSkipToNext();
                break;
            case ACTION_PREV:
                mService.mCallback.onSkipToPrevious();
                break;
        }
    }

    public void update(
            MediaMetadataCompat metadata,
            PlaybackStateCompat state,
            MediaSessionCompat.Token token) {
        if (state == null
            || state.getState() == PlaybackStateCompat.STATE_STOPPED
            || state.getState() == PlaybackStateCompat.STATE_NONE) {
            mService.stopForeground(true);
            try {
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // Ignore receiver not registered.
            }
            mService.stopSelf();
            Log.d(TAG, "update: stopForeground(), stopSelf()");
            return;
        }
        if (metadata == null) {
            return;
        }
        boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mService);
        MediaDescriptionCompat description = metadata.getDescription();

        notificationBuilder
                .setStyle(
                        new NotificationCompat.MediaStyle()
                                .setMediaSession(token)
                                .setShowActionsInCompactView(0, 1, 2))
                .setColor(
                        mService.getApplication().getResources().getColor(R.color.notification_bg))
                .setSmallIcon(R.drawable.ic_stat_image_audiotrack)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(createContentIntent())
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(MusicLibrary.getAlbumBitmap(mService, description.getMediaId()))
                .setOngoing(isPlaying)
                .setWhen(isPlaying ? System.currentTimeMillis() - state.getPosition() : 0)
                .setShowWhen(isPlaying)
                .setUsesChronometer(isPlaying);

        // If skip to next action is enabled
        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            notificationBuilder.addAction(mPrevAction);
        }

        notificationBuilder.addAction(isPlaying ? mPauseAction : mPlayAction);

        // If skip to prev action is enabled
        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            notificationBuilder.addAction(mNextAction);
        }

        Notification notification = notificationBuilder.build();

        if (isPlaying && !mStarted) {
            mService.startService(new Intent(mService.getApplicationContext(), MusicService.class));
            mService.startForeground(NOTIFICATION_ID, notification);
            mStarted = true;
        } else {
            if (!isPlaying) {
                mService.stopForeground(false);
                mStarted = false;
            }
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private PendingIntent createContentIntent() {
        Intent openUI = new Intent(mService, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                mService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}