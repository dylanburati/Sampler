#ifndef SAMPLER_LIMITER_H
#define SAMPLER_LIMITER_H


#include <cstdint>

class Limiter {

public:
    Limiter(int theOutputRate);
    void processStereo(float *targetData, int32_t numFrames);

private:
    int outputRate;
    float amp;
    float attackCoeff;
    float releaseCoeff;
};


#endif //SAMPLER_LIMITER_H
