//
// Created by dylan on 1/3/2020.
//

#include "Envelope.h"

Envelope::Envelope(ADSR src, int theOutputRate) {
    outputRate = theOutputRate;
    decayIndex = (int) (src.attack * 0.001F * outputRate);
    sustainIndex = decayIndex + (int) (src.decay * 0.001F * outputRate);
    sustainLvl = src.sustain;
    freeIndex = Envelope::RELEASE_START + (int) (src.release * 0.001F * outputRate);
    currentEnvIndex = 0;
}

void Envelope::beginRelease(ADSR updated) {
    freeIndex = Envelope::RELEASE_START + (int) (updated.release * 0.001F * outputRate);
    sustainLvl = getFraction();
    currentEnvIndex = Envelope::RELEASE_START;
}

float Envelope::getFraction() {
    if(currentEnvIndex < decayIndex) {
        // A: 0 -> 1
        return currentEnvIndex / 1.0F / decayIndex;
    } else if(currentEnvIndex < sustainIndex) {
        // D: 1 -> s
        return 1 - (1 - sustainLvl) * (currentEnvIndex - decayIndex) /
                   1.0F / (sustainIndex - decayIndex);
    } else if(currentEnvIndex < Envelope::RELEASE_START) {
        return sustainLvl;
    } else if(freeIndex > Envelope::RELEASE_START) {
        // R: s -> 0
        return sustainLvl * ((freeIndex - currentEnvIndex) / 1.0F /
                             (freeIndex - Envelope::RELEASE_START));
    }
    return 0;
}

void Envelope::advance() {
    currentEnvIndex++;
}

bool Envelope::isFinished() {
    return (currentEnvIndex >= freeIndex);
}
