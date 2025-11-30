#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "lame.h"

typedef struct {
    uint32_t chunk_id;
    uint32_t chunk_size;
    uint32_t format;
} WavHeader;

typedef struct {
    uint32_t subchunk1_id;
    uint32_t subchunk1_size;
    uint16_t audio_format;
    uint16_t num_channels;
    uint32_t sample_rate;
    uint32_t byte_rate;
    uint16_t block_align;
    uint16_t bits_per_sample;
} WavFmt;

typedef struct {
    uint32_t subchunk2_id;
    uint32_t subchunk2_size;
} WavData;

static int read_wav_header(FILE* file, WavFmt* fmt, uint32_t* data_size) {
    WavHeader header;
    if (fread(&header, sizeof(WavHeader), 1, file) != 1) {
        return 0;
    }

    if (header.chunk_id != 0x46464952) {
        return 0;
    }

    if (header.format != 0x45564157) {
        return 0;
    }

    WavFmt fmt_chunk;
    if (fread(&fmt_chunk, sizeof(WavFmt), 1, file) != 1) {
        return 0;
    }

    if (fmt_chunk.subchunk1_id != 0x20746d66) {
        return 0;
    }

    if (fmt_chunk.audio_format != 1) {
        return 0;
    }

    *fmt = fmt_chunk;

    WavData data_chunk;
    while (fread(&data_chunk, sizeof(WavData), 1, file) == 1) {
        if (data_chunk.subchunk2_id == 0x61746164) {
            *data_size = data_chunk.subchunk2_size;
            return 1;
        } else {
            fseek(file, data_chunk.subchunk2_size, SEEK_CUR);
        }
    }

    return 0;
}

int convert_wav_to_mp3(const char* wav_path, const char* mp3_path, int bitrate) {
    FILE* wav_file = fopen(wav_path, "rb");
    if (!wav_file) {
        return 0;
    }

    WavFmt fmt;
    uint32_t data_size;
    if (!read_wav_header(wav_file, &fmt, &data_size)) {
        fclose(wav_file);
        return 0;
    }

    if (fmt.bits_per_sample != 16) {
        fclose(wav_file);
        return 0;
    }

    lame_t lame = lame_init();
    if (!lame) {
        fclose(wav_file);
        return 0;
    }

    lame_set_in_samplerate(lame, fmt.sample_rate);
    lame_set_VBR(lame, vbr_off);
    lame_set_brate(lame, bitrate);
    lame_set_num_channels(lame, fmt.num_channels);
    lame_set_mode(lame, fmt.num_channels == 1 ? MONO : STEREO);
    lame_set_quality(lame, 2);

    if (lame_init_params(lame) < 0) {
        lame_close(lame);
        fclose(wav_file);
        return 0;
    }

    FILE* mp3_file = fopen(mp3_path, "wb");
    if (!mp3_file) {
        lame_close(lame);
        fclose(wav_file);
        return 0;
    }

    const int pcm_buffer_size = 8192;
    const int mp3_buffer_size = 8192;
    short* pcm_buffer = (short*)malloc(pcm_buffer_size * sizeof(short) * fmt.num_channels);
    unsigned char* mp3_buffer = (unsigned char*)malloc(mp3_buffer_size);

    if (!pcm_buffer || !mp3_buffer) {
        free(pcm_buffer);
        free(mp3_buffer);
        lame_close(lame);
        fclose(wav_file);
        fclose(mp3_file);
        return 0;
    }

    int samples_read;
    int total_samples = data_size / (fmt.num_channels * sizeof(short));

    while (total_samples > 0) {
        int to_read = pcm_buffer_size;
        if (to_read > total_samples) {
            to_read = total_samples;
        }

        samples_read = fread(pcm_buffer, sizeof(short) * fmt.num_channels, to_read, wav_file);
        if (samples_read <= 0) {
            break;
        }

        int mp3_bytes;
        if (fmt.num_channels == 1) {
            mp3_bytes = lame_encode_buffer(lame, pcm_buffer, NULL, samples_read, mp3_buffer, mp3_buffer_size);
        } else {
            mp3_bytes = lame_encode_buffer_interleaved(lame, pcm_buffer, samples_read, mp3_buffer, mp3_buffer_size);
        }

        if (mp3_bytes < 0) {
            break;
        }

        if (mp3_bytes > 0) {
            fwrite(mp3_buffer, 1, mp3_bytes, mp3_file);
        }

        total_samples -= samples_read;
    }

    int mp3_bytes = lame_encode_flush(lame, mp3_buffer, mp3_buffer_size);
    if (mp3_bytes > 0) {
        fwrite(mp3_buffer, 1, mp3_bytes, mp3_file);
    }

    free(pcm_buffer);
    free(mp3_buffer);
    lame_close(lame);
    fclose(wav_file);
    fclose(mp3_file);

    return 1;
}

