/*
 * Copyright (C) 2022 Cameron Wong
 *   name in passport: HUANG GUANNENG
 *   email: hgneng at gmail.com
 *   website: https://eguidedog.net
 *
 * Copyright (C) 2012-2017 Reece H. Dunn
 * Copyright (C) 2011 Google Inc.
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

/*
 * This file contains the JNI bindings to eSpeak used by SpeechSynthesis.java.
 *
 * Android Version: 4.0 (Ice Cream Sandwich)
 * API Version:     14
 */

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdbool.h>
#include <jni.h>

#include <ekho.h>
#include <Log.h>

#define BUFFER_SIZE_IN_MILLISECONDS 1000

const int DEFAULT_SAMPLE_RATE = 16000;
//const int DEFAULT_CHANNEL_COUNT = CHANNEL_COUNT_MONO;
//const int DEFAULT_AUDIO_FORMAT = ENCODING_PCM_16BIT;
const int DEFAULT_BUFFER_SIZE = 1000;

struct native_data_t {
    JNIEnv *env;
    jobject object;
    int sampleRate;
    int channelCount;
    int audioFormat;
    int bufferSizeInMillis;

    native_data_t() {
      env = NULL;
      object = NULL;
      sampleRate = DEFAULT_SAMPLE_RATE;
      //channelCount = DEFAULT_CHANNEL_COUNT;
      //audioFormat = DEFAULT_AUDIO_FORMAT;
      bufferSizeInMillis = DEFAULT_BUFFER_SIZE;
    }
};

jfieldID FIELD_mNativeData;
ekho::Ekho *gp_ekho = 0;

/* These are helpers for converting a jstring to wchar_t*.
 *
 * This assumes that wchar_t is a 32-bit (UTF-32) value.
 */
//@{

static const char *utf8_read(const char *in, wchar_t *c)
{
	if (((uint8_t)*in) < 0x80)
		*c = *in++;
	else switch (((uint8_t)*in) & 0xF0)
	{
	default:
		*c = ((uint8_t)*in++) & 0x1F;
		*c = (*c << 6) + (((uint8_t)*in++) & 0x3F);
		break;
	case 0xE0:
		*c = ((uint8_t)*in++) & 0x0F;
		*c = (*c << 6) + (((uint8_t)*in++) & 0x3F);
		*c = (*c << 6) + (((uint8_t)*in++) & 0x3F);
		break;
	case 0xF0:
		*c = ((uint8_t)*in++) & 0x07;
		*c = (*c << 6) + (((uint8_t)*in++) & 0x3F);
		*c = (*c << 6) + (((uint8_t)*in++) & 0x3F);
		*c = (*c << 6) + (((uint8_t)*in++) & 0x3F);
		break;
	}
	return in;
}

static wchar_t *unicode_string(JNIEnv *env, jstring str)
{
  if (str == NULL) return NULL;

  const char *utf8 = env->GetStringUTFChars(str, NULL);
  wchar_t *utf32 = (wchar_t *)malloc((strlen(utf8) + 1) * sizeof(wchar_t));

  const char *utf8_current = utf8;
  wchar_t *utf32_current = utf32;
  while (*utf8_current)
  {
    utf8_current = utf8_read(utf8_current, utf32_current);
    ++utf32_current;
  }
  *utf32_current = 0;

  env->ReleaseStringUTFChars(str, utf8);
  return utf32;
}

//@}

#define LOG_TAG "EkhoService"
#define DEBUG true

enum synthesis_result {
  SYNTH_CONTINUE = 0,
  SYNTH_ABORT = 1
};

static JavaVM *jvm = NULL;
jmethodID METHOD_nativeSynthCallback;

static JNIEnv *getJniEnv() {
  JNIEnv *env = NULL;
  jvm->AttachCurrentThread(&env, NULL);
  return env;
}

/* Callback from espeak.  Should call back to the TTS API */
static int SynthCallback(short *audioData, int numSamples,
                         void *data, OverlapType type) {
  JNIEnv *env = getJniEnv();
  native_data_t *nat = (native_data_t *) data;
  jobject object = nat->object;

  if (numSamples < 1) {
    env->CallVoidMethod(object, METHOD_nativeSynthCallback, NULL);
    return SYNTH_ABORT;
  } else {
    jbyteArray arrayAudioData = env->NewByteArray(numSamples * 2);
    env->SetByteArrayRegion(arrayAudioData, 0, (numSamples * 2), (jbyte *) audioData);
    env->CallVoidMethod(object, METHOD_nativeSynthCallback, arrayAudioData);
    return SYNTH_CONTINUE;
  }
}

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

JNIEXPORT jint
JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  jvm = vm;
  JNIEnv *env;

  if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
    LOGE("Failed to get the environment using GetEnv()");
    return -1;
  }

  return JNI_VERSION_1_6;
}

JNIEXPORT jboolean
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeClassInit(
    JNIEnv* env, jclass clazz) {
  if (DEBUG) LOGV("%s", __FUNCTION__);
  METHOD_nativeSynthCallback = env->GetMethodID(clazz, "nativeSynthCallback", "([B)V");

  return JNI_TRUE;
}

