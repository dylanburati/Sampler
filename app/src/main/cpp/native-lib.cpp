#include <jni.h>
#include <string>
#include <android/asset_manager_jni.h>
#include "AudioService.h"
#include "audio/AudioProperties.h"
#include "utils/logging.h"

extern "C" {

std::unique_ptr<AudioService> audioService;
JavaVM *javaVM;

JNIEXPORT void JNICALL
Java_libre_sampler_ProjectActivity_startAudioService(JNIEnv *env, jobject instance) {
    audioService = std::make_unique<AudioService>(&javaVM);
    audioService->start();
}

JNIEXPORT void JNICALL
Java_libre_sampler_ProjectActivity_stopAudioService(JNIEnv *env, jobject instance) {
    if(audioService == nullptr) {
        LOGD("audio service already stopped");
        return;
    }

    audioService->stop();
    audioService.release();
}

/*
 * processing one time initialization:
 *     Cache the javaVM into our context
 *     Find class ID for JniHelper
 *     Create an instance of JniHelper
 *     Make global reference since we are using them from a native thread
 * Note:
 *     All resources allocated here are never released by application
 *     we rely on system to free all global refs when it goes away;
 *     the pairing function JNI_OnUnload() never gets called at all.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_libre_sampler_ProjectActivity_loadSoundFile(JNIEnv *env, jobject instance,
                                                       jstring sampleId, jstring filename) {
    if(audioService == nullptr) {
        LOGD("audio service is stopped");
        return;
    }

    const char *filenameC = env->GetStringUTFChars(filename, nullptr);
    env->ReleaseStringUTFChars(filename, filenameC);
    const char *sampleIdC = env->GetStringUTFChars(sampleId, nullptr);
    env->ReleaseStringUTFChars(sampleId, sampleIdC);
    audioService->loadFile(std::string(sampleIdC), std::string(filenameC));
}

JNIEXPORT void JNICALL
Java_libre_sampler_ProjectActivity_setSampleLoadListener(JNIEnv *env, jobject instance,
                                                               jobject listener) {
    if(audioService == nullptr) {
        LOGD("audio service is stopped");
        return;
    }

    audioService->setSampleLoadListener(env, listener);
}

JNIEXPORT void JNICALL
Java_libre_sampler_ProjectActivity_sendNoteMsg(JNIEnv *env, jobject instance, jint voiceIndex, jint keynum,
                                                     jfloat velocity, jfloat attack,
                                                     jfloat decay, jfloat sustain, jfloat release,
                                                     jstring sampleId, jfloat start, jfloat resume,
                                                     jfloat end, jfloat baseKey) {

    if(audioService == nullptr) {
        LOGD("audio service is stopped");
        return;
    }

    const char *sampleIdC = env->GetStringUTFChars(sampleId, nullptr);
    env->ReleaseStringUTFChars(sampleId, sampleIdC);
    audioService->noteMsg(voiceIndex, keynum, velocity, ADSR{attack, decay, sustain, release},
                          std::string(sampleIdC), start, resume, end, baseKey);
}

JNIEXPORT void JNICALL
Java_libre_sampler_ProjectActivity_setVoiceFreeListener(JNIEnv *env, jobject instance,
                                                              jobject listener) {
    if(audioService == nullptr) {
        LOGD("audio service is stopped");
        return;
    }

    audioService->setVoiceFreeListener(env, listener);
}

}
