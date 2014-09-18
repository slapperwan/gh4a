package com.gh4a.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.widget.IntegerListPreference;

public class SettingsActivity extends SherlockPreferenceActivity implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    public static final String PREF_NAME = "Gh4a-pref";

    public static final String KEY_THEME = "theme";
    public static final String KEY_TEXT_SIZE = "webview_initial_zoom";
    private static final String KEY_LOGOUT = "logout";
    private static final String KEY_ABOUT = "about";

    public static final String RESULT_EXTRA_THEME_CHANGED = "theme_changed";
    public static final String RESULT_EXTRA_AUTH_CHANGED = "auth_changed";

    private static final String STATE_KEY_RESULT = "result";

    private Intent mResultIntent;

    private IntegerListPreference mThemePref;
    private Preference mLogoutPref;
    private Preference mAboutPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.settings);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            mResultIntent = new Intent();
        } else {
            mResultIntent = savedInstanceState.getParcelable(STATE_KEY_RESULT);
        }

        setResult(RESULT_OK, mResultIntent);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(PREF_NAME);
        addPreferencesFromResource(R.xml.settings);

        mThemePref = (IntegerListPreference) findPreference(KEY_THEME);
        mThemePref.setOnPreferenceChangeListener(this);

        mLogoutPref = findPreference(KEY_LOGOUT);
        mLogoutPref.setOnPreferenceClickListener(this);
        mAboutPref = findPreference(KEY_ABOUT);
        mAboutPref.setOnPreferenceClickListener(this);

        mAboutPref.setSummary(getAppName());
        updateLogoutPrefState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_KEY_RESULT, mResultIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (pref == mThemePref) {
            int newTheme = Integer.parseInt((String) newValue);
            mResultIntent.putExtra(RESULT_EXTRA_THEME_CHANGED, true);
            recreate();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == mLogoutPref) {
            Gh4Application.get(this).logout();
            updateLogoutPrefState();
            mResultIntent.putExtra(RESULT_EXTRA_AUTH_CHANGED, true);
            return true;
        } else if (pref == mAboutPref) {
            AboutDialog d = new AboutDialog(this, Gh4Application.get(this).isAuthorized());
            d.setTitle(getAppName());
            d.show();
            return true;
        }
        return false;
    }

    @Override
    public void recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.recreate();
        } else {
            final Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            startActivity(intent);
            overridePendingTransition(0, 0);

            finish();
            overridePendingTransition(0, 0);
        }
    }

    private void updateLogoutPrefState() {
        Gh4Application app = Gh4Application.get(this);
        if (app.isAuthorized()) {
            mLogoutPref.setEnabled(true);
            mLogoutPref.setSummary(getString(R.string.logout_pref_summary_logged_in,
                    app.getAuthLogin()));
        } else {
            mLogoutPref.setEnabled(false);
            mLogoutPref.setSummary(R.string.logout_pref_summary_logged_out);
        }
    }

    private String getAppName() {
        String version = getAppVersion();
        return getString(R.string.app_name) + " v" + version;
    }

    private String getAppVersion() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // shouldn't happen
            return "";
        }
    }

    private static class AboutDialog extends Dialog implements View.OnClickListener {
        public AboutDialog(Context context, boolean loggedIn) {
            super(context);

            setContentView(R.layout.about_dialog);

            TextView tvCopyright = (TextView) findViewById(R.id.copyright);
            tvCopyright.setText(R.string.copyright_notice);

            findViewById(R.id.btn_by_email).setOnClickListener(this);

            View newIssueButton = findViewById(R.id.btn_by_gh4a);
            if (loggedIn) {
                newIssueButton.setOnClickListener(this);
            } else {
                newIssueButton.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View view) {
            Context context = getContext();
            int id = view.getId();

            if (id == R.id.btn_by_email) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{
                        context.getString(R.string.my_email)
                });
                sendIntent.setType("message/rfc822");

                Intent chooserIntent = Intent.createChooser(sendIntent,
                        context.getString(R.string.send_email_title));
                context.startActivity(chooserIntent);
            } else if (id == R.id.btn_by_gh4a) {
                Intent intent = new Intent(context, IssueEditActivity.class);
                intent.putExtra(Constants.Repository.OWNER,
                        context.getString(R.string.my_username));
                intent.putExtra(Constants.Repository.NAME,
                        context.getString(R.string.my_repo));
                context.startActivity(intent);
            }
        }
    }
}
