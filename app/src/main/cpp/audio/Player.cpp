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

#include <cmath>
#include "Player.h"
#include "../utils/logging.h"

Player::Player(int theOutputRate) {
    outputRate = theOutputRate;
    envelope = std::make_unique<Envelope>(outputRate);
}

void Player::noteMsg(const std::shared_ptr<FileDataSource> src, int keynum, float velocity,
                     ADSR adsr, float start, float resume, float end, float baseKey) {

    if(velocity > 0) {
        playbackRate = pow(2.0F, (keynum - baseKey) / 12.0F);
        volumeMultiplier = velocity;

        mSource = src;
        int sampleRate = mSource->getProperties().sampleRate;

        envelope->beginAttack(adsr);

        startFrameIndex = (int) round(start * sampleRate);
        endFrameIndex = (int) round(end * sampleRate);
        resumeFrameIndex = (int) round(resume * sampleRate);
        setLooping(resume > 0);
        setPlaying(true);
    } else {
        envelope->beginRelease(adsr);
    }
}

bool Player::renderAudio(float *targetData, int32_t numFrames) {

    if(mSource != nullptr && mIsPlaying) {
        const AudioProperties properties = mSource->getProperties();
        int64_t framesToRenderFromData = numFrames;
        int64_t totalSourceFrames = mSource->getSize() / properties.channelCount;
        const float *data = mSource->getData();
        int64_t actualEndFrameIndex = (endFrameIndex <= 0 ? totalSourceFrames : endFrameIndex);

        float actualPlaybackRate =
                playbackRate * mSource->getProperties().sampleRate / 1.0F / outputRate;
        if(mReadFrameIndex > actualEndFrameIndex) {
            actualPlaybackRate = -actualPlaybackRate;
        }

        for(int i = 0; i < framesToRenderFromData; ++i) {
            float actualVolMultiplier = envelope->getFraction();
            envelope->advance();
            actualVolMultiplier *= actualVolMultiplier;
            actualVolMultiplier *= volumeMultiplier;

            int currentIndex = (int) mReadFrameIndex;
            float frac = mReadFrameIndex - currentIndex;
            if(currentIndex < 1) {
                currentIndex = 1;
                frac = 0;
            } else if(currentIndex >= totalSourceFrames - 2) {
                currentIndex = totalSourceFrames - 3;
                frac = 1;
            }

            for(int j = 0; j < 2; ++j) {
                float a = data[(currentIndex - 1) * properties.channelCount +
                               (j % properties.channelCount)];
                float b = data[currentIndex * properties.channelCount +
                               (j % properties.channelCount)];
                float c = data[(currentIndex + 1) * properties.channelCount +
                               (j % properties.channelCount)];
                float d = data[(currentIndex + 2) * properties.channelCount +
                               (j % properties.channelCount)];
                float cminusb = c - b;
                targetData[(i * 2) + j] += actualVolMultiplier *
                                           (b + frac * (cminusb - 0.1666667F * (1. - frac) * (
                                                   (d - a - 3.0F * cminusb) * frac +
                                                   (d + 2.0F * a - 3.0F * b))));
            }

            // Increment and handle wraparound
            mReadFrameIndex += actualPlaybackRate;
            if(envelope->isFinished()) {
                mIsPlaying = false;
                return true;
            } else if(actualPlaybackRate * (mReadFrameIndex - actualEndFrameIndex) > 0) {
                // outside of range
                if(mIsLooping) {
                    mReadFrameIndex = resumeFrameIndex;
                    if(mReadFrameIndex > actualEndFrameIndex) {
                        actualPlaybackRate = -actualPlaybackRate;
                    }
                } else {
                    mIsPlaying = false;
                    return true;
                }
            }
        }
    }

    return false;
}