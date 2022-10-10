/*
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

package net.eguidedog.ekho.preference;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import net.eguidedog.ekho.R;
import net.eguidedog.ekho.ResourceIdListAdapter;
import net.eguidedog.ekho.VoiceSettings;
import net.eguidedog.ekho.VoiceVariant;

public class VoiceVariantPreference extends DialogPreference {
    private Spinner mCategory;
    private Spinner mVariant;

    private int mCategoryIndex = 0;
    private int mVariantIndex = 0;

    static class ViewHolder
    {
        public TextView text;
    }

    private class VariantData {
        private final int name;
        private final Object arg;
        private final VoiceVariant variant;

        protected VariantData(int name, String variant) {
            this(name, null, variant);
        }

        protected VariantData(int name, Object arg, String variant) {
            this.name = name;
            this.arg = arg;
            this.variant = VoiceVariant.parseVoiceVariant(variant);
        }

        public String getDisplayName(Context context) {
            String text = context.getText(name).toString();
            if (arg == null) {
                return text;
            }
            return String.format(text, arg);
        }

        public VoiceVariant getVariant() {
            return variant;
        }
    }

    public class VariantDataListAdapter extends ArrayAdapter<VariantData>
    {
        private final LayoutInflater mInflater;

        public VariantDataListAdapter(Activity context, VariantData[] resources)
        {
            super(context, android.R.layout.simple_list_item_1, resources);
            mInflater = context.getLayoutInflater();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder;
            if (convertView == null)
            {
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                holder = new ViewHolder();
                holder.text = (TextView)convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            }
            else
            {
                holder = (ViewHolder)convertView.getTag();
            }

            holder.text.setText(getItem(position).getDisplayName(getContext()));
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {
            return getView(position, convertView, parent);
        }
    }

    private Integer[] categories = {
        R.string.variant_default,
    };

    private VariantData[][] variants = {
        {
            new VariantData(R.string.variant_default, "male")
        },
    };

    public VoiceVariantPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDialogLayoutResource(R.layout.voice_variant_preference);
        setLayoutResource(R.layout.information_view);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    public VoiceVariantPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoiceVariantPreference(Context context) {
        this(context, null);
    }

    public void setVoiceVariant(VoiceVariant variant) {
        for (int i = 0; i < variants.length; ++i) {
            VariantData[] items = variants[i];
            for (int j = 0; j < items.length; ++j) {
                if (items[j].getVariant().equals(variant)) {
                    mCategoryIndex = i;
                    mVariantIndex  = j;
                    onDataChanged();
                    return;
                }
            }
        }
        onDataChanged();
    }

    @Override
    protected View onCreateDialogView() {
        View root = super.onCreateDialogView();
        mCategory = (Spinner)root.findViewById(R.id.category);
        mVariant = (Spinner)root.findViewById(R.id.variant);
        return root;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // Cache the indices so they don't get overwritten by the OnItemSelectedListener handlers.
        final int category = mCategoryIndex;
        final int variant  = mVariantIndex;

        mCategory.setAdapter(new ResourceIdListAdapter((Activity)getContext(), categories));
        mCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean mInitializing = true;

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mVariant.setAdapter(new VariantDataListAdapter((Activity) getContext(), variants[position]));
                if (mInitializing) {
                    mVariant.setSelection(variant);
                    mInitializing = false;
                }
                mCategoryIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        mVariant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mVariantIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        mCategory.setSelection(category);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                onDataChanged();
                if (shouldCommit()) {
                    SharedPreferences.Editor editor = getEditor();
                    if (editor != null) {
                        VoiceVariant variant = variants[mCategoryIndex][mVariantIndex].getVariant();
                        editor.putString(VoiceSettings.PREF_VARIANT, variant.toString());
                        editor.commit();
                    }
                }
                break;
        }
        super.onClick(dialog, which);
    }

    private void onDataChanged() {
        Context context = getContext();
        CharSequence category = context.getText(categories[mCategoryIndex]);
        CharSequence variant  = variants[mCategoryIndex][mVariantIndex].getDisplayName(context);
        callChangeListener(String.format("%s (%s)", category, variant));
    }
}
