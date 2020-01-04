//
// Created by dylan on 1/3/2020.
//

#ifndef SAMPLER_ENVELOPE_H
#define SAMPLER_ENVELOPE_H


#include "AudioProperties.h"

class Envelope {
public:
    const static int RELEASE_START = 1000 * 1000 * 1000;

    Envelope(ADSR src, int theOutputRate);
    void beginRelease(ADSR updated);
    float getFraction();
    void advance();

    bool isFinished();

private:
    int outputRate;
    int decayIndex;
    int sustainIndex;
    float sustainLvl;
    int freeIndex;
    int currentEnvIndex;
};


#endif //SAMPLER_ENVELOPE_H
