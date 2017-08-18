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

package com.example.android.mediasession.ui;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.mediasession.R;
import com.example.android.mediasession.service.MusicService;
import com.example.android.mediasession.service.PlaybackInfoListener;
import com.example.android.mediasession.service.contentcatalogs.MusicLibrary;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MS_MainActivity";

    private TextView mTextDebug;
    private Button mButtonPlay;
    private Button mButtonPause;
    private Button mButtonPrevious;
    private Button mButtonNext;
    private Button mButtonStop;
    private SeekBar mSeekbarAudio;
    private ScrollView mScrollContainer;
    private MediaBrowserCompat mMediaBrowser;

    private final MediaBrowserConnectionCallback mMediaBrowserConnectionCallback =
            new MediaBrowserConnectionCallback();
    private final MediaControllerCallback mMediaControllerCallback =
            new MediaControllerCallback();
    private final MediaBrowserSubscriptionCallback mMediaBrowserSubscriptionCallback =
            new MediaBrowserSubscriptionCallback();

    // Metadata and PlaybackState comprise the overall state of the MediaSession, and the app
    // should use just this information to update the UI.
    @Nullable
    private PlaybackStateCompat mCurrentPlaybackState;
    @Nullable
    private MediaMetadataCompat mCurrentMetadata;

    @Nullable
    private MediaControllerCompat mMediaController;
    private boolean mClientIsConnectedToService = false;

    private List<MediaBrowserCompat.MediaItem> mMediaItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMediaBrowser =
                new MediaBrowserCompat(
                        this,
                        new ComponentName(this, MusicService.class),
                        mMediaBrowserConnectionCallback,
                        null);
        initializeUI();
    }

    private void initializeUI() {
        mTextDebug = findViewById(R.id.text_debug);
        mButtonStop = findViewById(R.id.button_stop);
        mButtonPlay = findViewById(R.id.button_play);
        mButtonPause = findViewById(R.id.button_pause);
        mButtonPrevious = findViewById(R.id.button_previous);
        mButtonNext = findViewById(R.id.button_next);
        mSeekbarAudio = findViewById(R.id.seekbar_audio);
        mScrollContainer = findViewById(R.id.scroll_container);

        mButtonPause.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getTransportControls().pause();
                    }
                });
        mButtonStop.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getTransportControls().stop();
                    }
                });
        mButtonPlay.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean isMusicLoaded = mCurrentMetadata != null;
                        Log.d(TAG, String.format("onClick: isMusicLoaded: %s", isMusicLoaded));
                        if (!isMusicLoaded) {
                            String mediaId = MusicLibrary.getMediaItems().get(0).getMediaId();
                            getTransportControls().playFromMediaId(mediaId, null);
                        } else {
                            getTransportControls().play();
                        }
                    }
                });
        mButtonPrevious.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getTransportControls().skipToPrevious();
                    }
                });
        mButtonNext.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getTransportControls().skipToNext();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaBrowser.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowser.disconnect();
    }

    // Receives callbacks from the MediaBrowser when it has successfully connected to the
    // MediaBrowserService (MusicService).
    public class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        // Happens onStart().
        @Override
        public void onConnected() {
            mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mMediaBrowserSubscriptionCallback);
            try {
                mMediaController = new MediaControllerCompat(MainActivity.this,
                                                             mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallback);
                mClientIsConnectedToService = true;
                Log.d(TAG, "onConnected: ");
            } catch (RemoteException e) {
                Log.d(TAG, String.format("onConnected: Problem: %s", e.toString()));
                throw new RuntimeException(e);
            }
        }

        // Does not happen onStop(). In fact, does not get called.
        @Override
        public void onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended: ");
            cleanupResources();
        }

        // Does not happen onStop(). In fact, does not get called.
        @Override
        public void onConnectionFailed() {
            Log.d(TAG, "onConnectionFailed: ");
            cleanupResources();
        }

        public void cleanupResources() {
            mMediaController.unregisterCallback(mMediaControllerCallback);
            mMediaController = null;
            mClientIsConnectedToService = false;
            Log.d(TAG, "cleanupResources: Releasing MediaController");
        }

        // Happens when the MusicService dies.
        public void onServiceDestroyed() {
            // It is possible for the MusicService to die while the UI is running, and to handle
            // this case, mCurrentMetadata has to be set to null here. When the Play button is
            // pressed the media will be loaded. Basically reset the state of the Activity to
            // what it is when it is first created.
            mCurrentMetadata = null;
            mCurrentPlaybackState = null;
            Log.d(TAG, "onServiceDestroyed: MusicService has died!!!");
        }
    }

    // Receives callbacks from the MediaBrowser when the MediaBrowserService has loaded new media
    // that is ready for playback.
    public class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(
                String parentId, List<MediaBrowserCompat.MediaItem> children) {
            onMediaLoaded(children);
        }

        private void onMediaLoaded(List<MediaBrowserCompat.MediaItem> media) {
            mMediaItemList = media;
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < media.size(); i++) {
                MediaBrowserCompat.MediaItem mediaItem = media.get(i);
                stringBuffer
                        .append("[").append(i).append("] ")
                        .append(mediaItem.getDescription().toString())
                        .append("\n");
            }
            logToUI(String.format("onMediaLoaded:\n%s", stringBuffer));
        }
    }

    // Receives callbacks from the MediaController and updates the UI state,
    // i.e.: Which is the current item, whether it's playing or paused, etc.
    public class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mCurrentMetadata = metadata;
            updateUIOnMetadataChange();
        }

        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackStateCompat state) {
            mCurrentPlaybackState = state;
            updateUIOnPlaybackStateChange();
        }

        // This happens when the MusicService is killed.
        @Override
        public void onSessionDestroyed() {
            mMediaBrowserConnectionCallback.onServiceDestroyed();
            onPlaybackStateChanged(null);
        }

    }

    // Methods that make UI updates.
    // TODO: 8/7/17 Update the play/pause button when state changes.
    private void updateUIOnPlaybackStateChange() {
        logToUI(String.format("Playback State Updated: %s",
                              PlaybackInfoListener.stateToString(mCurrentPlaybackState)));
/*
            if (state == null
                || state.getState() == PlaybackState.STATE_PAUSED
                || state.getState() == PlaybackState.STATE_STOPPED) {
                mPlayPause.setImageDrawable(
                        ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_36dp));
            } else {
                mPlayPause.setImageDrawable(
                        ContextCompat.getDrawable(this, R.drawable.ic_pause_black_36dp));
            }
            mPlaybackControls.setVisibility(state == null ? View.GONE : View.VISIBLE);
*/
    }

    // TODO: 8/7/17 Update the UI when new metadata is loaded via the MediaController.
    private void updateUIOnMetadataChange() {
        final String metadataString = mCurrentMetadata == null
                                      ? "null"
                                      : mCurrentMetadata.getDescription().toString();
        logToUI(String.format("Metadata updated: %s", metadataString));
/*
        mTitle.setText(mCurrentMetadata == null ? "" : mCurrentMetadata.getDescription().getTitle());
        mSubtitle.setText(mCurrentMetadata == null ? "" : mCurrentMetadata.getDescription().getSubtitle());
        mAlbumArt.setImageBitmap(
                mCurrentMetadata == null
                ? null
                : MusicLibrary.getAlbumBitmap(this, mCurrentMetadata.getDescription().getMediaId()));
*/
    }

    public void logToUI(String message) {
        if (mTextDebug != null) {
            mTextDebug.append(message);
            mTextDebug.append("\n");
            // Moves the scrollContainer focus to the end.
            mScrollContainer.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            mScrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
        }
    }

    // Helper methods.
    private MediaControllerCompat.TransportControls getTransportControls() {
        Log.d(TAG, String.format("getTransportControls: MC is null:%s, Client is connected:%s",
                                 mMediaController == null,
                                 mClientIsConnectedToService));
        if (mMediaController == null) {
            Log.d(TAG, "getTransportControls: MediaController is null!");
            throw new IllegalStateException();
        } else {
            Log.d(TAG, "getTransportControls: MediaController is not null!");
            return mMediaController.getTransportControls();
        }
    }

}