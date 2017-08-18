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

package com.example.android.mediasession.service;

import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;

public abstract class PlaybackInfoListener {

    public static StringBuffer stateToString(@Nullable PlaybackStateCompat state) {
        if (state == null) {
            return new StringBuffer("UNKNOWN");
        } else {
            return stateToString(state.getState());
        }
    }

    public static StringBuffer stateToString(@PlaybackStateCompat.State int state) {
        StringBuffer stateString = new StringBuffer();
        switch (state) {
            case PlaybackStateCompat.STATE_PLAYING:
                stateString.append("PLAYING");
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                stateString.append("STOPPED");
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                stateString.append("PAUSED");
                break;
            case PlaybackStateCompat.STATE_ERROR:
                stateString.append("ERROR");
                break;
            case PlaybackStateCompat.STATE_NONE:
                stateString.append("NONE");
                break;
            default:
                stateString.append("UNKNOWN");
        }
        return stateString;
    }

    public abstract void onLogUpdated(String formattedMessage);

    public void onDurationChanged(int duration) {
    }

    public void onPositionChanged(int position) {
    }

    public void onPlaybackCompleted() {
    }

    public abstract void onPlaybackStateChange(PlaybackStateCompat state);

}