JNIEXPORT jint
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeCreate(
    JNIEnv *env, jobject object, jstring path) {
  if (DEBUG) LOGV("%s [env=%p, object=%p]", __FUNCTION__, env, object);

  const char *c_path = path ? env->GetStringUTFChars(path, NULL) : NULL;

  if (DEBUG) LOGV("Initializing with path %s", c_path);
  int sampleRate = 0; //espeak_Initialize(AUDIO_OUTPUT_SYNCHRONOUS, BUFFER_SIZE_IN_MILLISECONDS, c_path, 0);

  if (c_path) env->ReleaseStringUTFChars(path, c_path);

  return sampleRate;
}

JNIEXPORT jstring
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeGetVersion(
    JNIEnv *env, jclass clazz) {
  if (DEBUG) LOGV("%s", __FUNCTION__);
  return env->NewStringUTF("9.0"); //espeak_Info(NULL));
}

JNIEXPORT jobjectArray
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeGetAvailableVoices(
    JNIEnv *env, jobject object) {
  if (DEBUG) LOGV("%s", __FUNCTION__);

  jobjectArray voicesArray = (jobjectArray) env->NewObjectArray(
      4, env->FindClass("java/lang/String"), NULL);

  // Cantonese
  int voicesIndex = 0;
  env->SetObjectArrayElement(
      voicesArray, voicesIndex++, env->NewStringUTF("yue"));
  env->SetObjectArrayElement(
      voicesArray, voicesIndex++, env->NewStringUTF("yue"));
  env->SetObjectArrayElement(
      voicesArray, voicesIndex++, env->NewStringUTF("1"));
  env->SetObjectArrayElement(
      voicesArray, voicesIndex++, env->NewStringUTF("28"));

  // Mandarin
  /*
  env->SetObjectArrayElement(
      voicesArray, voicesIndex++, env->NewStringUTF("zho"));
  env->SetObjectArrayElement(
      voicesArray, voicesIndex++, env->NewStringUTF("zho"));
  env->SetObjectArrayElement(
      voicesArray, voicesIndex++, env->NewStringUTF("0"));
  env->SetObjectArrayElement(
      voicesArray, voicesIndex++, env->NewStringUTF("28"));
*/
  return voicesArray;
}

JNIEXPORT jboolean
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeSetVoiceByName(
    JNIEnv *env, jobject object, jstring name) {
  const char *c_name = name ? env->GetStringUTFChars(name, NULL) : NULL;

  if (DEBUG) LOGV("%s(name=%s)", __FUNCTION__, c_name);

  return JNI_FALSE;
}

JNIEXPORT jboolean
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeSetVoiceByProperties(
    JNIEnv *env, jobject object, jstring language, jint gender, jint age) {
  const char *c_language = language ? env->GetStringUTFChars(language, NULL) : NULL;

  if (DEBUG) LOGV("%s(language=%s, gender=%d, age=%d)", __FUNCTION__, c_language, gender, age);

  return JNI_FALSE;
}

JNIEXPORT jboolean
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeSetParameter(
    JNIEnv *env, jobject object, jint parameter, jint value) {
  if (DEBUG) LOGV("%s(parameter=%d, value=%d)", __FUNCTION__, parameter, value);

  return JNI_FALSE;
}

JNIEXPORT jint
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeGetParameter(
    JNIEnv *env, jobject object, jint parameter, jint current) {
  if (DEBUG) LOGV("%s(parameter=%d, pitch=%d)", __FUNCTION__, parameter, current);
  return 0;
}

JNIEXPORT jboolean
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeSetPunctuationCharacters(
    JNIEnv *env, jobject object, jstring characters) {
  if (DEBUG) LOGV("%s)", __FUNCTION__);

  return JNI_FALSE;
}

static native_data_t *getNativeData(JNIEnv *env, jobject object) {
  return (native_data_t *) (env->GetIntField(object, FIELD_mNativeData));
}

JNIEXPORT jboolean
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeSynthesize(
    JNIEnv *env, jobject object, jstring text, jboolean isSsml) {
  if (DEBUG) LOGV("%s", __FUNCTION__);

  native_data_t *nat = getNativeData(env, object);
  const char *c_text = env->GetStringUTFChars(text, NULL);
  nat->env = env;

  if (DEBUG) LOGV("gp_ekho: %p", gp_ekho);
  if (gp_ekho && *c_text) {
    if (DEBUG) LOGV("synth(len=%ul): %s", strlen(c_text), c_text);
    gp_ekho->synth2(c_text, SynthCallback, nat);
  }

  env->ReleaseStringUTFChars(text, c_text);

  return JNI_TRUE;
}

JNIEXPORT jboolean
JNICALL Java_net_eguidedog_ekho_SpeechSynthesis_nativeStop(
    JNIEnv *env, jobject object) {
  if (DEBUG) LOGV("%s", __FUNCTION__);

  if (gp_ekho)
    gp_ekho->stop();

  return JNI_TRUE;
}

#ifdef __cplusplus
}
#endif /* __cplusplus */
