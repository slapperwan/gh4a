package com.gh4a.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;
import android.view.View;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.IssueEditActivity;
import com.gh4a.widget.IntegerListPreference;

public class SettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    public interface OnStateChangeListener {
        void onThemeChanged();
        void onAuthStateChanged();
    }

    public static final String PREF_NAME = "Gh4a-pref";

    public static final String KEY_THEME = "theme";
    public static final String KEY_TEXT_SIZE = "webview_initial_zoom";
    private static final String KEY_LOGOUT = "logout";
    private static final String KEY_ABOUT = "about";

    private OnStateChangeListener mListener;
    private IntegerListPreference mThemePref;
    private Preference mLogoutPref;
    private Preference mAboutPref;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof OnStateChangeListener)) {
            throw new IllegalArgumentException("Activity must implement OnStateChangeListener");
        }
        mListener = (OnStateChangeListener) activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (pref == mThemePref) {
            mListener.onThemeChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        Gh4Application app = Gh4Application.get(getActivity());
        if (pref == mLogoutPref) {
            app.logout();
            updateLogoutPrefState();
            mListener.onAuthStateChanged();
            return true;
        } else if (pref == mAboutPref) {
            AboutDialog d = new AboutDialog(getActivity(), app.isAuthorized());
            d.setTitle(getAppName());
            d.show();
            return true;
        }
        return false;
    }

    private void updateLogoutPrefState() {
        Gh4Application app = Gh4Application.get(getActivity());
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
            PackageManager pm = getActivity().getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(getActivity().getPackageName(), 0);
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
