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

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.mediasession.R;
import com.example.android.mediasession.client.MediaBrowserAdapter;
import com.example.android.mediasession.client.MediaBrowserChangeListener;
import com.example.android.mediasession.service.PlaybackInfoListener;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MS_MainActivity";

    private TextView mTextDebug;
    private Button mButtonPlay;
    private Button mButtonPause;
    private Button mButtonPrevious;
    private Button mButtonNext;
    private Button mButtonStop;
    private SeekBar mSeekBarAudio;
    private ScrollView mScrollContainer;

    private MediaBrowserAdapter mMediaBrowserAdapter;
    private MediaBrowserListener mMediaBrowserListener;
    private PlaybackProgress mPlaybackProgressListener;

    // This is used to synchronize the PlaybackProgress and SeekBar so when the user is moving
    // the scrubber on the SeekBar, it doesn't get updated automatically.
    private boolean mUserIsSeeking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        mMediaBrowserAdapter = new MediaBrowserAdapter(this);
        mMediaBrowserListener = new MediaBrowserListener();
        mMediaBrowserAdapter.addListener(mMediaBrowserListener);
        mPlaybackProgressListener = new PlaybackProgress();
        mMediaBrowserAdapter.addListener(mPlaybackProgressListener);
        respondToSeekBarDragByUser();
    }

    private void respondToSeekBarDragByUser() {
        mSeekBarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mMediaBrowserAdapter.getTransportControls().seekTo(
                                Long.valueOf(userSelectedPosition));
                        mPlaybackProgressListener.seekTo(userSelectedPosition);
                        Log.d(TAG,
                              String.format("onStopTrackingTouch: seekTo(%d)",
                                            userSelectedPosition));
                    }
                });
    }

    private void initializeUI() {
        mTextDebug = (TextView) findViewById(R.id.text_debug);
        mButtonStop = (Button) findViewById(R.id.button_stop);
        mButtonPlay = (Button) findViewById(R.id.button_play);
        mButtonPause = (Button) findViewById(R.id.button_pause);
        mButtonPrevious = (Button) findViewById(R.id.button_previous);
        mButtonNext = (Button) findViewById(R.id.button_next);
        mSeekBarAudio = (SeekBar) findViewById(R.id.seekbar_audio);
        mScrollContainer = (ScrollView) findViewById(R.id.scroll_container);

        mButtonPause.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mMediaBrowserAdapter.getTransportControls().pause();
                    }
                });
        mButtonStop.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mMediaBrowserAdapter.getTransportControls().stop();
                    }
                });
        mButtonPlay.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MediaMetadataCompat loadedMetadata =
                                mMediaBrowserAdapter.getState().getMediaMetadata();
                        boolean isMusicLoaded = loadedMetadata != null;
                        if (!isMusicLoaded) {
                            String mediaId = mMediaBrowserAdapter
                                    .getMediaItemList()
                                    .get(0)
                                    .getMediaId();
                            mMediaBrowserAdapter
                                    .getTransportControls().playFromMediaId(mediaId, null);
                            Log.d(TAG, "onClick: play newly-loaded media");
                        } else {
                            mMediaBrowserAdapter.getTransportControls().play();
                            Log.d(TAG, "onClick: Play pre-loaded media");
                        }
                    }
                });
        mButtonPrevious.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mMediaBrowserAdapter.getTransportControls().skipToPrevious();
                    }
                });
        mButtonNext.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mMediaBrowserAdapter.getTransportControls().skipToNext();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaBrowserAdapter.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowserAdapter.onStop();
    }

    public class MediaBrowserListener extends MediaBrowserChangeListener {

        // TODO: 8/7/17 Update the play/pause button when state changes.
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            StringBuffer stateToString = PlaybackInfoListener.stateToString(playbackState);
            long position = getPosition(playbackState);
            logToUI(String.format("\nPlayback State Updated: %s", stateToString));
            logToUI(String.format("position: %d", position));
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
        @Override
        public void onMetadataChanged(MediaMetadataCompat mediaMetadata) {
            String metadataString = mediaMetadata == null
                                    ? "null"
                                    : mediaMetadata.getDescription().toString();
            long duration = getDuration(mediaMetadata);
            logToUI(String.format("\nMetadata updated: %s", metadataString));
            logToUI(String.format("duration: %d", duration));
/*
        mTitle.setText(mCurrentMetadata == null ? "" : mCurrentMetadata.getDescription().getTitle());
        mSubtitle.setText(mCurrentMetadata == null ? "" : mCurrentMetadata.getDescription().getSubtitle());
        mAlbumArt.setImageBitmap(
                mCurrentMetadata == null
                ? null
                : MusicLibrary.getAlbumBitmap(this, mCurrentMetadata.getDescription().getMediaId()));
*/
        }

        @Override
        public void onMediaLoaded(List<MediaBrowserCompat.MediaItem> mediaItemList) {
            if (mediaItemList != null) {
                StringBuffer stringBuffer = new StringBuffer();
                for (int i = 0; i < mediaItemList.size(); i++) {
                    MediaBrowserCompat.MediaItem mediaItem = mediaItemList.get(i);
                    stringBuffer
                            .append("[").append(i).append("] ")
                            .append(mediaItem.getDescription().toString())
                            .append("\n");
                }
                logToUI(String.format("\nonMediaLoaded:\n%s", stringBuffer));
            } else {
                logToUI(String.format("\nonMediaLoaded:\n%s", "NULL"));
            }
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

    public class PlaybackProgress extends MediaBrowserChangeListener {

        public static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000;

        private ScheduledExecutorService mExecutor;

        private boolean mPaused;
        private long mTimePlayPressed;
        private long mPlaybackTime;
        private MediaMetadataCompat currentlyLoadedMedia;

        @Override
        public void onMetadataChanged(MediaMetadataCompat mediaMetadata) {
            if (currentlyLoadedMedia == null) {
                currentlyLoadedMedia = mediaMetadata;
            }
            stopUpdating();
            int duration = getDuration(mediaMetadata).intValue();
            mSeekBarAudio.setMax(duration);
            mSeekBarAudio.setProgress(0);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            if (playbackState != null) {
                switch (playbackState.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING:
                        mPaused = false;
                        startUpdating();
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                        mPaused = true;
                        break;
                    case PlaybackStateCompat.STATE_STOPPED:
                        mPaused = false;
                        stopUpdating();
                        mSeekBarAudio.setProgress(getPosition(playbackState).intValue());
                        break;
                }
            } else {
                Log.d(TAG, "onPlaybackStateChanged: playback state is NULL");
            }
        }

        public void seekTo(int position) {
            mPlaybackTime = position;
            mTimePlayPressed = System.currentTimeMillis();
        }

        private void task() {
            long currentTime = System.currentTimeMillis();
            if (!mPaused) {
                mPlaybackTime += currentTime - mTimePlayPressed;
            }
            mTimePlayPressed = currentTime;
            if (!mUserIsSeeking) {
                mSeekBarAudio.setProgress(Long.valueOf(mPlaybackTime).intValue());
            } else {
                Log.d(TAG, "task: skipping setProgress, since user is moving scrubber");
            }
        }

        private void stopUpdating() {
            if (mExecutor != null) {
                mExecutor.shutdownNow();
                mExecutor = null;
            }
        }

        private void startUpdating() {
            if (mExecutor == null) {
                mExecutor = Executors.newSingleThreadScheduledExecutor();
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        task();
                    }
                };
                mExecutor.scheduleAtFixedRate(
                        task, 0, PLAYBACK_POSITION_REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                mTimePlayPressed = System.currentTimeMillis();
                mPlaybackTime = 0;
            }
        }
    }

}