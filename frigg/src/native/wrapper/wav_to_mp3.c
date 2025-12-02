#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <errno.h>
#include "lame.h"

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "FriggConverterNative"
#define LOG_ERROR(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_WARN(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOG_INFO(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_DEBUG(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#else
#define LOG_ERROR(...)
#define LOG_WARN(...)
#define LOG_INFO(...)
#define LOG_DEBUG(...)
#endif

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
    LOG_DEBUG("Lendo header WAV...");
    
    WavHeader header;
    if (fread(&header, sizeof(WavHeader), 1, file) != 1) {
        LOG_ERROR("Falha ao ler header WAV: fread retornou erro");
        return 0;
    }

    LOG_DEBUG("Header lido: chunk_id=0x%08X, format=0x%08X", header.chunk_id, header.format);

    if (header.chunk_id != 0x46464952) {
        LOG_ERROR("Chunk ID inválido: esperado 0x46464952 (RIFF), encontrado 0x%08X", header.chunk_id);
        return 0;
    }

    if (header.format != 0x45564157) {
        LOG_ERROR("Formato inválido: esperado 0x45564157 (WAVE), encontrado 0x%08X", header.format);
        return 0;
    }

    LOG_DEBUG("Header RIFF/WAVE válido");

    uint32_t chunk_id;
    uint32_t chunk_size;
    int found_fmt = 0;
    int chunks_checked = 0;

    while (fread(&chunk_id, sizeof(uint32_t), 1, file) == 1) {
        if (fread(&chunk_size, sizeof(uint32_t), 1, file) != 1) {
            LOG_ERROR("Falha ao ler tamanho do chunk");
            return 0;
        }

        chunks_checked++;
        LOG_DEBUG("Chunk encontrado #%d: id=0x%08X, size=%u", chunks_checked, chunk_id, chunk_size);

        if (chunk_id == 0x20746d66) {
            LOG_DEBUG("Chunk 'fmt ' encontrado, lendo dados do formato...");
            
            WavFmt fmt_chunk;
            fmt_chunk.subchunk1_id = chunk_id;
            fmt_chunk.subchunk1_size = chunk_size;
            
            if (fread(&fmt_chunk.audio_format, sizeof(uint16_t), 1, file) != 1) {
                LOG_ERROR("Falha ao ler audio_format do chunk fmt");
                return 0;
            }
            if (fread(&fmt_chunk.num_channels, sizeof(uint16_t), 1, file) != 1) {
                LOG_ERROR("Falha ao ler num_channels do chunk fmt");
                return 0;
            }
            if (fread(&fmt_chunk.sample_rate, sizeof(uint32_t), 1, file) != 1) {
                LOG_ERROR("Falha ao ler sample_rate do chunk fmt");
                return 0;
            }
            if (fread(&fmt_chunk.byte_rate, sizeof(uint32_t), 1, file) != 1) {
                LOG_ERROR("Falha ao ler byte_rate do chunk fmt");
                return 0;
            }
            if (fread(&fmt_chunk.block_align, sizeof(uint16_t), 1, file) != 1) {
                LOG_ERROR("Falha ao ler block_align do chunk fmt");
                return 0;
            }
            if (fread(&fmt_chunk.bits_per_sample, sizeof(uint16_t), 1, file) != 1) {
                LOG_ERROR("Falha ao ler bits_per_sample do chunk fmt");
                return 0;
            }

            LOG_DEBUG("Fmt chunk lido: audio_format=%d, num_channels=%d, sample_rate=%d, bits_per_sample=%d",
                      fmt_chunk.audio_format, fmt_chunk.num_channels, 
                      fmt_chunk.sample_rate, fmt_chunk.bits_per_sample);

            if (fmt_chunk.audio_format != 1) {
                LOG_ERROR("Formato de áudio não suportado: esperado 1 (PCM), encontrado %d", fmt_chunk.audio_format);
                return 0;
            }

            if (fmt_chunk.bits_per_sample != 16) {
                LOG_ERROR("Bits por sample não suportado: esperado 16, encontrado %d", fmt_chunk.bits_per_sample);
                return 0;
            }

            *fmt = fmt_chunk;
            found_fmt = 1;

            if (chunk_size > 16) {
                LOG_DEBUG("Chunk fmt tem tamanho maior que o esperado (%u > 16), pulando bytes extras", chunk_size);
                fseek(file, chunk_size - 16, SEEK_CUR);
            }

            break;
        } else {
            LOG_DEBUG("Pulando chunk não-fmt (id=0x%08X), tamanho=%u", chunk_id, chunk_size);
            if (chunk_size % 2 != 0) {
                chunk_size++;
            }
            fseek(file, chunk_size, SEEK_CUR);
        }
    }

    if (!found_fmt) {
        LOG_ERROR("Chunk 'fmt ' não encontrado após verificar %d chunks", chunks_checked);
        return 0;
    }

    LOG_DEBUG("Procurando chunk 'data'...");
    WavData data_chunk;
    while (fread(&data_chunk, sizeof(WavData), 1, file) == 1) {
        chunks_checked++;
        LOG_DEBUG("Chunk encontrado #%d: subchunk2_id=0x%08X, size=%u", 
                  chunks_checked, data_chunk.subchunk2_id, data_chunk.subchunk2_size);
        
        if (data_chunk.subchunk2_id == 0x61746164) {
            *data_size = data_chunk.subchunk2_size;
            LOG_DEBUG("Chunk 'data' encontrado: tamanho=%u bytes", *data_size);
            return 1;
        } else {
            LOG_DEBUG("Pulando chunk não-data, tamanho=%u", data_chunk.subchunk2_size);
            if (data_chunk.subchunk2_size % 2 != 0) {
                fseek(file, data_chunk.subchunk2_size + 1, SEEK_CUR);
            } else {
                fseek(file, data_chunk.subchunk2_size, SEEK_CUR);
            }
        }
    }

    LOG_ERROR("Chunk 'data' não encontrado após verificar %d chunks", chunks_checked);
    return 0;
}

