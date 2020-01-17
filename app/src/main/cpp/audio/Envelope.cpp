//
// Created by dylan on 1/3/2020.
//

#include <utility>
#include "Envelope.h"

Envelope::Envelope(int theOutputRate) {
    outputRate = theOutputRate;
}

void Envelope::beginAttack(ADSR src) {
    queue.clear();
    int decayIndex = (int) (src.attack * 0.001F * outputRate);
    int sustainIndex = decayIndex + (int) (src.decay * 0.001F * outputRate);

    queue.push(std::pair<uint64_t, float>{currentEnvIndex, 0.0F});
    queue.push(std::pair<uint64_t, float>{currentEnvIndex + decayIndex, 1.0F});
    queue.push(std::pair<uint64_t, float>{currentEnvIndex + sustainIndex, src.sustain});

    isReleased = false;
}

void Envelope::beginRelease(ADSR updated) {
    queue.clear();
    int freeIndex = (int) (updated.release * 0.001F * outputRate);

    queue.push(std::pair<uint64_t, float>{currentEnvIndex + freeIndex, 0.0F});

    isReleased = true;
}

float Envelope::getFraction() {
    std::pair<uint64_t, float> next;
    bool hasNext = queue.peek(next);
    if(hasNext) {
        if(currentEnvIndex < next.first) {
            // Next event is in the future, use linear interpolation
            currentEnv += (next.second - currentEnv) / (float)(next.first - currentEnvIndex + 1);
        } else {
            // Next event is now, set directly and check for other events at the same timestamp
            while(hasNext && currentEnvIndex >= next.first) {
                currentEnv = next.second;
                queue.pop(next);
                hasNext = queue.peek(next);
            }
        }
    }
    return currentEnv;
}

void Envelope::advance() {
    currentEnvIndex++;
}

bool Envelope::isFinished() {
    return (isReleased && queue.size() == 0);
}
