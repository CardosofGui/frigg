#include <jni.h>
#include <string>
#include "wav_to_mp3.h"

extern "C" JNIEXPORT jboolean JNICALL
Java_com_br_lame_utils_LameConverter_convertWavToMp3(
    JNIEnv *env,
    jobject thiz,
    jstring wavPath,
    jstring mp3Path,
    jint bitrate
) {
    const char* wav_path = env->GetStringUTFChars(wavPath, nullptr);
    const char* mp3_path = env->GetStringUTFChars(mp3Path, nullptr);
    
    if (!wav_path || !mp3_path) {
        if (wav_path) env->ReleaseStringUTFChars(wavPath, wav_path);
        if (mp3_path) env->ReleaseStringUTFChars(mp3Path, mp3_path);
        return JNI_FALSE;
    }
    
    int result = convert_wav_to_mp3(wav_path, mp3_path, bitrate);
    
    env->ReleaseStringUTFChars(wavPath, wav_path);
    env->ReleaseStringUTFChars(mp3Path, mp3_path);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

