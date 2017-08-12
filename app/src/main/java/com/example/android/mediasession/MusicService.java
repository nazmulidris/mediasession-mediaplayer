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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.List;

public class MusicService extends MediaBrowserServiceCompat {

    private static final String TAG = "MS_MusicService";

    private MediaSessionCompat mSession;
    private MediaPlayerHolder mPlayback;
    private MediaNotificationManager mMediaNotificationManager;
    public MediaSessionCallback mCallback;
    protected MediaPlayerHolderListener mMediaPlayerHolderListener;
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

        mMediaPlayerHolderListener = new MediaPlayerHolderListener();
        mPlayback = new MediaPlayerHolder(this, mMediaPlayerHolderListener);
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
    public class MediaPlayerHolderListener extends PlaybackInfoListener {

        @Override
        public void onStateChanged(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
            mSession.setPlaybackState(state);

            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    if (!mServiceInStartedState) {
                        ContextCompat.startForegroundService(
                                MusicService.this,
                                new Intent(MusicService.this, MusicService.class));
                        mServiceInStartedState = true;
                    }
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    stopForeground(false);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    stopSelf();
                    mServiceInStartedState = false;
            }

            // Update the notification.
            mMediaNotificationManager.update(
                    mPlayback.getCurrentMedia(), state, getSessionToken());

        }

        @Override
        public void onLogUpdated(String formattedMessage) {
            Log.d(TAG, String.format("log: %s", formattedMessage));
        }

    }
}