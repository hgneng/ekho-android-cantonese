/*
 * Copyright (C) 2022 Cameron Wong
 *   name in passport: HUANG GUANNENG
 *   email: hgneng at gmail.com
 *   website: https://eguidedog.net
 *
 * Copyright (C) 2012-2013 Reece H. Dunn
 * Copyright (C) 2009 The Android Open Source Project
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
 * This Activity is used by Android to get the list of languages to display
 * to the user when selecting the text-to-speech language. This is by locale,
 * not voice name.
 */

package net.eguidedog.ekho;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech.Engine;
import android.util.Log;

import net.eguidedog.ekho.SpeechSynthesis.SynthReadyCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckVoiceData extends Activity {
    private static final String TAG = "EkhoTTS";

    /** Resources required for Ekho to run correctly. */
    private static final String[] BASE_RESOURCES = {
        "zhy.dict",
        "jyutping.index",
        "jyutping.voice",
    };

    public static File getDataPath(Context context) {
        return new File(context.getDir("voices", MODE_PRIVATE), "ekho-data");
    }

    public static boolean hasBaseResources(Context context) {
        final File dataPath = getDataPath(context);
        Log.i(TAG, "dataPath=" + dataPath.toString());

        for (String resource : BASE_RESOURCES) {
            final File resourceFile = new File(dataPath, resource);

            if (!resourceFile.exists()) {
                Log.e(TAG, "Missing base resource: " + resourceFile.getPath());
                return false;
            }
        }

        return true;
    }

    public static boolean canUpgradeResources(Context context) {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> availableLanguages = new ArrayList<String>();
        ArrayList<String> unavailableLanguages = new ArrayList<String>();

        boolean haveBaseResources = hasBaseResources(this);
        if (!haveBaseResources || canUpgradeResources(this)) {
            if (!haveBaseResources) {
                unavailableLanguages.add(Locale.ENGLISH.toString());
            }
            returnResults(Engine.CHECK_VOICE_DATA_FAIL, availableLanguages, unavailableLanguages);
            return;
        }

        final SpeechSynthesis engine = new SpeechSynthesis(this, mSynthReadyCallback);
        final List<Voice> voices = engine.getAvailableVoices();

        for (Voice voice : voices) {
            availableLanguages.add(voice.toString());
        }

        returnResults(Engine.CHECK_VOICE_DATA_PASS, availableLanguages, unavailableLanguages);
    }

    private void returnResults(int result, ArrayList<String> availableLanguages, ArrayList<String> unavailableLanguages) {
        final Intent returnData = new Intent();
        returnData.putStringArrayListExtra(Engine.EXTRA_AVAILABLE_VOICES, availableLanguages);
        returnData.putStringArrayListExtra(Engine.EXTRA_UNAVAILABLE_VOICES, unavailableLanguages);
        setResult(result, returnData);
        finish();
    }

    private final SynthReadyCallback mSynthReadyCallback = new SynthReadyCallback() {
        @Override
        public void onSynthDataReady(byte[] audioData) {
            // Do nothing.
        }

        @Override
        public void onSynthDataComplete() {
            // Do nothing.
        }
    };
}
