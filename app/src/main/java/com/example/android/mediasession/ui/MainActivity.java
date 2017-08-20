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
import com.example.android.mediasession.service.PlaybackInfoListener;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements MediaBrowserAdapter.ClientCallback {

    private static final String TAG = "MS_MainActivity";

    private TextView mTextDebug;
    private Button mButtonPlay;
    private Button mButtonPause;
    private Button mButtonPrevious;
    private Button mButtonNext;
    private Button mButtonStop;
    private SeekBar mSeekbarAudio;
    private ScrollView mScrollContainer;

    private MediaBrowserAdapter mMediaBrowserAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        mMediaBrowserAdapter = new MediaBrowserAdapter(this, this);
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
                        boolean isMusicLoaded =
                                mMediaBrowserAdapter.getState().mediaMetadata != null;
                        Log.d(TAG, String.format("onClick: isMusicLoaded: %s", isMusicLoaded));
                        if (!isMusicLoaded) {
                            String mediaId = mMediaBrowserAdapter
                                    .getMediaItemList()
                                    .get(0)
                                    .getMediaId();
                            mMediaBrowserAdapter
                                    .getTransportControls().playFromMediaId(mediaId, null);
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

    // TODO: 8/7/17 Update the play/pause button when state changes.
    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
        logToUI(String.format("Playback State Updated: %s",
                              PlaybackInfoListener.stateToString(playbackState)));
        if (playbackState != null) {
            long currentPosition = playbackState.getPosition();
            logToUI(String.format("position:%d", currentPosition));
        }
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
            logToUI(String.format("onMediaLoaded:\n%s", stringBuffer));
        } else {
            logToUI(String.format("onMediaLoaded:\n%s", "NULL"));
        }
    }

    // TODO: 8/7/17 Update the UI when new metadata is loaded via the MediaController.
    @Override
    public void onMetadataChanged(MediaMetadataCompat mediaMetadata) {
        final String metadataString = mediaMetadata == null
                                      ? "null"
                                      : mediaMetadata.getDescription().toString();
        logToUI(String.format("Metadata updated: %s", metadataString));
        if (mediaMetadata != null) {
            long duration = mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            logToUI(String.format("duration:%d", duration));
        }
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

}