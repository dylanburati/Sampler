#ifndef CSAMPLER_AUDIOPROPERTIES_H
#define CSAMPLER_AUDIOPROPERTIES_H

#include <cstdint>

struct AudioProperties {
    int32_t channelCount;
    int32_t sampleRate;
};

struct ADSR {
    float attack;
    float decay;
    float sustain;
    float release;
};

#endif //CSAMPLER_AUDIOPROPERTIES_H
