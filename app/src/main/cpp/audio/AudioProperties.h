#ifndef CSAMPLER_AUDIOPROPERTIES_H
#define CSAMPLER_AUDIOPROPERTIES_H

struct AudioProperties {
    int32_t channelCount;
    int32_t sampleRate;
};

struct ADSR {
    float attack;
    float decay;
    float sustain;
    float release;
    const static int RELEASE_START = 1000 * 1000 * 1000;
};

#endif //CSAMPLER_AUDIOPROPERTIES_H
