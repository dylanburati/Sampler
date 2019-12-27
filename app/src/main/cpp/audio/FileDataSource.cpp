/*
 * Copyright (C) 2018 The Android Open Source Project
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


#include <oboe/Oboe.h>
#include <fcntl.h>

#include "FileDataSource.h"
#include "AudioProperties.h"
#include "NDKExtractor.h"
#include "../utils/logging.h"
#include "../tinysndfile/sndfile.h"

constexpr int kMaxCompressionRatio{3};

FileDataSource *FileDataSource::newFromUncompressedAsset(std::string filename) {
    SF_INFO properties;
    SNDFILE *audioFile = sf_open(filename.c_str(), SFM_READ, &properties);
    if(!audioFile) {
        LOGE("Failed to open asset %s", filename.c_str());
        return nullptr;
    }

    if(properties.channels == 0) {
        LOGD("Asset %s could not be decoded", filename.c_str());
        return nullptr;
    } else {
        // stereo or greater
        int numSamples = properties.frames * properties.channels;
        auto outputBuffer = std::make_unique<float[]>(numSamples);
        sf_readf_float(audioFile, outputBuffer.get(), properties.frames);
        sf_close(audioFile);
        return new FileDataSource(std::move(outputBuffer), numSamples,
                                  AudioProperties{properties.channels, properties.samplerate});
    }
}

FileDataSource *FileDataSource::newFromCompressedAsset(const char *filename) {

    int fd = open(filename, O_RDONLY | O_CLOEXEC);
    if(fd == -1) {
        LOGE("Failed to open asset %s", filename);
        return nullptr;
    }

    off_t assetSize = lseek(fd, 0, SEEK_END);
    LOGD("Opened %s, size %ld", filename, assetSize);

    // Allocate memory to store the decompressed audio. We don't know the exact
    // size of the decoded data until after decoding so we make an assumption about the
    // maximum compression ratio and the decoded sample format (float for FFmpeg, int16 for NDK).
    const long maximumDataSizeInBytes = kMaxCompressionRatio * assetSize * sizeof(int16_t);
    auto decodedData = new uint8_t[maximumDataSizeInBytes];

    AudioProperties targetProperties = AudioProperties{0, 0};
    int64_t bytesDecoded = NDKExtractor::decode(fd, decodedData, assetSize, &targetProperties);
    auto numSamples = bytesDecoded / sizeof(int16_t);

    // Now we know the exact number of samples we can create a float array to hold the audio data
    auto outputBuffer = std::make_unique<float[]>(numSamples);

    // The NDK decoder can only decode to int16, we need to convert to floats
    oboe::convertPcm16ToFloat(
            reinterpret_cast<int16_t *>(decodedData),
            outputBuffer.get(),
            bytesDecoded / sizeof(int16_t));

    delete[] decodedData;
    close(fd);

    if(bytesDecoded > 0) {
        return new FileDataSource(std::move(outputBuffer), numSamples, targetProperties);
    } else {
        LOGD("Asset %s could not be decoded", filename);
        return nullptr;
    }
}