int convert_wav_to_mp3(const char* wav_path, const char* mp3_path, int bitrate) {
    LOG_INFO("Iniciando conversão: WAV=%s, MP3=%s, bitrate=%d", wav_path, mp3_path, bitrate);
    
    FILE* wav_file = fopen(wav_path, "rb");
    if (!wav_file) {
        LOG_ERROR("Falha ao abrir arquivo WAV: %s (errno=%d: %s)", wav_path, errno, strerror(errno));
        return 0;
    }
    LOG_DEBUG("Arquivo WAV aberto com sucesso");

    WavFmt fmt;
    uint32_t data_size;
    if (!read_wav_header(wav_file, &fmt, &data_size)) {
        LOG_ERROR("Falha ao ler header WAV do arquivo: %s", wav_path);
        fclose(wav_file);
        return 0;
    }
    LOG_INFO("Header WAV lido: %d canais, %d Hz, %d-bit, %u bytes de dados", 
             fmt.num_channels, fmt.sample_rate, fmt.bits_per_sample, data_size);

    if (fmt.bits_per_sample != 16) {
        LOG_ERROR("Bits por sample não suportado: esperado 16, encontrado %d", fmt.bits_per_sample);
        fclose(wav_file);
        return 0;
    }
    LOG_DEBUG("Validação de bits por sample OK (16-bit)");

    lame_t lame = lame_init();
    if (!lame) {
        LOG_ERROR("Falha ao inicializar encoder LAME");
        fclose(wav_file);
        return 0;
    }
    LOG_DEBUG("Encoder LAME inicializado");

    lame_set_in_samplerate(lame, fmt.sample_rate);
    lame_set_VBR(lame, vbr_off);
    lame_set_brate(lame, bitrate);
    lame_set_num_channels(lame, fmt.num_channels);
    lame_set_mode(lame, fmt.num_channels == 1 ? MONO : STEREO);
    lame_set_quality(lame, 2);
    LOG_DEBUG("Parâmetros LAME configurados: sample_rate=%d, bitrate=%d, channels=%d, mode=%s",
              fmt.sample_rate, bitrate, fmt.num_channels, fmt.num_channels == 1 ? "MONO" : "STEREO");

    if (lame_init_params(lame) < 0) {
        LOG_ERROR("Falha ao inicializar parâmetros LAME (lame_init_params retornou < 0)");
        lame_close(lame);
        fclose(wav_file);
        return 0;
    }
    LOG_DEBUG("Parâmetros LAME inicializados com sucesso");

    FILE* mp3_file = fopen(mp3_path, "wb");
    if (!mp3_file) {
        LOG_ERROR("Falha ao criar arquivo MP3: %s (errno=%d: %s)", mp3_path, errno, strerror(errno));
        lame_close(lame);
        fclose(wav_file);
        return 0;
    }
    LOG_DEBUG("Arquivo MP3 criado com sucesso");

    const int pcm_buffer_size = 8192;
    const int mp3_buffer_size = 8192;
    size_t pcm_buffer_bytes = pcm_buffer_size * sizeof(short) * fmt.num_channels;
    short* pcm_buffer = (short*)malloc(pcm_buffer_bytes);
    unsigned char* mp3_buffer = (unsigned char*)malloc(mp3_buffer_size);

    if (!pcm_buffer || !mp3_buffer) {
        LOG_ERROR("Falha ao alocar memória: pcm_buffer=%p, mp3_buffer=%p", pcm_buffer, mp3_buffer);
        free(pcm_buffer);
        free(mp3_buffer);
        lame_close(lame);
        fclose(wav_file);
        fclose(mp3_file);
        return 0;
    }
    LOG_DEBUG("Buffers alocados: PCM=%zu bytes, MP3=%d bytes", pcm_buffer_bytes, mp3_buffer_size);

    int samples_read;
    int total_samples = data_size / (fmt.num_channels * sizeof(short));
    LOG_INFO("Iniciando codificação: %d samples totais a processar", total_samples);

    int iterations = 0;
    int total_mp3_bytes = 0;
    while (total_samples > 0) {
        int to_read = pcm_buffer_size;
        if (to_read > total_samples) {
            to_read = total_samples;
        }

        samples_read = fread(pcm_buffer, sizeof(short) * fmt.num_channels, to_read, wav_file);
        if (samples_read <= 0) {
            if (samples_read == 0 && total_samples > 0) {
                LOG_WARN("Fim de arquivo inesperado: restam %d samples, mas fread retornou 0", total_samples);
            }
            break;
        }

        int mp3_bytes;
        if (fmt.num_channels == 1) {
            mp3_bytes = lame_encode_buffer(lame, pcm_buffer, NULL, samples_read, mp3_buffer, mp3_buffer_size);
        } else {
            mp3_bytes = lame_encode_buffer_interleaved(lame, pcm_buffer, samples_read, mp3_buffer, mp3_buffer_size);
        }

        if (mp3_bytes < 0) {
            LOG_ERROR("Erro na codificação LAME: retornou %d (iteração %d)", mp3_bytes, iterations);
            break;
        }

        if (mp3_bytes > 0) {
            size_t written = fwrite(mp3_buffer, 1, mp3_bytes, mp3_file);
            if (written != (size_t)mp3_bytes) {
                LOG_ERROR("Erro ao escrever no arquivo MP3: esperado %d bytes, escrito %zu", mp3_bytes, written);
                break;
            }
            total_mp3_bytes += mp3_bytes;
        }

        total_samples -= samples_read;
        iterations++;
        
        if (iterations % 100 == 0) {
            LOG_DEBUG("Progresso: %d iterações, %d samples restantes, %d bytes MP3 escritos", 
                      iterations, total_samples, total_mp3_bytes);
        }
    }

    LOG_DEBUG("Loop de codificação concluído: %d iterações, %d bytes MP3 escritos", iterations, total_mp3_bytes);

    int mp3_bytes = lame_encode_flush(lame, mp3_buffer, mp3_buffer_size);
    if (mp3_bytes > 0) {
        size_t written = fwrite(mp3_buffer, 1, mp3_bytes, mp3_file);
        if (written != (size_t)mp3_bytes) {
            LOG_WARN("Erro ao escrever flush no MP3: esperado %d bytes, escrito %zu", mp3_bytes, written);
        } else {
            total_mp3_bytes += mp3_bytes;
            LOG_DEBUG("Flush LAME escrito: %d bytes", mp3_bytes);
        }
    } else if (mp3_bytes < 0) {
        LOG_WARN("Erro no flush LAME: retornou %d", mp3_bytes);
    }

    free(pcm_buffer);
    free(mp3_buffer);
    lame_close(lame);
    fclose(wav_file);
    fclose(mp3_file);

    LOG_INFO("Conversão concluída com sucesso: %d bytes MP3 gerados", total_mp3_bytes);
    return 1;
}

