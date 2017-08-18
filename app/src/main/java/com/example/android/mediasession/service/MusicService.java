/*
 * Copyright 2017 Nazmul Idris. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.mediasession.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.example.android.mediasession.service.contentcatalogs.MusicLibrary;
import com.example.android.mediasession.service.notifications.MediaNotificationManager;
import com.example.android.mediasession.service.players.MediaPlayerHolder;

import java.util.List;

public class MusicService extends MediaBrowserServiceCompat {

    private static final String TAG = "MS_MusicService";

    private MediaSessionCompat mSession;
    private PlayerAdapter mPlayback;
    private MediaNotificationManager mMediaNotificationManager;
    public MediaSessionCallback mCallback;
    private boolean mServiceInStartedState;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a new MediaSession.
        mSession = new MediaSessionCompat(this, "MusicService");
        mCallback = new MediaSessionCallback();
        mSession.setCallback(mCallback);
        mSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        setSessionToken(mSession.getSessionToken());

        mMediaNotificationManager = new MediaNotificationManager(this);

        mPlayback = new MediaPlayerHolder(this, new MediaPlayerListener());
        Log.d(TAG, "onCreate: MusicService creating MediaSession, and MediaNotificationManager");
    }

    @Override
    public void onDestroy() {
        mMediaNotificationManager.onDestroy();
        mPlayback.stop();
        mSession.release();
        Log.d(TAG, "onDestroy: MediaPlayerHolder stopped, and MediaSession released");
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(MusicLibrary.getRoot(), null);
    }

    @Override
    public void onLoadChildren(
            final String parentMediaId, final Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(MusicLibrary.getMediaItems());
    }

    public MediaSessionCompat getMediaSession() {
        return mSession;
    }

    // MediaSession Callback: Transport Controls -> MediaPlayerHolder
    public class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {

            mSession.setActive(true);
            MediaMetadataCompat metadata = MusicLibrary.getMetadata(MusicService.this, mediaId);
            mSession.setMetadata(metadata);

            mPlayback.loadAndPlayMedia(metadata);

            Log.d(TAG, "onPlayFromMediaId: MediaSession active");
        }

        @Override
        public void onPlay() {
            if (mPlayback.getCurrentMediaId() != null) {
                onPlayFromMediaId(mPlayback.getCurrentMediaId(), null);
            }
        }

        @Override
        public void onPause() {
            mPlayback.pause();
        }

        @Override
        public void onStop() {
            mPlayback.stop();
        }

        @Override
        public void onSkipToNext() {
            onPlayFromMediaId(
                    MusicLibrary.getNextSong(mPlayback.getCurrentMediaId()), null);
        }

        @Override
        public void onSkipToPrevious() {
            onPlayFromMediaId(
                    MusicLibrary.getPreviousSong(mPlayback.getCurrentMediaId()), null);
        }

    }

    // MediaPlayerHolder Callback: MediaPlayerHolder state -> MusicService.
    public class MediaPlayerListener extends PlaybackInfoListener {

        private final ServiceManager mServiceManager;

        MediaPlayerListener() {
            mServiceManager = new ServiceManager();
        }

        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
            mSession.setPlaybackState(state);

            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    mServiceManager.moveServiceToStartedState(state);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    mServiceManager.updateNotificationForPause(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    mServiceManager.moveServiceOutOfStartedState(state);
                    break;
            }
        }

        @Override
        public void onLogUpdated(String formattedMessage) {
            Log.d(TAG, String.format("MPH: %s", formattedMessage));
        }

        class ServiceManager {

            private void moveServiceToStartedState(PlaybackStateCompat state) {
                Notification notification =
                        mMediaNotificationManager.getNotification(
                                mPlayback.getCurrentMedia(), state, getSessionToken());

                if (!mServiceInStartedState) {
                    ContextCompat.startForegroundService(
                            MusicService.this,
                            new Intent(MusicService.this, MusicService.class));
                    mServiceInStartedState = true;
                    generateLog(state, "startForegroundService(true)");
                }

                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
                generateLog(state, "startForeground()");
            }

            private void updateNotificationForPause(PlaybackStateCompat state) {
                stopForeground(false);
                Notification notification =
                        mMediaNotificationManager.getNotification(
                                mPlayback.getCurrentMedia(), state, getSessionToken());
                mMediaNotificationManager.getNotificationManager()
                        .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
                generateLog(state, "stopForeground(false)");
            }

            private void moveServiceOutOfStartedState(PlaybackStateCompat state) {
                stopForeground(true);
                stopSelf();
                mServiceInStartedState = false;
                generateLog(state, "stopForeground(true), stopSelf()");
            }

            private void generateLog(PlaybackStateCompat state, String message) {
                Log.d(TAG,
                      String.format("onStateChanged(%s): %s",
                                    PlaybackInfoListener.stateToString(state.getState()),
                                    message
                      )
                );
            }

        }

    }

}