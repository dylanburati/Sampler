#ifndef CSAMPLER_AUDIOSERVICE_H
#define CSAMPLER_AUDIOSERVICE_H

#include <android/asset_manager.h>
#include <oboe/Oboe.h>
#include <future>
#include <jni.h>
#include <vector>
#include <map>
#include <list>
#include "audio/Limiter.h"
#include "audio/Player.h"
#include "utils/LockFreeQueue.h"

using namespace oboe;

class AudioService : public AudioStreamCallback {
public:
    const static int NUM_VOICES = 64;

    explicit AudioService(JavaVM** vm);
    void start();
    void stop();
    void loadFile(std::string sampleId, std::string path);
    void noteMsg(int voiceIndex, int keynum, float velocity, ADSR adsr, std::string sampleId,
            float start, float resume, float end, float baseKey);
    void setSampleLoadListener(JNIEnv *env, jobject l);
    void setVoiceFreeListener(JNIEnv *env, jobject l);

    // Inherited from oboe::AudioStreamCallback
    DataCallbackResult
    onAudioReady(AudioStream *oboeStream, void *audioData, int32_t numFrames) override;
    void onErrorAfterClose(AudioStream *oboeStream, Result error) override;

private:
    JavaVM *javaVM;
    AudioStream *audioStream { nullptr };
    std::unique_ptr<float[]> conversionBuffer { nullptr };
    std::vector<std::unique_ptr<Player>> voices;
    LockFreeQueue<int, NUM_VOICES> voicesToFree;
    std::mutex voicesToFreeMutex;
    std::map<std::string, std::shared_ptr<FileDataSource>> sources;
    std::mutex sourcesMutex;
    std::unique_ptr<jobject> voiceFreeListener { nullptr };
    std::unique_ptr<jobject> sampleLoadListener { nullptr };
    std::unique_ptr<Limiter> limiter;

    std::future<void> tmpFuture;
    std::list<std::future<void>> tmpFutures;
    void doLoadFile(std::string sampleId, std::string path);
    void notifyLoadFile(std::string sampleId, int sampleLength, int sampleRate);
    void notifyVoiceFree();
    bool openStream();
};


#endif //CSAMPLER_AUDIOSERVICE_H
