//
// Created by dylan on 1/3/2020.
//

#ifndef SAMPLER_ENVELOPE_H
#define SAMPLER_ENVELOPE_H


#include <atomic>
#include "AudioProperties.h"
#include "../utils/LockFreeQueue.h"

class Envelope {
public:
    explicit Envelope(int theOutputRate);
    void beginAttack(ADSR src);
    void beginRelease(ADSR updated);
    float getFraction();
    void advance();

    bool isFinished();

private:
    std::atomic<bool> isReleased {false};
    int outputRate;
    LockFreeQueue<std::pair<uint64_t, float>, 16> queue;

    uint64_t currentEnvIndex = 0;
    float currentEnv = 0.0F;
    float slope = 0.0F;
};


#endif //SAMPLER_ENVELOPE_H
