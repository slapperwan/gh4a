package com.gh4a.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.util.AttributeSet;

import java.lang.reflect.Method;

public class AppCompatListPreference extends ListPreference {
    private AppCompatDialog mDialog;

    public AppCompatListPreference(Context context) {
        super(context);
    }

    public AppCompatListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        final CharSequence summary = super.getSummary();
        if (summary == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return summary;
        }
        CharSequence entry = getEntry();
        return String.format(summary.toString(), entry == null ? "" : entry);
    }

    @Override
    protected void showDialog(Bundle state) {
        if (getEntries() == null || getEntryValues() == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        int selected = findIndexOfValue(getValue());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(getDialogTitle())
                .setIcon(getDialogIcon())
                .setNegativeButton(getNegativeButtonText(), this)
                .setSingleChoiceItems(getEntries(), selected, this);

        PreferenceManager pm = getPreferenceManager();
        try {
            Method method = pm.getClass().getDeclaredMethod(
                    "registerOnActivityDestroyListener",
                    PreferenceManager.OnActivityDestroyListener.class);
            method.setAccessible(true);
            method.invoke(pm, this);
        } catch (Exception e) {
            // ignored, nothing we can do
        }

        mDialog = builder.create();
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which >= 0 && getEntryValues() != null) {
            String value = getEntryValues()[which].toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
            dialog.dismiss();
        }
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}
