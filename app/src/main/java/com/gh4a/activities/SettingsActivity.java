package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.fragment.SettingsFragment;

public class SettingsActivity extends BaseActivity implements
        SettingsFragment.OnStateChangeListener {
    public static final String RESULT_EXTRA_THEME_CHANGED = "theme_changed";
    private static final String STATE_KEY_RESULT = "result";

    private Intent mResultIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mResultIntent = new Intent();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.content_container, new SettingsFragment())
                    .commit();
        } else {
            mResultIntent = savedInstanceState.getParcelable(STATE_KEY_RESULT);
        }

        setResult(RESULT_OK, mResultIntent);
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.settings);
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // we don't have any loaded content
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_KEY_RESULT, mResultIntent);
    }

    @Override
    public void onThemeChanged() {
        mResultIntent.putExtra(RESULT_EXTRA_THEME_CHANGED, true);
        recreate();
    }
}
