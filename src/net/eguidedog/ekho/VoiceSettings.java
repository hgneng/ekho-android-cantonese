/*
 * Copyright (C) 2022 Cameron Wong
 *   name in passport: HUANG GUANNENG
 *   email: hgneng at gmail.com
 *   website: https://eguidedog.net
 *
 * Copyright (C) 2013 Reece H. Dunn
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

package net.eguidedog.ekho;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class VoiceSettings {
    private final SharedPreferences mPreferences;
    private final SpeechSynthesis mEngine;

    public static final String PREF_DEFAULT_RATE = "default_rate";
    public static final String PREF_RATE = "ekho_rate";
    public static final String PREF_DEFAULT_PITCH = "default_pitch";
    public static final String PREF_PITCH = "ekho_pitch";
    public static final String PREF_VOLUME = "ekho_volume";
    public static final String PREF_PUNCTUATION_LEVEL = "ekho_punctuation_level";
    public static final String PREF_PUNCTUATION_CHARACTERS = "ekho_punctuation_characters";

    public static final String PRESET_RATE = "rate";
    public static final String PRESET_PITCH = "pitch";
    public static final String PRESET_PITCH_RANGE = "pitch-range";
    public static final String PRESET_VOLUME = "volume";
    public static final String PRESET_PUNCTUATION_LEVEL = "punctuation-level";
    public static final String PRESET_PUNCTUATION_CHARACTERS = "punctuation-characters";

    public static final String PUNCTUATION_NONE = "none";
    public static final String PUNCTUATION_SOME = "some";
    public static final String PUNCTUATION_ALL = "all";

    public VoiceSettings(SharedPreferences preferences, SpeechSynthesis engine) {
        mPreferences = preferences;
        mEngine = engine;
    }

    public int getRate() {
        int min = mEngine.Rate.getMinValue();
        int max = mEngine.Rate.getMaxValue();

        int rate = getPreferenceValue(PREF_RATE, Integer.MIN_VALUE);
        if (rate == Integer.MIN_VALUE) {
            rate = (int)((float)getPreferenceValue(PREF_DEFAULT_RATE, 100) / 100 * (float)mEngine.Rate.getDefaultValue());
        }

        if (rate > max) rate = max;
        if (rate < min) rate = min;
        return rate;
    }

    public int getPitch() {
        int min = mEngine.Pitch.getMinValue();
        int max = mEngine.Pitch.getMaxValue();

        int pitch = getPreferenceValue(PREF_PITCH, Integer.MIN_VALUE);
        if (pitch == Integer.MIN_VALUE) {
            pitch = getPreferenceValue(PREF_DEFAULT_PITCH, 100) / 2;
        }

        if (pitch > max) pitch = max;
        if (pitch < min) pitch = min;

        Log.d("Ekho", "VoiceSettings.java:getPitch min=" + min +
            ", max=" + max + ", pitch=" + pitch);

        return pitch;
    }

    public int getVolume() {
        int min = mEngine.Volume.getMinValue();
        int max = mEngine.Volume.getMaxValue();

        int range = getPreferenceValue(PREF_VOLUME, mEngine.Volume.getDefaultValue());
        if (range > max) range = max;
        if (range < min) range = min;
        return range;
    }

    public int getPunctuationLevel() {
        int min = mEngine.Punctuation.getMinValue();
        int max = mEngine.Punctuation.getMaxValue();

        int level = getPreferenceValue(PREF_PUNCTUATION_LEVEL, mEngine.Punctuation.getDefaultValue());
        if (level > max) level = max;
        if (level < min) level = min;
        return level;
    }

    public String getPunctuationCharacters() {
        return mPreferences.getString(PREF_PUNCTUATION_CHARACTERS, null);
    }

    private int getPreferenceValue(String preference, int defaultValue) {
        String prefString = mPreferences.getString(preference, null);
        if (prefString == null) {
            return defaultValue;
        }
        return Integer.parseInt(prefString);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject settings = new JSONObject();
        settings.put(PRESET_RATE, getRate());
        settings.put(PRESET_PITCH, getPitch());
        settings.put(PRESET_VOLUME, getVolume());
        settings.put(PRESET_PUNCTUATION_CHARACTERS, getPunctuationCharacters());
        switch (getPunctuationLevel()) {
            case SpeechSynthesis.PUNCT_NONE:
                settings.put(PRESET_PUNCTUATION_LEVEL, PUNCTUATION_NONE);
                break;
            case SpeechSynthesis.PUNCT_SOME:
                settings.put(PRESET_PUNCTUATION_LEVEL, PUNCTUATION_SOME);
                break;
            case SpeechSynthesis.PUNCT_ALL:
                settings.put(PRESET_PUNCTUATION_LEVEL, PUNCTUATION_ALL);
                break;
        }
        return settings;
    }
}
