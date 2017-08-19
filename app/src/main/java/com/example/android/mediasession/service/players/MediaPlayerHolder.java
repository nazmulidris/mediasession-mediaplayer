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

package com.example.android.mediasession.service.players;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.example.android.mediasession.service.PlaybackInfoListener;
import com.example.android.mediasession.service.PlayerAdapter;
import com.example.android.mediasession.service.contentcatalogs.MusicLibrary;
import com.example.android.mediasession.ui.MainActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Exposes the functionality of the {@link MediaPlayer} and implements the {@link PlayerAdapter}
 * so that {@link MainActivity} can control music playback.
 */
public final class MediaPlayerHolder implements PlayerAdapter, MediaPlayer.OnCompletionListener {

    public static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000;

    private final Context mContext;
    private MediaPlayer mMediaPlayer;
    private int mResourceId;
    private PlaybackInfoListener mPlaybackInfoListener;
    private ScheduledExecutorService mExecutor;
    private Runnable mSeekbarPositionUpdateTask;
    private MediaMetadataCompat mCurrentMedia;
    private int mState;
    private boolean mCurrentMediaPlayedToCompletion;

    public MediaPlayerHolder(Context context, PlaybackInfoListener listener) {
        mContext = context.getApplicationContext();
        mPlaybackInfoListener = listener;
    }

