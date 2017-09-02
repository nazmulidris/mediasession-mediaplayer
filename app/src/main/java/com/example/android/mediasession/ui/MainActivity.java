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
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.mediasession.R;
import com.example.android.mediasession.client.MediaBrowserAdapter;
import com.example.android.mediasession.client.MediaBrowserChangeListener;
import com.example.android.mediasession.service.contentcatalogs.MusicLibrary;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MS_MainActivity";

    private ImageView mAlbumArt;
    private TextView mTitleTextView;
    private TextView mArtistTextView;
    private ImageView mMediaControlsImage;
    private Button mButtonPlay;
    private Button mButtonPrevious;
    private Button mButtonNext;
    private MediaSeekBar mSeekBarAudio;

    private MediaBrowserAdapter mMediaBrowserAdapter;
    private MediaBrowserListener mMediaBrowserListener;

    // This is used to synchronize the PlaybackProgress and SeekBar so when the user is moving
    // the scrubber on the SeekBar, it doesn't get updated automatically.
    private boolean mUserIsSeeking;

    private boolean mIsPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        mMediaBrowserAdapter = new MediaBrowserAdapter(this);
        mMediaBrowserListener = new MediaBrowserListener();
        mMediaBrowserAdapter.addListener(mMediaBrowserListener);
        respondToSeekBarDragByUser();
    }

    private void respondToSeekBarDragByUser() {
        mSeekBarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
//                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                        if (fromUser) {
//                            userSelectedPosition = progress;
//                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
//                        mUserIsSeeking = false;
//                        mMediaBrowserAdapter.getTransportControls().seekTo(userSelectedPosition);
//                        mPlaybackProgressListener.seekTo(userSelectedPosition);
//                        Log.d(TAG,
//                                String.format("onStopTrackingTouch: seekTo(%d)",
//                                        userSelectedPosition));
                    }
                });
    }

    private void initializeUI() {
        mTitleTextView = findViewById(R.id.song_title);
        mArtistTextView = findViewById(R.id.song_artist);
        mAlbumArt = findViewById(R.id.album_art);
        mMediaControlsImage = findViewById(R.id.media_controls);
        mButtonPlay = findViewById(R.id.button_play);
        mButtonPrevious = findViewById(R.id.button_previous);
        mButtonNext = findViewById(R.id.button_next);
        mSeekBarAudio = findViewById(R.id.seekbar_audio);

        mButtonPlay.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mIsPlaying) {
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
        mSeekBarAudio.disconnectController();
        mMediaBrowserAdapter.onStop();
    }

    public class MediaBrowserListener extends MediaBrowserChangeListener {

        @Override
        public void onConnected(@Nullable MediaControllerCompat mediaController) {
            super.onConnected(mediaController);
            mSeekBarAudio.setMediaController(mediaController);
        }

        // TODO: 8/7/17 Update the play/pause button when state changes.
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            mIsPlaying = playbackState != null &&
                    playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;
            mMediaControlsImage.setPressed(mIsPlaying);
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
            mAlbumArt.setImageBitmap(MusicLibrary.getAlbumBitmap(
                    MainActivity.this,
                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)));
        }
    }
}