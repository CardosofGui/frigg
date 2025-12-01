#include <jni.h>
#include <string>
#include <android/log.h>
#include "wav_to_mp3.h"

#define LOG_TAG "FriggConverterJNI"
#define LOG_ERROR(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_WARN(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOG_INFO(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_DEBUG(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jboolean JNICALL
Java_com_br_frigg_FriggConverter_convertWavToMp3(
    JNIEnv *env,
    jobject thiz,
    jstring wavPath,
    jstring mp3Path,
    jint bitrate
) {
    LOG_INFO("JNI: Recebida chamada convertWavToMp3 com bitrate=%d", bitrate);
    
    const char* wav_path = env->GetStringUTFChars(wavPath, nullptr);
    const char* mp3_path = env->GetStringUTFChars(mp3Path, nullptr);
    
    if (!wav_path || !mp3_path) {
        LOG_ERROR("JNI: Falha ao converter strings JNI para UTF-8: wav_path=%p, mp3_path=%p", wav_path, mp3_path);
        if (wav_path) env->ReleaseStringUTFChars(wavPath, wav_path);
        if (mp3_path) env->ReleaseStringUTFChars(mp3Path, mp3_path);
        return JNI_FALSE;
    }
    
    LOG_DEBUG("JNI: Caminhos convertidos - WAV: %s, MP3: %s", wav_path, mp3_path);
    LOG_INFO("JNI: Chamando convert_wav_to_mp3 nativa...");
    
    int result = convert_wav_to_mp3(wav_path, mp3_path, bitrate);
    
    LOG_INFO("JNI: Função nativa retornou: %s", result ? "SUCESSO" : "FALHA");
    
    env->ReleaseStringUTFChars(wavPath, wav_path);
    env->ReleaseStringUTFChars(mp3Path, mp3_path);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

