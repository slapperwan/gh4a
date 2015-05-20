package com.gh4a.widget;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class IntegerListPreference extends AppCompatListPreference {
    public IntegerListPreference(Context context) {
        super(context);
    }

    public IntegerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean persistString(String value) {
        if (value == null) {
            return false;
        }
        return persistInt(Integer.valueOf(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if (!getSharedPreferences().contains(getKey())) {
            return defaultReturnValue;
        }
        return String.valueOf(getPersistedInt(0));
    }
}
