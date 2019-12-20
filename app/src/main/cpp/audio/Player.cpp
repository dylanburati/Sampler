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

void Player::noteMsg(const std::shared_ptr<FileDataSource> src, int keynum, float velocity,
                     ADSR adsr, float start, float resume, float end, float baseKey) {

    if(velocity > 0) {
        playbackRate = pow(2.0F, (keynum - baseKey) / 12.0F);
        volumeMultiplier = velocity;

        mSource = src;
        int sampleRate = mSource->getProperties().sampleRate;
        currentEnvIndex = 0;
        envelope = adsr;
        startFrameIndex = (int) round(start * sampleRate);
        endFrameIndex = (int) round(end * sampleRate);
        resumeFrameIndex = (int) round(resume * sampleRate);
        setLooping(resume > 0);
        setPlaying(true);
    } else {
        currentEnvIndex = ADSR::RELEASE_START;
    }
}

bool Player::renderAudio(float *targetData, int32_t numFrames, int outputRate) {

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
        int decayIndex = (int) (envelope.attack * 0.001F * outputRate);
        int sustainIndex = decayIndex + (int) (envelope.decay * 0.001F * outputRate);
        int freeIndex = ADSR::RELEASE_START + (int) (envelope.release * 0.001F * outputRate);

        for(int i = 0; i < framesToRenderFromData; ++i) {
            int currentIndex = (int) mReadFrameIndex;
            float frac = mReadFrameIndex - currentIndex;
            if(i < 1) {
                currentIndex = 1;
                frac = 0;
            } else if(i > totalSourceFrames - 2) {
                currentIndex = totalSourceFrames - 2;
                frac = 1;
            }

            currentEnvIndex++;
            float actualVolMultiplier;
            if(currentEnvIndex < decayIndex) {
                actualVolMultiplier = currentEnvIndex / 1.0F / decayIndex;
            } else if(currentEnvIndex < sustainIndex) {
                actualVolMultiplier = 1 - (1 - envelope.sustain) * (currentEnvIndex - decayIndex) /
                                           1.0F / (sustainIndex - decayIndex);
            } else if(currentEnvIndex < ADSR::RELEASE_START) {
                actualVolMultiplier = envelope.sustain;
            } else {
                actualVolMultiplier = envelope.sustain * (1 - (currentEnvIndex -
                                                                ADSR::RELEASE_START) / 1.0F /
                                                               (freeIndex - ADSR::RELEASE_START));
            }
            actualVolMultiplier *= actualVolMultiplier;
            actualVolMultiplier *= volumeMultiplier;

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
            if(mReadFrameIndex >= totalSourceFrames || mReadFrameIndex < 0 ||
               currentEnvIndex >= freeIndex) {
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

void Player::renderSilence(float *start, int32_t numSamples) {
    for(int i = 0; i < numSamples; ++i) {
        start[i] = 0;
    }
}