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

import android.support.annotation.IntDef;
import android.support.v4.media.session.PlaybackStateCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface PlaybackInfoListener {

    @IntDef({State.INVALID, State.PLAYING, State.PAUSED, State.COMPLETED})
    @Retention(RetentionPolicy.SOURCE)
    @interface State {

        int INVALID = -1;
        int PLAYING = 0;
        int PAUSED = 1;
        int COMPLETED = 2;
    }

    void onLogUpdated(String formattedMessage);

    void onDurationChanged(int duration);

    void onPositionChanged(int position);

    void onStateChanged(@State int state);

    void onPlaybackCompleted();

    void onPlaybackStatusChanged(PlaybackStateCompat state);

}