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

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

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

    private PlaybackStateCompat mCurrentPlaybackState;
    private int mCurrentState;
    private MediaMetadataCompat mCurrentMetadata;
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
        mTextDebug = (TextView) findViewById(R.id.text_debug);
        mButtonStop = (Button) findViewById(R.id.button_stop);
        mButtonPlay = (Button) findViewById(R.id.button_play);
        mButtonPause = (Button) findViewById(R.id.button_pause);
        mButtonPrevious = (Button) findViewById(R.id.button_previous);
        mButtonNext = (Button) findViewById(R.id.button_next);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);
        mScrollContainer = (ScrollView) findViewById(R.id.scroll_container);

        mButtonPause.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final boolean canPause = mCurrentState == PlaybackStateCompat.STATE_PLAYING;
                        if (canPause) {
                            MediaControllerCompat.getMediaController(MainActivity.this)
                                    .getTransportControls()
                                    .pause();
                        }
                    }
                });
        mButtonStop.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final boolean canPause = mCurrentState == PlaybackStateCompat.STATE_PLAYING;
                        if (canPause) {
                            MediaControllerCompat.getMediaController(MainActivity.this)
                                    .getTransportControls()
                                    .stop();
                        }
                    }
                });
        mButtonPlay.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean canPlay = mCurrentState == PlaybackStateCompat.STATE_PAUSED ||
                                          mCurrentState == PlaybackStateCompat.STATE_STOPPED ||
                                          mCurrentState == PlaybackStateCompat.STATE_NONE;
                        boolean isMusicLoaded = mCurrentMetadata != null;
                        if (canPlay) {
                            if (!isMusicLoaded) {
                                mCurrentMetadata =
                                        MusicLibrary.getMetadata(
                                                MainActivity.this,
                                                MusicLibrary.getMediaItems().get(0).getMediaId());
                                updateMetadata(mCurrentMetadata);
                            }
                            MediaControllerCompat.getMediaController(MainActivity.this)
                                    .getTransportControls()
                                    .playFromMediaId(
                                            mCurrentMetadata.getDescription().getMediaId(), null);
                        }
                    }
                });
        mButtonPrevious.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean canSkip = mCurrentState == PlaybackStateCompat.STATE_PAUSED ||
                                          mCurrentState == PlaybackStateCompat.STATE_PLAYING ||
                                          mCurrentState == PlaybackStateCompat.STATE_STOPPED;
                        if (canSkip) {
                            MediaControllerCompat.getMediaController(MainActivity.this)
                                    .getTransportControls()
                                    .skipToPrevious();
                        }
                    }
                });
        mButtonNext.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean canSkip = mCurrentState == PlaybackStateCompat.STATE_PAUSED ||
                                          mCurrentState == PlaybackStateCompat.STATE_PLAYING ||
                                          mCurrentState == PlaybackStateCompat.STATE_STOPPED;
                        if (canSkip) {
                            MediaControllerCompat.getMediaController(MainActivity.this)
                                    .getTransportControls()
                                    .skipToNext();
                        }
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

        @Override
        public void onConnected() {
            mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mMediaBrowserSubscriptionCallback);
            try {
                MediaControllerCompat mediaController =
                        new MediaControllerCompat(
                                MainActivity.this, mMediaBrowser.getSessionToken());
                updatePlaybackState(mediaController.getPlaybackState());
                updateMetadata(mediaController.getMetadata());
                mediaController.registerCallback(mMediaControllerCallback);
                MediaControllerCompat.setMediaController(MainActivity.this, mediaController);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onConnectionSuspended() {
            MediaControllerCompat mediaController = MediaControllerCompat
                    .getMediaController(MainActivity.this);
            if (mediaController != null) {
                mediaController.unregisterCallback(mMediaControllerCallback);
                MediaControllerCompat.setMediaController(MainActivity.this, null);
            }
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
            updateMetadata(metadata);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            updatePlaybackState(state);
        }

        @Override
        public void onSessionDestroyed() {
            updatePlaybackState(null);
        }

    }

    // Methods that make UI updates.
    // TODO: 8/7/17 Update the play/pause button when state changes.
    private void updatePlaybackState(@Nullable PlaybackStateCompat state) {
        mCurrentPlaybackState = state;
        if (state == null) {
            mCurrentState = PlaybackStateCompat.STATE_NONE;
        } else {
            mCurrentState = state.getState();
        }
        logToUI(String.format("updatePlaybackState(%s)",
                              state == null ? "null" : state.getPlaybackState().toString()));
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
    private void updateMetadata(@Nullable MediaMetadataCompat metadata) {
        mCurrentMetadata = metadata;
        logToUI(String.format("updateMetadata(%s)",
                              metadata == null ? "null" : metadata.getDescription().toString()));
/*
            mTitle.setText(metadata == null ? "" : metadata.getDescription().getTitle());
            mSubtitle.setText(metadata == null ? "" : metadata.getDescription().getSubtitle());
            mAlbumArt.setImageBitmap(
                    metadata == null
                    ? null
                    : MusicLibrary.getAlbumBitmap(
                            this, metadata.getDescription().getMediaId()));
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

}