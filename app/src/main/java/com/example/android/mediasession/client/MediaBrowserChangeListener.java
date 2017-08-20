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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.List;

public abstract class MediaBrowserChangeListener {

    public void onMediaLoaded(@Nullable List<MediaBrowserCompat.MediaItem> mediaItemList) {
    }

    public void onMetadataChanged(@Nullable MediaMetadataCompat mediaMetadata) {
    }

    public void onPlaybackStateChanged(@Nullable PlaybackStateCompat playbackState) {
    }

    public void logToUI(String message) {
    }

    /**
     * @return Current playback position in ms.
     */
    @NonNull
    public static Long getPosition(@Nullable PlaybackStateCompat state) {
        if (state != null) {
            return state.getPosition();
        } else {
            return 0L;
        }
    }

    /**
     * @return Duration of media in ms.
     */
    @NonNull
    public static Long getDuration(@Nullable MediaMetadataCompat metadata) {
        if (metadata != null) {
            return metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        } else {
            return 0L;
        }
    }

}
