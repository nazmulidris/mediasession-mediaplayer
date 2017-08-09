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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

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
    public void loadAndPlayMedia(int resourceId) {
        boolean mediaChanged = (resourceId != mResourceId);
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
        updatePlaybackState(PlaybackInfoListener.State.STOPPED);
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
            updatePlaybackState(PlaybackInfoListener.State.PLAYING);
            startUpdatingCallbackWithPosition();
        }
    }

    @Override
    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            updatePlaybackState(PlaybackInfoListener.State.PAUSED);
            logToUI("playbackPause()");
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopUpdatingCallbackWithPosition(true);
        logToUI("MediaPlayer playback completed");
        mPlaybackInfoListener.onPlaybackCompleted();
        updatePlaybackState(PlaybackInfoListener.State.STOPPED);
    }

    private void updatePlaybackState(@PlaybackInfoListener.State int newPlayerState) {
        switch (newPlayerState) {
            case PlaybackInfoListener.State.STOPPED:
                mState = PlaybackStateCompat.STATE_STOPPED;
                break;
            case PlaybackInfoListener.State.INVALID:
                mState = PlaybackStateCompat.STATE_ERROR;
                break;
            case PlaybackInfoListener.State.PAUSED:
                mState = PlaybackStateCompat.STATE_PAUSED;
                break;
            case PlaybackInfoListener.State.PLAYING:
                mState = PlaybackStateCompat.STATE_PLAYING;
                break;
        }
        mPlaybackInfoListener.onStateChanged(newPlayerState);
        PlaybackStateCompat.Builder stateBuilder =
                new PlaybackStateCompat.Builder().setActions(getAvailableActions());
        stateBuilder.setState(
                mState,
                mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition(),
                1.0f,
                SystemClock.elapsedRealtime());
        mPlaybackInfoListener.onPlaybackStatusChanged(stateBuilder.build());
    }

    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions =
                PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        if (isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
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
