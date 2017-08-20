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

package com.example.android.mediasession.client;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.example.android.mediasession.service.MusicService;

import java.util.List;

public class MediaSessionClientHolder {

    public static final String TAG = "MS_ClientHolder";

    private final InternalState mState;

    private final Context mContext;
    private final ClientCallback mCallback;

    private final MediaBrowserConnectionCallback mMediaBrowserConnectionCallback =
            new MediaBrowserConnectionCallback();
    private final MediaControllerCallback mMediaControllerCallback =
            new MediaControllerCallback();
    private final MediaBrowserSubscriptionCallback mMediaBrowserSubscriptionCallback =
            new MediaBrowserSubscriptionCallback();

    private MediaBrowserCompat mMediaBrowser;

    private List<MediaBrowserCompat.MediaItem> mMediaItemList;

    @Nullable
    private MediaControllerCompat mMediaController;

    public MediaSessionClientHolder(Context context, ClientCallback callback) {
        mContext = context;
        mCallback = callback;
        mState = new InternalState();
    }

    public InternalState getState() {
        return mState;
    }

    public List<MediaBrowserCompat.MediaItem> getMediaItemList() {
        return mMediaItemList;
    }

    public void onStart() {
        if (mMediaBrowser == null) {
            mMediaBrowser =
                    new MediaBrowserCompat(
                            mContext,
                            new ComponentName(mContext, MusicService.class),
                            mMediaBrowserConnectionCallback,
                            null);
            mMediaBrowser.connect();
        }
        Log.d(TAG, "onStart: Creating MediaBrowser, and connecting");
    }

    public void onStop() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
            mMediaController = null;
        }
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
            mMediaBrowser = null;
        }
        resetState();
        Log.d(TAG, "onStop: Releasing MediaController, Disconnecting from MediaBrowser");
    }

    /**
     * The internal state of the app needs to revert to what it looks like when it started before
     * any connections to the {@link MusicService} happens via the {@link MediaSessionCompat}.
     */
    public void resetState() {
        mState.playbackState = null;
        mState.mediaMetadata = null;
        if (mCallback != null) {
            mCallback.onMediaLoaded(null);
            mCallback.onPlaybackStateChanged(null);
            mCallback.onMediaLoaded(null);
        }
        Log.d(TAG, "resetState: ");
    }

    public MediaControllerCompat.TransportControls getTransportControls() {
        if (mMediaController == null) {
            Log.d(TAG, "getTransportControls: MediaController is null!");
            throw new IllegalStateException();
        } else {
            Log.d(TAG, "getTransportControls: MediaController is not null!");
            return mMediaController.getTransportControls();
        }
    }

    // Receives callbacks from the MediaBrowser when it has successfully connected to the
    // MediaBrowserService (MusicService).
    public class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        // Happens onStart().
        @Override
        public void onConnected() {
            mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mMediaBrowserSubscriptionCallback);
            try {
                mMediaController = new MediaControllerCompat(mContext,
                                                             mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallback);
                Log.d(TAG, "onConnected: Subscribing to media, Creating MediaController");
            } catch (RemoteException e) {
                Log.d(TAG, String.format("onConnected: Problem: %s", e.toString()));
                throw new RuntimeException(e);
            }
        }
    }

    // Receives callbacks from the MediaBrowser when the MediaBrowserService has loaded new media
    // that is ready for playback.
    public class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(String parentId,
                                     List<MediaBrowserCompat.MediaItem> children) {
            onMediaLoaded(children);
        }

        private void onMediaLoaded(List<MediaBrowserCompat.MediaItem> media) {
            mMediaItemList = media;
            if (mCallback != null) {
                mCallback.onMediaLoaded(media);
            }
        }
    }

    // Receives callbacks from the MediaController and updates the UI state,
    // i.e.: Which is the current item, whether it's playing or paused, etc.
    public class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mState.mediaMetadata = metadata;
            if (mCallback != null) {
                mCallback.onMetadataChanged(metadata);
            }
        }

        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackStateCompat state) {
            mState.playbackState = state;
            mCallback.onPlaybackStateChanged(state);
        }

        // This might happen if the MusicService is killed while the Activity is in the
        // foreground and onStart() has been called (but not onStop()).
        @Override
        public void onSessionDestroyed() {
            resetState();
            onPlaybackStateChanged(null);
            Log.d(TAG, "onSessionDestroyed: MusicService is dead!!!");
        }

    }

    // A holder class that contains the internal state.
    public class InternalState {

        public PlaybackStateCompat playbackState;
        public MediaMetadataCompat mediaMetadata;
    }

    public interface ClientCallback {

        void onMediaLoaded(List<MediaBrowserCompat.MediaItem> mediaItemList);

        void onMetadataChanged(MediaMetadataCompat mediaMetadata);

        void onPlaybackStateChanged(PlaybackStateCompat playbackState);

        void logToUI(String message);
    }

}