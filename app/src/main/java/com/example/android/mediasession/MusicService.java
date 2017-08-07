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

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.List;

public class MusicService extends MediaBrowserServiceCompat {

    private MediaSessionCompat mSession;
    private MediaPlayerHolder mPlayback;
    public MediaSessionCallback mCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        // Start a new MediaSession.
        mSession = new MediaSessionCompat(this, "MusicService");
        mCallback = new MediaSessionCallback();
        mSession.setCallback(mCallback);
        mSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        setSessionToken(mSession.getSessionToken());

        final MediaNotificationManager mediaNotificationManager =
                new MediaNotificationManager(this);

        mPlayback = new MediaPlayerHolder(this);
        mPlayback.setPlaybackInfoListener(new PlaybackInfoListener() {
            @Override
            public void onLogUpdated(String formattedMessage) {
            }

            @Override
            public void onDurationChanged(int duration) {
            }

            @Override
            public void onPositionChanged(int position) {
            }

            @Override
            public void onStateChanged(@State int state) {
            }

            @Override
            public void onPlaybackCompleted() {
            }

            @Override
            public void onPlaybackStatusChanged(PlaybackStateCompat state) {
                mSession.setPlaybackState(state);
                mediaNotificationManager.update(
                        mPlayback.getCurrentMedia(), state, getSessionToken());
            }
        });
    }

    @Override
    public void onDestroy() {
        mPlayback.release();
        mSession.release();
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

    public class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mSession.setActive(true);
            MediaMetadataCompat metadata =
                    MusicLibrary.getMetadata(MusicService.this, mediaId);
            mSession.setMetadata(metadata);
            mPlayback.loadAndPlayMedia(metadata);
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
            stopSelf();
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

}
