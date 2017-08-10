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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;


/**
 * Keeps track of a notification and updates it automatically for a given MediaSession. This is
 * required so that the music service don't get killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {

    private static final String TAG = "MS_NotificationManager";
    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;
    private static final String CHANNEL_ID = "com.example.android.musicplayer.channel";

    private static final String ACTION_PAUSE = "com.example.android.musicplayer.pause";
    private static final String ACTION_PLAY = "com.example.android.musicplayer.play";
    private static final String ACTION_NEXT = "com.example.android.musicplayer.next";
    private static final String ACTION_PREV = "com.example.android.musicplayer.prev";

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

        Log.d(TAG, "registered broadcast receiver");
    }

    public void onDestroy() {
        try {
            mService.unregisterReceiver(this);
        } catch (IllegalArgumentException ex) {
            // Ignore receiver not registered.
        }
        Log.d(TAG, "onDestroy: unregistered broadcast receiver");
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

    public void update(MediaMetadataCompat metadata,
                       @NonNull PlaybackStateCompat state,
                       MediaSessionCompat.Token token) {
        if (state.getState() == PlaybackStateCompat.STATE_STOPPED
            || state.getState() == PlaybackStateCompat.STATE_NONE) {
            handlePlayerStoppedState();
        } else if (metadata == null) {
            // Do nothing.
        } else {
            handleOtherPlayerStates(metadata, state, token);
        }
    }

    private void handlePlayerStoppedState() {
        mService.stopForeground(true);
        mService.stopSelf();
        Log.d(TAG, "update: stopForeground(), stopSelf()");
    }

    private void handleOtherPlayerStates(MediaMetadataCompat metadata,
                                         @NonNull PlaybackStateCompat state,
                                         MediaSessionCompat.Token token) {
        boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;

        createChannel();

        MediaDescriptionCompat description = metadata.getDescription();

        NotificationCompat.Builder builder =
                buildNotification(state, token, isPlaying, description);

        // If skip to next action is enabled.
        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(mPrevAction);
        }

        builder.addAction(isPlaying ? mPauseAction : mPlayAction);

        // If skip to prev action is enabled.
        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            builder.addAction(mNextAction);
        }

        Notification notification = builder.build();

        if (isPlaying && !mStarted) {
            // Show notification for the first time.
            Intent intent = new Intent(mService.getApplicationContext(), MusicService.class);
            ContextCompat.startForegroundService(mService, intent);
            mService.startForeground(NOTIFICATION_ID, notification);
            mStarted = true;
        } else {
            // Paused.
            if (!isPlaying) {
                mService.stopForeground(false);
                mStarted = false;
            }
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private NotificationCompat.Builder buildNotification(@NonNull PlaybackStateCompat state,
                                                         MediaSessionCompat.Token token,
                                                         boolean isPlaying,
                                                         MediaDescriptionCompat description) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mService, CHANNEL_ID);
        builder
                .setStyle(
                        new MediaStyle()
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
                .setUsesChronometer(isPlaying)
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService, PlaybackStateCompat.ACTION_STOP));
        return builder;
    }

    private void createChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = CHANNEL_ID;
        // The user-visible name of the channel.
        CharSequence name = "MediaSession";
        // The user-visible description of the channel.
        String description = "MediaSession and MediaPlayer";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.enableLights(true);
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        mChannel.setLightColor(Color.RED);
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        mNotificationManager.createNotificationChannel(mChannel);
    }

    private PendingIntent createContentIntent() {
        Intent openUI = new Intent(mService, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                mService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT);
    }

}