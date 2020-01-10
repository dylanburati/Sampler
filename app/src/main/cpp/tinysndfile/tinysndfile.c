/*
 * Copyright (C) 2012 The Android Open Source Project
 * Modifications Copyright (C) 2020 Dylan Burati
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <string.h>
#include <errno.h>
#include "primitives.h"
#include "sndfile.h"

#define WAVE_FORMAT_PCM         1
#define WAVE_FORMAT_IEEE_FLOAT  3
#define WAVE_FORMAT_EXTENSIBLE  0xFFFE

struct SNDFILE_ {
    int mode;
    uint8_t *temp;  // realloc buffer used for shrinking 16 bits to 8 bits and byte-swapping
    FILE *stream;
    size_t bytesPerFrame;
    size_t remaining;   // frames unread for SFM_READ, frames written for SFM_WRITE
    SF_INFO info;
};

static unsigned little2u(unsigned char *ptr)
{
    return (ptr[1] << 8) + ptr[0];
}

static unsigned big2u(unsigned char *ptr)
{
    return (ptr[0] << 8) + ptr[1];
}

static unsigned little4u(unsigned char *ptr)
{
    return (ptr[3] << 24) + (ptr[2] << 16) + (ptr[1] << 8) + ptr[0];
}

static unsigned big4u(unsigned char *ptr)
{
    return (ptr[0] << 24) + (ptr[1] << 16) + (ptr[2] << 8) + ptr[3];
}

static int isLittleEndian(void)
{
    static const short one = 1;
    return *((const char *) &one) == 1;
}

// "swab" conflicts with OS X <string.h>
static void my_swab(short *ptr, size_t numToSwap)
{
    while (numToSwap > 0) {
        *ptr = little2u((unsigned char *) ptr);
        --numToSwap;
        ++ptr;
    }
}

static int str_endswith(const char *str, const char *ending) {
    size_t len = strlen(str);
    size_t end = strlen(ending);
    if(end > len) {
        return 0;
    }
    return !strcmp(&str[len - end], ending);
}

static int decodeWav(FILE *stream, SNDFILE *handle) {
    handle->info.format = SF_FORMAT_WAV;
    // don't attempt to parse all valid forms, just the most common ones
    unsigned char wav[12];
    size_t actual;
    actual = fread(wav, sizeof(char), sizeof(wav), stream);
    if (actual < 12) {
        fprintf(stderr, "actual %zu < 44\n", actual);
        return 0;
    }
    if (memcmp(wav, "RIFF", 4)) {
        fprintf(stderr, "wav != RIFF\n");
        return 0;
    }
    unsigned riffSize = little4u(&wav[4]);
    if (riffSize < 4) {
        fprintf(stderr, "riffSize %u < 4\n", riffSize);
        return 0;
    }
    if (memcmp(&wav[8], "WAVE", 4)) {
        fprintf(stderr, "missing WAVE\n");
        return 0;
    }
    size_t remaining = riffSize - 4;
    int hadFmt = 0;
    int hadData = 0;
    long dataTell = 0L;
    while (remaining >= 8) {
        unsigned char chunk[8];
        actual = fread(chunk, sizeof(char), sizeof(chunk), stream);
        if (actual != sizeof(chunk)) {
            fprintf(stderr, "actual %zu != %zu\n", actual, sizeof(chunk));
            return 0;
        }
        remaining -= 8;
        unsigned chunkSize = little4u(&chunk[4]);
        if (chunkSize > remaining) {
            fprintf(stderr, "chunkSize %u > remaining %zu\n", chunkSize, remaining);
            return 0;
        }
        if (!memcmp(&chunk[0], "fmt ", 4)) {
            if (hadFmt) {
                fprintf(stderr, "multiple fmt\n");
                return 0;
            }
            if (chunkSize < 2) {
                fprintf(stderr, "chunkSize %u < 2\n", chunkSize);
                return 0;
            }
            unsigned char fmt[40];
            actual = fread(fmt, sizeof(char), 2, stream);
            if (actual != 2) {
                fprintf(stderr, "actual %zu != 2\n", actual);
                return 0;
            }
            unsigned format = little2u(&fmt[0]);
            size_t minSize = 0;
            switch (format) {
                case WAVE_FORMAT_PCM:
                case WAVE_FORMAT_IEEE_FLOAT:
                    minSize = 16;
                    break;
                case WAVE_FORMAT_EXTENSIBLE:
                    minSize = 40;
                    break;
                default:
                    fprintf(stderr, "unsupported format %u\n", format);
                    return 0;
            }
            if (chunkSize < minSize) {
                fprintf(stderr, "chunkSize %u < minSize %zu\n", chunkSize, minSize);
                return 0;
            }
            actual = fread(&fmt[2], sizeof(char), minSize - 2, stream);
            if (actual != minSize - 2) {
                fprintf(stderr, "actual %zu != %zu\n", actual, minSize - 16);
                return 0;
            }
            if (chunkSize > minSize) {
                fseek(stream, (long) (chunkSize - minSize), SEEK_CUR);
            }
            unsigned channels = little2u(&fmt[2]);
            if ((channels < 1) || (channels > 8)) {
                fprintf(stderr, "unsupported channels %u\n", channels);
                return 0;
            }
            unsigned samplerate = little4u(&fmt[4]);
            if (samplerate == 0) {
                fprintf(stderr, "samplerate %u == 0\n", samplerate);
                return 0;
            }
            // ignore byte rate
            // ignore block alignment
            unsigned bitsPerSample = little2u(&fmt[14]);
            if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24 &&
                bitsPerSample != 32) {
                fprintf(stderr, "bitsPerSample %u != 8 or 16 or 24 or 32\n", bitsPerSample);
                return 0;
            }
            unsigned bytesPerFrame = (bitsPerSample >> 3) * channels;
            handle->bytesPerFrame = bytesPerFrame;
            handle->info.samplerate = samplerate;
            handle->info.channels = channels;
            switch (bitsPerSample) {
                case 8:
                    handle->info.format |= SF_FORMAT_PCM_U8;
                    break;
                case 16:
                    handle->info.format |= SF_FORMAT_PCM_16;
                    break;
                case 24:
                    handle->info.format |= SF_FORMAT_PCM_24;
                    break;
                case 32:
                    if (format == WAVE_FORMAT_IEEE_FLOAT)
                        handle->info.format |= SF_FORMAT_FLOAT;
                    else
                        handle->info.format |= SF_FORMAT_PCM_32;
                    break;
            }
            hadFmt = 1;
        } else if (!memcmp(&chunk[0], "data", 4)) {
            if (!hadFmt) {
                fprintf(stderr, "data not preceded by fmt\n");
                return 0;
            }
            if (hadData) {
                fprintf(stderr, "multiple data\n");
                return 0;
            }
            handle->remaining = chunkSize / handle->bytesPerFrame;
            handle->info.frames = handle->remaining;
            dataTell = ftell(stream);
            if (chunkSize > 0) {
                fseek(stream, (long) chunkSize, SEEK_CUR);
            }
            hadData = 1;
        } else if (!memcmp(&chunk[0], "fact", 4)) {
            // ignore fact
            if (chunkSize > 0) {
                fseek(stream, (long) chunkSize, SEEK_CUR);
            }
        } else {
            // ignore unknown chunk
            fprintf(stderr, "ignoring unknown chunk %c%c%c%c\n",
                    chunk[0], chunk[1], chunk[2], chunk[3]);
            if (chunkSize > 0) {
                fseek(stream, (long) chunkSize, SEEK_CUR);
            }
        }
        remaining -= chunkSize;
    }
    if (remaining > 0) {
        fprintf(stderr, "partial chunk at end of RIFF, remaining %zu\n", remaining);
        return 0;
    }
    if (!hadData) {
        fprintf(stderr, "missing data\n");
        return 0;
    }
    (void) fseek(stream, dataTell, SEEK_SET);
    return 1;
}

typedef struct {
    unsigned int key;
    unsigned char value[10];
} AiffSampleRatePair;

static unsigned int getAiffSampleRate(unsigned char *ptr) {
    const int TABLE_SIZE = 19;
    const AiffSampleRatePair table[TABLE_SIZE] = {
        {44100, {64, 14, 172, 68, 0, 0, 0, 0, 0, 0}},
        {48000, {64, 14, 187, 128, 0, 0, 0, 0, 0, 0}},
        {16000, {64, 12, 250, 0, 0, 0, 0, 0, 0, 0}},
        {22050, {64, 13, 172, 68, 0, 0, 0, 0, 0, 0}},
        {32000, {64, 13, 250, 0, 0, 0, 0, 0, 0, 0}},
        {8000, {64, 11, 250, 0, 0, 0, 0, 0, 0, 0}},
        {11025, {64, 12, 172, 68, 0, 0, 0, 0, 0, 0}},
        {88200, {64, 15, 172, 68, 0, 0, 0, 0, 0, 0}},
        {96000, {64, 15, 187, 128, 0, 0, 0, 0, 0, 0}},
        {176400, {64, 16, 172, 68, 0, 0, 0, 0, 0, 0}},
        {192000, {64, 16, 187, 128, 0, 0, 0, 0, 0, 0}},
        {352800, {64, 17, 172, 68, 0, 0, 0, 0, 0, 0}},
        {37800, {64, 14, 147, 168, 0, 0, 0, 0, 0, 0}},
        {44056, {64, 14, 172, 24, 0, 0, 0, 0, 0, 0}},
        {47250, {64, 14, 184, 146, 0, 0, 0, 0, 0, 0}},
        {50000, {64, 14, 195, 80, 0, 0, 0, 0, 0, 0}},
        {50400, {64, 14, 196, 224, 0, 0, 0, 0, 0, 0}},
        {2822400, {64, 20, 172, 68, 0, 0, 0, 0, 0, 0}},
        {5644800, {64, 21, 172, 68, 0, 0, 0, 0, 0, 0}}
    };

    for(int i = 0; i < TABLE_SIZE; i++) {
        if(!memcmp(ptr, table[i].value, 10)) {
            return table[i].key;
        }
    }
    return 0;
}

static int decodeAiff(FILE *stream, SNDFILE *handle) {
    handle->info.format = SF_FORMAT_AIFF;
    // -----------------------------------------------------------
    // HEADER CHUNK
    unsigned char header[12];
    size_t actual;
    actual = fread(header, sizeof(char), sizeof(header), stream);
    if (actual < 12) {
        fprintf(stderr, "actual %zu < 44\n", actual);
        return 0;
    }
    if (memcmp(header, "FORM", 4)) {
        fprintf(stderr, "missing FORM\n");
        return 0;
    }
    if (memcmp(&header[8], "AIFF", 4)) {
        fprintf(stderr, "missing AIFF\n");
        return 0;
    }

    unsigned int remaining = big4u(&header[4]) - 4;
    int hadCOMM = 0;
    int hadSSND = 0;
    long dataTell = 0L;

    while(remaining >= 8) {
        unsigned char chunk[8];
        actual = fread(chunk, sizeof(char), sizeof(chunk), stream);
        if (actual != sizeof(chunk)) {
            fprintf(stderr, "actual %zu != %zu\n", actual, sizeof(chunk));
            return 0;
        }
        remaining -= 8;
        unsigned chunkSize = big4u(&chunk[4]);
        if (chunkSize > remaining) {
            fprintf(stderr, "chunkSize %u > remaining %zu\n", chunkSize, remaining);
            return 0;
        }
        if (!memcmp(&chunk[0], "COMM", 4)) {
            if (hadCOMM) {
                fprintf(stderr, "multiple COMM\n");
                return 0;
            }
            if (chunkSize != 18) {
                fprintf(stderr, "chunkSize %u != 18\n", chunkSize);
                return 0;
            }
            unsigned char fmt[18];
            actual = fread(fmt, sizeof(char), 18, stream);
            if (actual != 18) {
                fprintf(stderr, "actual %zu != 18\n", actual);
                return 0;
            }
            unsigned int channels = big2u(&fmt[0]);
            handle->info.frames = big4u(&fmt[2]);
            handle->remaining = handle->info.frames;
            unsigned int bitsPerSample = big2u(&fmt[6]);
            unsigned int samplerate = getAiffSampleRate(&fmt[8]);

            if ((channels < 1) || (channels > 8)) {
                fprintf(stderr, "unsupported channels %u\n", channels);
                return 0;
            }
            if (samplerate == 0) {
                fprintf(stderr, "samplerate %u == 0\n", samplerate);
                return 0;
            }
            switch (bitsPerSample) {
                case 8:
                    handle->info.format |= SF_FORMAT_PCM_U8;
                    break;
                case 16:
                    handle->info.format |= SF_FORMAT_PCM_16;
                    break;
                case 24:
                    handle->info.format |= SF_FORMAT_PCM_24;
                    break;
                case 32:
                    handle->info.format |= SF_FORMAT_PCM_32;
                    break;
                default:
                    fprintf(stderr, "bitsPerSample %u != 8 or 16 or 24 or 32\n", bitsPerSample);
                    return 0;
            }
            unsigned bytesPerFrame = (bitsPerSample >> 3) * channels;
            handle->bytesPerFrame = bytesPerFrame;
            handle->info.samplerate = samplerate;
            handle->info.channels = channels;
            hadCOMM = 1;
        } else if (!memcmp(&chunk[0], "SSND", 4)) {
            if (hadSSND) {
                fprintf(stderr, "multiple data\n");
                return 0;
            }
            if(chunkSize < 8) {
                fprintf(stderr, "SSND chunk too small");
                return 0;
            }
            fseek(stream, 8, SEEK_CUR);

            dataTell = ftell(stream);
            if (chunkSize > 8) {
                fseek(stream, (long) (chunkSize - 8), SEEK_CUR);
            }
            hadSSND = 1;
        } else {
            // ignore unknown chunk
            fprintf(stderr, "ignoring unknown chunk %c%c%c%c\n",
                    chunk[0], chunk[1], chunk[2], chunk[3]);
            if (chunkSize > 0) {
                fseek(stream, (long) chunkSize, SEEK_CUR);
            }
        }
        remaining -= chunkSize;
    }
    if (remaining > 0) {
        fprintf(stderr, "partial chunk at end of RIFF, remaining %zu\n", remaining);
        return 0;
    }
    if (!hadCOMM) {
        fprintf(stderr, "missing COMM\n");
        return 0;
    }
    if (!hadSSND) {
        fprintf(stderr, "missing SSND\n");
        return 0;
    }
    (void) fseek(stream, dataTell, SEEK_SET);
    return 1;
}

static SNDFILE *sf_open_read(const char *path, SF_INFO *info)
{
    FILE *stream = fopen(path, "rb");
    if (stream == NULL) {
        fprintf(stderr, "fopen %s failed errno %d\n", path, errno);
        return NULL;
    }

    SNDFILE *handle = (SNDFILE *) malloc(sizeof(SNDFILE));
    handle->mode = SFM_READ;
    handle->temp = NULL;
    handle->stream = stream;

    if((str_endswith(path, ".aif") || str_endswith(path, ".aiff")) && decodeAiff(stream, handle)) {
        *info = handle->info;
        return handle;
    } else if(decodeWav(stream, handle)) {
        *info = handle->info;
        return handle;
    }

    // Neither decode worked
    free(handle);
    fclose(stream);
    return NULL;
}

static void write4u(unsigned char *ptr, unsigned u)
{
    ptr[0] = u;
    ptr[1] = u >> 8;
    ptr[2] = u >> 16;
    ptr[3] = u >> 24;
}

static SNDFILE *sf_open_write(const char *path, SF_INFO *info)
{
    int sub = info->format & SF_FORMAT_SUBMASK;
    if (!(
            (info->samplerate > 0) &&
            (info->channels > 0 && info->channels <= 8) &&
            ((info->format & SF_FORMAT_TYPEMASK) == SF_FORMAT_WAV) &&
            (sub == SF_FORMAT_PCM_16 || sub == SF_FORMAT_PCM_U8 || sub == SF_FORMAT_FLOAT ||
                sub == SF_FORMAT_PCM_24 || sub == SF_FORMAT_PCM_32)
          )) {
        return NULL;
    }
    FILE *stream = fopen(path, "w+b");
    if (stream == NULL) {
        fprintf(stderr, "fopen %s failed errno %d\n", path, errno);
        return NULL;
    }
    unsigned char wav[58];
    memset(wav, 0, sizeof(wav));
    memcpy(wav, "RIFF", 4);
    memcpy(&wav[8], "WAVEfmt ", 8);
    if (sub == SF_FORMAT_FLOAT) {
        wav[4] = 50;    // riffSize
        wav[16] = 18;   // fmtSize
        wav[20] = WAVE_FORMAT_IEEE_FLOAT;
    } else {
        wav[4] = 36;    // riffSize
        wav[16] = 16;   // fmtSize
        wav[20] = WAVE_FORMAT_PCM;
    }
    wav[22] = info->channels;
    write4u(&wav[24], info->samplerate);
    unsigned bitsPerSample;
    switch (sub) {
    case SF_FORMAT_PCM_16:
        bitsPerSample = 16;
        break;
    case SF_FORMAT_PCM_U8:
        bitsPerSample = 8;
        break;
    case SF_FORMAT_FLOAT:
        bitsPerSample = 32;
        break;
    case SF_FORMAT_PCM_24:
        bitsPerSample = 24;
        break;
    case SF_FORMAT_PCM_32:
        bitsPerSample = 32;
        break;
    default:    // not reachable
        bitsPerSample = 0;
        break;
    }
    unsigned blockAlignment = (bitsPerSample >> 3) * info->channels;
    unsigned byteRate = info->samplerate * blockAlignment;
    write4u(&wav[28], byteRate);
    wav[32] = blockAlignment;
    wav[34] = bitsPerSample;
    size_t extra = 0;
    if (sub == SF_FORMAT_FLOAT) {
        memcpy(&wav[38], "fact", 4);
        wav[42] = 4;
        memcpy(&wav[50], "data", 4);
        extra = 14;
    } else
        memcpy(&wav[36], "data", 4);
    // dataSize is initially zero
    (void) fwrite(wav, 44 + extra, 1, stream);
    SNDFILE *handle = (SNDFILE *) malloc(sizeof(SNDFILE));
    handle->mode = SFM_WRITE;
    handle->temp = NULL;
    handle->stream = stream;
    handle->bytesPerFrame = blockAlignment;
    handle->remaining = 0;
    handle->info = *info;
    return handle;
}

SNDFILE *sf_open(const char *path, int mode, SF_INFO *info)
{
    if (path == NULL || info == NULL) {
        fprintf(stderr, "path=%p info=%p\n", path, info);
        return NULL;
    }
    switch (mode) {
    case SFM_READ:
        return sf_open_read(path, info);
    case SFM_WRITE:
        return sf_open_write(path, info);
    default:
        fprintf(stderr, "mode=%d\n", mode);
        return NULL;
    }
}

void sf_close(SNDFILE *handle)
{
    if (handle == NULL)
        return;
    free(handle->temp);
    if (handle->mode == SFM_WRITE) {
        (void) fflush(handle->stream);
        rewind(handle->stream);
        unsigned char wav[58];
        size_t extra = (handle->info.format & SF_FORMAT_SUBMASK) == SF_FORMAT_FLOAT ? 14 : 0;
        (void) fread(wav, 44 + extra, 1, handle->stream);
        unsigned dataSize = handle->remaining * handle->bytesPerFrame;
        write4u(&wav[4], dataSize + 36 + extra);    // riffSize
        write4u(&wav[40 + extra], dataSize);        // dataSize
        rewind(handle->stream);
        (void) fwrite(wav, 44 + extra, 1, handle->stream);
    }
    (void) fclose(handle->stream);
    free(handle);
}

sf_count_t sf_readf_float(SNDFILE *handle, float *ptr, sf_count_t desiredFrames)
{
    if (handle == NULL || handle->mode != SFM_READ || ptr == NULL || !handle->remaining ||
            desiredFrames <= 0) {
        return 0;
    }
    if (handle->remaining < (size_t) desiredFrames) {
        desiredFrames = handle->remaining;
    }
    // does not check for numeric overflow
    size_t desiredBytes = desiredFrames * handle->bytesPerFrame;
    size_t actualBytes;
    void *temp = NULL;
    unsigned format = handle->info.format & SF_FORMAT_SUBMASK;
    if (format == SF_FORMAT_PCM_16 || format == SF_FORMAT_PCM_U8 || format == SF_FORMAT_PCM_24) {
        temp = malloc(desiredBytes);
    } else {
        temp = (void *) ptr;
    }
    actualBytes = fread(temp, sizeof(char), desiredBytes, handle->stream);
    size_t actualFrames = actualBytes / handle->bytesPerFrame;
    if((handle->info.format & SF_FORMAT_TYPEMASK) == SF_FORMAT_AIFF) {
        // AIFF uses big-endian
        if(format == SF_FORMAT_PCM_16) {
            uint8_t *tempByteArr = (uint8_t *) temp;
            for(int i = 0; i < actualBytes / 3; i++) {
                uint8_t s = tempByteArr[i * 2];
                tempByteArr[i * 2] = tempByteArr[i * 2 + 1];
                tempByteArr[i * 2 + 1] = s;
            }
        } else if(format == SF_FORMAT_PCM_24) {
            uint8_t *tempByteArr = (uint8_t *) temp;
            for(int i = 0; i < actualBytes / 3; i++) {
                uint8_t s = tempByteArr[i * 3];
                tempByteArr[i * 3] = tempByteArr[i * 3 + 2];
                tempByteArr[i * 3 + 2] = s;
            }
        } else if(format == SF_FORMAT_PCM_32) {
            uint8_t *tempByteArr = (uint8_t *) temp;
            for(int i = 0; i < actualBytes / 4; i++) {
                uint8_t s0 = tempByteArr[i * 4];
                uint8_t s1 = tempByteArr[i * 4 + 1];
                tempByteArr[i * 4] = tempByteArr[i * 4 + 3];
                tempByteArr[i * 4 + 1] = tempByteArr[i * 4 + 2];
                tempByteArr[i * 4 + 2] = s1;
                tempByteArr[i * 4 + 3] = s0;
            }
        }
    }

    handle->remaining -= actualFrames;
    switch (format) {
    case SF_FORMAT_PCM_U8:
        memcpy_to_float_from_u8(ptr, (const unsigned char *) temp,
                actualFrames * handle->info.channels);
        free(temp);
        break;
    case SF_FORMAT_PCM_16:
        memcpy_to_float_from_i16(ptr, (const short *) temp, actualFrames * handle->info.channels);
        free(temp);
        break;
    case SF_FORMAT_PCM_32:
        memcpy_to_float_from_i32(ptr, (const int *) temp, actualFrames * handle->info.channels);
        break;
    case SF_FORMAT_FLOAT:
        break;
    case SF_FORMAT_PCM_24:
        memcpy_to_float_from_p24(ptr, (const uint8_t *) temp, actualFrames * handle->info.channels);
        free(temp);
        break;
    default:
        memset(ptr, 0, actualFrames * handle->info.channels * sizeof(float));
        break;
    }
    return actualFrames;
}