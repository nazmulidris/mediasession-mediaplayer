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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

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

    private TextView mTitleTextView;
    private TextView mArtistTextView;
    private ToggleButton mButtonPlay;
    private Button mButtonPrevious;
    private Button mButtonNext;
    private SeekBar mSeekBarAudio;

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
                        mMediaBrowserAdapter.getTransportControls().seekTo(userSelectedPosition);
                        mPlaybackProgressListener.seekTo(userSelectedPosition);
                        Log.d(TAG,
                                String.format("onStopTrackingTouch: seekTo(%d)",
                                        userSelectedPosition));
                    }
                });
    }

    private void initializeUI() {
        mTitleTextView = findViewById(R.id.song_title);
        mArtistTextView = findViewById(R.id.song_artist);
        mButtonPlay = findViewById(R.id.button_play);
        mButtonPrevious = findViewById(R.id.button_previous);
        mButtonNext = findViewById(R.id.button_next);
        mSeekBarAudio = findViewById(R.id.seekbar_audio);

        mButtonPlay.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!mButtonPlay.isChecked()) {
                            mMediaBrowserAdapter.getTransportControls().pause();
                        } else {
                            mMediaBrowserAdapter.getTransportControls().play();
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
            final boolean isPlaying = playbackState != null &&
                    playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;
            mButtonPlay.setChecked(isPlaying);
        }

        // TODO: 8/7/17 Update the UI when new metadata is loaded via the MediaController.
        @Override
        public void onMetadataChanged(MediaMetadataCompat mediaMetadata) {
            if (mediaMetadata == null) {
                return;
            }
            mTitleTextView.setText(
                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            mArtistTextView.setText(
                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
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
            if (mediaMetadata == null) {
                return;
            }

            currentlyLoadedMedia = mediaMetadata;
            stopUpdating();

            final int duration =
                    (int) mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            Log.d(TAG, "Duration set to: " + duration);
            mSeekBarAudio.setMax(duration);
            mSeekBarAudio.setProgress(0);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            // Change the progress reporting UI so that it matches the current state.
            if (playbackState != null) {
                switch (playbackState.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING:
                        startUpdating((int) playbackState.getPosition(), false);
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                        startUpdating((int) playbackState.getPosition(), true);
                        break;
                    case PlaybackStateCompat.STATE_STOPPED:
                        stopUpdating();
                        mSeekBarAudio.setProgress(0);
                        break;
                }
            } else {
                // State is empty.
                stopUpdating();
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
                Log.d(TAG,
                        "PlaybackProgress.task: skipping setProgress, since user is moving scrubber");
            }
        }

        private void stopUpdating() {
            mPaused = false;
            if (mExecutor != null) {
                mExecutor.shutdownNow();
                mExecutor = null;
            }
        }

        private void startUpdating(int startPosition, boolean isPaused) {
            mPaused = isPaused;
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
                mPlaybackTime = startPosition;
            }
        }
    }

}