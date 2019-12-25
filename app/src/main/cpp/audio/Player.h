/*
 * Copyright 2018 The Android Open Source Project
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

#ifndef CSAMPLER_PLAYER_H
#define CSAMPLER_PLAYER_H

#include <cstdint>
#include <array>

#include <chrono>
#include <memory>
#include <atomic>

#include <android/asset_manager.h>
#include "AudioProperties.h"
#include "FileDataSource.h"

class Player {

public:
    Player(int theOutputRate);
    bool renderAudio(float *targetData, int32_t numFrames);
    void resetPlayHead() { mReadFrameIndex = startFrameIndex; };
    void setPlaying(bool isPlaying) {
        mIsPlaying = isPlaying;
        resetPlayHead();
    };
    void setLooping(bool isLooping) { mIsLooping = isLooping; };
    void noteMsg(std::shared_ptr<FileDataSource> src, int keynum, float velocity, ADSR adsr,
            float start, float resume, float end, float baseKey);

private:
    std::shared_ptr<FileDataSource> mSource;
    int outputRate;
    float mReadFrameIndex = 0;
    int startFrameIndex = 0;
    int endFrameIndex = 0;
    int resumeFrameIndex = 0;
    float playbackRate = 1;
    float volumeMultiplier = 1;
    int currentEnvIndex = 0;
    ADSR envelope{0, 0, 1, 0};
    std::atomic<bool> mIsPlaying{false};
    std::atomic<bool> mIsLooping{false};

    void renderSilence(float *, int32_t);
    float getAttackDecayFraction();
};

#endif //CSAMPLER_PLAYER_H
