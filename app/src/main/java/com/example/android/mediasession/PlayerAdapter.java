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

package com.example.android.mediasession;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public interface PlayerAdapter {

    void loadAndPlayMedia(MediaMetadataCompat metadata);

    MediaMetadataCompat getCurrentMedia();

    String getCurrentMediaId();

    @PlaybackStateCompat.State
    int getCurrentState();

    void loadAndPlayMedia(int resourceId);

    void stop();

    boolean isPlaying();

    void pause();

    void initializeProgressCallback();

    void seekTo(int position);
}
