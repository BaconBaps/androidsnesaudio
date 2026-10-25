# LazyUSF2 in ChipAmp9x 199x

Source: https://github.com/losnoco/Cog/tree/1ef38735c065da74c499eda05e0ae7b1e953586c/Frameworks/lazyusf2/lazyusf2

Portable cached interpreter only; unused architecture-specific recompilers are
omitted. Mupen64Plus and HLE files: GPL-2.0-or-later; Cat RSP files: CC0;
LazyUSF2 glue/resampling: distributed with the project under GPL. X-Scale CIC and Berkeley debugger components: BSD-2/BSD-3. See source headers and rsp_hle/LICENSES. The combined
APK is distributed under GPLv3 as with the existing PSF implementation.

Local changes: bounded CPU/RSP execution per render call; bounded error strings; checked audio DMA and DAC-rate masking;
per-instance resampling; allocation checks and
input validation are described in docs/USF_SUPPORT.md. No Nintendo ROM, BIOS,
USF rip or game audio is included. Tests generate an original MIPS tone program.