    /**
     * Once the {@link MediaPlayer} is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the {@link MainActivity} the {@link MediaPlayer} is
     * released. Then in the onStart() of the {@link MainActivity} a new {@link MediaPlayer}
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    private void initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(this);
            logToUI("mMediaPlayer = new MediaPlayer()");
        }
    }

    // Implements PlaybackControl.
    @Override
    public void loadAndPlayMedia(MediaMetadataCompat metadata) {
        mCurrentMedia = metadata;
        String mediaId = metadata.getDescription().getMediaId();
        loadAndPlayMedia(MusicLibrary.getMusicRes(mediaId));
    }

    @Override
    public MediaMetadataCompat getCurrentMedia() {
        return mCurrentMedia;
    }

    @Override
    public String getCurrentMediaId() {
        return mCurrentMedia == null ? null : mCurrentMedia.getDescription().getMediaId();
    }

    @Override
    public int getCurrentState() {
        return mState;
    }

    @Override
    public void loadAndPlayMedia(int resourceId) {
        boolean mediaChanged = (resourceId != mResourceId);
        if (mCurrentMediaPlayedToCompletion) {
            // Last audio file was played to completion, the resourceId hasn't changed, but the
            // player was released, so force a reload of the media file for playback.
            mediaChanged = true;
            mCurrentMediaPlayedToCompletion = false;
        }
        if (!mediaChanged) {
            if (isPlaying()) {
                return;
            } else {
                play();
                return;
            }
        } else {
            release();
        }

        mResourceId = resourceId;

        initializeMediaPlayer();

        AssetFileDescriptor assetFileDescriptor =
                mContext.getResources().openRawResourceFd(mResourceId);
        try {
            logToUI("load() {1. setDataSource}");
            mMediaPlayer.setDataSource(assetFileDescriptor);
        } catch (Exception e) {
            logToUI(e.toString());
        }

        try {
            logToUI("load() {2. prepare}");
            mMediaPlayer.prepare();
        } catch (Exception e) {
            logToUI(e.toString());
        }

        initializeProgressCallback();
        logToUI("initializeProgressCallback()");

        play();
    }

    @Override
    public void stop() {
        // Regardless of whether or not the MediaPlayer has been created / started, the state must
        // be updated, so that MediaNotificationManager can take down the notification.
        reducer(PlaybackStateCompat.STATE_STOPPED);
        release();
        logToUI("stop() and updatePlaybackState(STOPPED)");
    }

    private void release() {
        if (mMediaPlayer != null) {
            stopUpdatingCallbackWithPosition(true);
            mMediaPlayer.release();
            mMediaPlayer = null;
            logToUI("release() and mMediaPlayer = null");
        }
    }

    @Override
    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    public void play() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            logToUI(String.format("playbackStart() %s",
                                  mContext.getResources().getResourceEntryName(mResourceId)));
            mMediaPlayer.start();
            reducer(PlaybackStateCompat.STATE_PLAYING);
            startUpdatingCallbackWithPosition();
        }
    }

    @Override
    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            reducer(PlaybackStateCompat.STATE_PAUSED);
            logToUI("playbackPause()");
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopUpdatingCallbackWithPosition(true);
        logToUI("MediaPlayer playback completed");
        mPlaybackInfoListener.onPlaybackCompleted();
        reducer(PlaybackStateCompat.STATE_STOPPED);
    }

    // This is the main reducer for the player state machine.
    private void reducer(@PlaybackStateCompat.State int newPlayerState) {
        mState = newPlayerState;

        // Whether playback goes to completion, or whether it is stopped, the
        // mCurrentMediaPlayedToCompletion is set to true.
        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            mCurrentMediaPlayedToCompletion = true;
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(getAvailableActions());
        stateBuilder.setState(mState,
                              mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition(),
                              1.0f,
                              SystemClock.elapsedRealtime());
        mPlaybackInfoListener.onPlaybackStateChange(stateBuilder.build());
    }

    /**
     * Set the current capabilities available on this session. Note: If a capability is not
     * listed in the bitmask of capabilities then the MediaSession will not handle it. For
     * example, if you don't want ACTION_STOP to be handled by the MediaSession, then don't
     * included it in the bitmask that's returned.
     */
    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions;
        switch (mState) {
            case PlaybackStateCompat.STATE_STOPPED:
                actions = PlaybackStateCompat.ACTION_PLAY
                          | PlaybackStateCompat.ACTION_PAUSE
                          | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                          | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                          | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                          | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                actions = PlaybackStateCompat.ACTION_STOP
                          | PlaybackStateCompat.ACTION_PAUSE
                          | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                          | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                          | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                          | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                actions = PlaybackStateCompat.ACTION_PLAY
                          | PlaybackStateCompat.ACTION_STOP
                          | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                          | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                          | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                          | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
                break;
            default:
                actions = PlaybackStateCompat.ACTION_PLAY
                          | PlaybackStateCompat.ACTION_PLAY_PAUSE
                          | PlaybackStateCompat.ACTION_STOP
                          | PlaybackStateCompat.ACTION_PAUSE
                          | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                          | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                          | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                          | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }
        return actions;
    }

    @Override
    public void seekTo(int position) {
        if (mMediaPlayer != null) {
            logToUI(String.format("seekTo() %d ms", position));
            mMediaPlayer.seekTo(position);
        }
    }

    /**
     * Syncs the mMediaPlayer position with mPlaybackProgressCallback via recurring task.
     */
    private void startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        if (mSeekbarPositionUpdateTask == null) {
            mSeekbarPositionUpdateTask = new Runnable() {
                @Override
                public void run() {
                    updateProgressCallbackTask();
                }
            };
        }
        mExecutor.scheduleAtFixedRate(
                mSeekbarPositionUpdateTask,
                0,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    // Reports media playback position to mPlaybackProgressCallback.
    private void stopUpdatingCallbackWithPosition(boolean resetUIPlaybackPosition) {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
            mSeekbarPositionUpdateTask = null;
            if (resetUIPlaybackPosition && mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onPositionChanged(0);
            }
        }
    }

    private void updateProgressCallbackTask() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int currentPosition = mMediaPlayer.getCurrentPosition();
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onPositionChanged(currentPosition);
            }
        }
    }

    @Override
    public void initializeProgressCallback() {
        final int duration = mMediaPlayer.getDuration();
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener.onDurationChanged(duration);
            mPlaybackInfoListener.onPositionChanged(0);
            logToUI(String.format("firing setPlaybackDuration(%d sec)",
                                  TimeUnit.MILLISECONDS.toSeconds(duration)));
            logToUI("firing setPlaybackPosition(0)");
        }
    }

    private void logToUI(String message) {
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener.onLogUpdated(message);
        }
    }

}
