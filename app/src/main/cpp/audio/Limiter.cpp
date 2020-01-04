//
// Created by dylan on 12/25/2019.
//

#include <cmath>
#include <algorithm>
#include "Limiter.h"
#include "../utils/logging.h"

Limiter::Limiter(int theOutputRate) : outputRate(theOutputRate) {
    amp = 1.0F;
    attackCoeff = 0.75F;
    releaseCoeff = 0.00025F;
}

void Limiter::process(float *targetData, int32_t numFrames) {
    for(int i = 0; i < numFrames; ++i) {
        float ampFollow = std::max(std::abs(targetData[i * 2]), std::abs(targetData[i * 2 + 1]));
        if(ampFollow > amp) {
            amp = (1 - attackCoeff) * amp + attackCoeff * ampFollow;
        } else {
            amp = (1 - releaseCoeff) * amp + releaseCoeff * ampFollow;
        }
        targetData[i * 2] /= std::max(1.5F * amp, 1.003F);
        targetData[i * 2 + 1] /= std::max(1.5F * amp, 1.003F);
    }
}