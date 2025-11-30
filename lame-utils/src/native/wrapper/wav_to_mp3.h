#ifndef WAV_TO_MP3_H
#define WAV_TO_MP3_H

#ifdef __cplusplus
extern "C" {
#endif

int convert_wav_to_mp3(const char* wav_path, const char* mp3_path, int bitrate);

#ifdef __cplusplus
}
#endif

#endif


