package com.gh4a.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
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
    private static final String KEY_OPEN_SOURCE_COMPONENTS = "open_source_components";

    private OnStateChangeListener mListener;
    private IntegerListPreference mThemePref;
    private Preference mLogoutPref;
    private Preference mAboutPref;
    private Preference mOpenSourcePref;

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

        mOpenSourcePref = findPreference(KEY_OPEN_SOURCE_COMPONENTS);
        mOpenSourcePref.setOnPreferenceClickListener(this);

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
        Gh4Application app = Gh4Application.get();
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
        } else if (pref == mOpenSourcePref) {
            OpenSourceComponentListDialog d = new OpenSourceComponentListDialog(getActivity());
            d.show();
            return true;
        }
        return false;
    }

    private void updateLogoutPrefState() {
        Gh4Application app = Gh4Application.get();
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

    private static class AboutDialog extends AppCompatDialog implements View.OnClickListener {
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

    private static class OpenSourceComponentListDialog extends AlertDialog {
        public OpenSourceComponentListDialog(Context context) {
            super(context);

            LayoutInflater inflater = LayoutInflater.from(context);
            ListView lv = (ListView) inflater.inflate(R.layout.open_source_component_list, null);
            lv.setAdapter(new OpenSourceComponentAdapter(context));

            setView(lv);
            setTitle(R.string.open_source_components);
            setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.ok),
                    (DialogInterface.OnClickListener) null);
        }
    }

    private static class OpenSourceComponentAdapter extends BaseAdapter {
        private static final String[][] COMPONENTS = new String[][] {
            { "Android-ProgressFragment", "https://github.com/johnkil/Android-ProgressFragment" },
            { "android-support-v4-preferencefragment",
                    "https://github.com/kolavar/android-support-v4-preferencefragment" },
            { "Floating Action Button", "https://github.com/shamanland/floating-action-button" },
            { "Github Java bindings", "https://github.com/maniac103/egit-github" },
            { "HoloColorPicker", "https://github.com/LarsWerkman/HoloColorPicker" },
            { "Material Design Icons", "https://github.com/google/material-design-icons" },
            { "Nine Old Androids", "https://github.com/JakeWharton/NineOldAndroids" },
            { "PrettyTime", "https://github.com/ocpsoft/prettytime" },
            { "SmoothProgressBar", "https://github.com/castorflex/SmoothProgressBar" }
        };

        private LayoutInflater mInflater;

        public OpenSourceComponentAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return COMPONENTS.length;
        }

        @Override
        public Object getItem(int position) {
            return COMPONENTS[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.open_source_component_item, parent, false);
            }

            TextView title = (TextView) convertView.findViewById(R.id.title);
            TextView url = (TextView) convertView.findViewById(R.id.url);

            title.setText(COMPONENTS[position][0]);
            url.setText(COMPONENTS[position][1]);

            return convertView;
        }
    }
}
