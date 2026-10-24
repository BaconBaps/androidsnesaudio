// ChipPlayer standalone compatibility shim for the BSD Audio Overload core.
#pragma once
#include <stdint.h>
#include <cstring>
#define FROM_LE16(x) (x)
#define FROM_LE32(x) (x)
#define FROM_LE64(x) (x)
#define TO_LE16(x) (x)
#define TO_LE32(x) (x)
#define TO_LE64(x) (x)
#define LSB_FIRST 1
#define AUDIO_RATE 44100

#include <stdexcept>
extern thread_local int64_t chip_psf_budget;
inline void chip_psf_tick(int64_t amount) {
    chip_psf_budget-=amount;
    if(chip_psf_budget<=0) throw std::runtime_error("PSF driver exceeded its execution budget");
}
