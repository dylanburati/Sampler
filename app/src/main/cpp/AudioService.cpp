//
// Created by dylan on 12/17/2019.
//

#include <jni.h>
#include <media/NdkMediaExtractor.h>
#include <thread>
#include "AudioService.h"
#include "utils/logging.h"

AudioService::AudioService(JavaVM **vm) {
    javaVM = *vm;
}

void AudioService::start() {
    openStream();
    Result result = audioStream->requestStart();
    if(result != Result::OK) {
        LOGE("Failed to start stream. Error: %s", convertToText(result));
        return;
    }
}

bool AudioService::openStream() {
    // Create an audio stream
    AudioStreamBuilder builder;
    builder.setCallback(this);
    builder.setPerformanceMode(PerformanceMode::LowLatency);
    builder.setSharingMode(SharingMode::Exclusive);

    Result result = builder.openStream(&audioStream);
    if(result != Result::OK) {
        LOGE("Failed to open stream. Error: %s", convertToText(result));
        return false;
    }
    for(int i = 0; i < NUM_VOICES; i++) {
        voices.emplace_back(std::make_unique<Player>());
    }

    if(audioStream->getFormat() == AudioFormat::I16) {
        conversionBuffer = std::make_unique<float[]>(
                (size_t) audioStream->getBufferCapacityInFrames() * audioStream->getChannelCount());
    }

    // Reduce stream latency by setting the buffer size to a multiple of the burst size
    auto setBufferSizeResult = audioStream->setBufferSizeInFrames(
            audioStream->getFramesPerBurst() * 2);
    if(setBufferSizeResult != Result::OK) {
        LOGW("Failed to set buffer size. Error: %s", convertToText(setBufferSizeResult.error()));
    }

    return true;
}

void AudioService::stop() {
    if(audioStream != nullptr) {
        audioStream->close();
        voices.clear();
        delete audioStream;
        audioStream = nullptr;
    }
}

void AudioService::loadFile(int sampleIndex, std::string path) {
    tmpFuture = std::async(std::launch::async, &AudioService::doLoadFile, this, sampleIndex, path);
}

void AudioService::doLoadFile(int sampleIndex, std::string path) {
    FileDataSource *src = FileDataSource::newFromUncompressedAsset(path);
    if(src) {
        sources.insert(std::make_pair(sampleIndex, std::shared_ptr<FileDataSource>{src}));
        notifyLoadFile(sampleIndex, src->getSize() / src->getProperties().channelCount,
                       src->getProperties().sampleRate);
        LOGD("props %s %d", path.c_str(), src->getProperties().channelCount);
    }
}

void AudioService::notifyLoadFile(int sampleIndex, int sampleLength, int sampleRate) {
    if(sampleLoadListener == nullptr) return;

    JNIEnv *env;
    jint res = javaVM->AttachCurrentThread(&env, nullptr);
    if(JNI_OK != res) {
        LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
        return;
    }

    jmethodID methodID = env->GetMethodID(env->GetObjectClass(*sampleLoadListener),
                                          "setSampleInfo", "(III)V");
    env->CallVoidMethod(*sampleLoadListener, methodID, sampleIndex, sampleLength, sampleRate);
    javaVM->DetachCurrentThread();
}

void AudioService::notifyVoiceFree() {
    if(voiceFreeListener == nullptr) return;

    JNIEnv *env;
    jint res = javaVM->AttachCurrentThread(&env, nullptr);
    if(JNI_OK != res) {
        LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
        return;
    }

    jmethodID methodID = env->GetMethodID(env->GetObjectClass(*voiceFreeListener),
                                          "voiceFree", "(I)V");
    int outIndex;
    while(voicesToFree.pop(outIndex)) {
        env->CallVoidMethod(*voiceFreeListener, methodID, outIndex);
    }
    javaVM->DetachCurrentThread();
}

void AudioService::setSampleLoadListener(JNIEnv *env, jobject l) {
    if(sampleLoadListener != nullptr) {
        env->DeleteGlobalRef(*sampleLoadListener);
    }
    sampleLoadListener = std::make_unique<jobject>(env->NewGlobalRef(l));
}

void AudioService::setVoiceFreeListener(JNIEnv *env, jobject l) {
    if(voiceFreeListener != nullptr) {
        env->DeleteGlobalRef(*voiceFreeListener);
    }
    voiceFreeListener = std::make_unique<jobject>(env->NewGlobalRef(l));
}

void AudioService::noteMsg(int voiceIndex, int keynum, float velocity, ADSR adsr, int sampleIndex,
                           float start, float resume, float end, float baseKey) {

    if(sampleIndex >= 0 && sampleIndex < sources.size()) {
        Player *player = voices.at(voiceIndex).get();
        player->noteMsg(sources.at(sampleIndex), keynum, velocity, adsr, start, resume, end,
                        baseKey);
    } else {
        LOGE("Invalid sample index");
    }
};

DataCallbackResult
AudioService::onAudioReady(AudioStream *oboeStream, void *audioData, int32_t numFrames) {

    // If our audio stream is expecting 16-bit samples we need to render our floats into a separate
    // buffer then convert them into 16-bit ints
    bool is16Bit = (oboeStream->getFormat() == AudioFormat::I16);
    float *outputBuffer = (is16Bit) ? conversionBuffer.get() : static_cast<float *>(audioData);

    int32_t numSamples = numFrames * oboeStream->getChannelCount();
    for(int i = 0; i < numSamples; i++) {
        outputBuffer[i] = 0;
    }

    int voiceIndex = 0;
    for(auto &voice : voices) {
        if(voice->renderAudio(outputBuffer, numFrames, oboeStream->getSampleRate())) {
            voicesToFree.push(voiceIndex);
        }
        voiceIndex++;
    }

    if(voicesToFree.size() > 0) {
        tmpFuture = std::async(std::launch::async, &AudioService::notifyVoiceFree, this);
    }

    if(is16Bit) {
        oboe::convertFloatToPcm16(outputBuffer,
                                  static_cast<int16_t *>(audioData),
                                  numFrames * oboeStream->getChannelCount());
    }

    return DataCallbackResult::Continue;
}

void AudioService::onErrorAfterClose(AudioStream *oboeStream, Result error) {
    LOGE("The audio stream was closed, please restart the app. Error: %s", convertToText(error));
}