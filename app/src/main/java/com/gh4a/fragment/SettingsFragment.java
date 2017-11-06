package com.gh4a.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.TwoStatePreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.IssueEditActivity;
import com.gh4a.job.NotificationsJob;
import com.gh4a.widget.IntegerListPreference;

public class SettingsFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    public interface OnStateChangeListener {
        void onThemeChanged();
    }

    public static final String PREF_NAME = "Gh4a-pref";

    public static final String KEY_THEME = "theme";
    public static final String KEY_START_PAGE = "start_page";
    public static final String KEY_TEXT_SIZE = "webview_initial_zoom";
    public static final String KEY_GIF_LOADING = "http_gif_load_mode";
    public static final String KEY_NOTIFICATIONS = "notifications";
    public static final String KEY_NOTIFICATION_INTERVAL = "notification_interval";
    private static final String KEY_ABOUT = "about";
    private static final String KEY_OPEN_SOURCE_COMPONENTS = "open_source_components";

    private OnStateChangeListener mListener;
    private IntegerListPreference mThemePref;
    private Preference mAboutPref;
    private Preference mOpenSourcePref;
    private TwoStatePreference mNotificationsPref;
    private IntegerListPreference mNotificationIntervalPref;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof OnStateChangeListener)) {
            throw new IllegalArgumentException("Activity must implement OnStateChangeListener");
        }
        mListener = (OnStateChangeListener) context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(PREF_NAME);
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mThemePref = (IntegerListPreference) findPreference(KEY_THEME);
        mThemePref.setOnPreferenceChangeListener(this);

        mAboutPref = findPreference(KEY_ABOUT);
        mAboutPref.setOnPreferenceClickListener(this);
        mAboutPref.setSummary(getAppName());

        mOpenSourcePref = findPreference(KEY_OPEN_SOURCE_COMPONENTS);
        mOpenSourcePref.setOnPreferenceClickListener(this);

        mNotificationsPref = (TwoStatePreference) findPreference(KEY_NOTIFICATIONS);
        mNotificationsPref.setOnPreferenceChangeListener(this);

        mNotificationIntervalPref =
                (IntegerListPreference) findPreference(KEY_NOTIFICATION_INTERVAL);
        mNotificationIntervalPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (pref == mThemePref) {
            mListener.onThemeChanged();
            return true;
        }
        if (pref == mNotificationsPref) {
            if ((boolean) newValue) {
                NotificationsJob.createNotificationChannels(getActivity());
                NotificationsJob.scheduleJob(Integer.valueOf(mNotificationIntervalPref.getValue()));
            } else {
                NotificationsJob.cancelJob();
            }
            return true;
        }
        if (pref == mNotificationIntervalPref) {
            if (mNotificationsPref.isChecked()) {
                NotificationsJob.scheduleJob(Integer.parseInt((String) newValue));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == mAboutPref) {
            AboutDialog d = new AboutDialog(getActivity(), Gh4Application.get().isAuthorized());
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

            TextView tvCopyright = findViewById(R.id.copyright);
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
                Intent intent = IssueEditActivity.makeCreateIntent(context,
                        context.getString(R.string.my_username),
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
            { "android-gif-drawable", "https://github.com/koral--/android-gif-drawable" },
            { "Android-Job", "https://github.com/evernote/android-job" },
            { "AndroidSVG", "https://github.com/BigBadaboom/androidsvg" },
            { "emoji-java", "https://github.com/vdurmont/emoji-java" },
            { "GitHubSdk", "https://github.com/maniac103/GitHubSdk" },
            { "HoloColorPicker", "https://github.com/LarsWerkman/HoloColorPicker" },
            { "MarkdownEdit", "https://github.com/Tunous/MarkdownEdit" },
            { "Material Design Icons", "https://github.com/google/material-design-icons" },
            { "PrettyTime", "https://github.com/ocpsoft/prettytime" },
            { "Recycler Fast Scroll", "https://github.com/pluscubed/recycler-fast-scroll" },
            { "Retrofit", "https://github.com/square/retrofit" },
            { "RxAndroid", "https://github.com/ReactiveX/RxAndroid" },
            { "RxJava", "https://github.com/ReactiveX/RxJava" },
            { "RxLoader", "https://github.com/maniac103/RxLoader" },
            { "SmoothProgressBar", "https://github.com/castorflex/SmoothProgressBar" },
        };

        private final LayoutInflater mInflater;

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

            TextView title = convertView.findViewById(R.id.title);
            TextView url = convertView.findViewById(R.id.url);

            title.setText(COMPONENTS[position][0]);
            url.setText(COMPONENTS[position][1]);

            return convertView;
        }
    }
}
