package com.gh4a.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.worker.NotificationsWorker;
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

        mThemePref = findPreference(KEY_THEME);
        mThemePref.setOnPreferenceChangeListener(this);

        mAboutPref = findPreference(KEY_ABOUT);
        mAboutPref.setOnPreferenceClickListener(this);
        mAboutPref.setSummary(getAppName());

        mOpenSourcePref = findPreference(KEY_OPEN_SOURCE_COMPONENTS);
        mOpenSourcePref.setOnPreferenceClickListener(this);

        mNotificationsPref = findPreference(KEY_NOTIFICATIONS);
        mNotificationsPref.setOnPreferenceChangeListener(this);

        mNotificationIntervalPref = findPreference(KEY_NOTIFICATION_INTERVAL);
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
                NotificationsWorker.createNotificationChannels(getActivity());
                NotificationsWorker.schedule(getContext(),
                        Integer.valueOf(mNotificationIntervalPref.getValue()));
            } else {
                NotificationsWorker.cancel(getContext());
            }
            return true;
        }
        if (pref == mNotificationIntervalPref) {
            if (mNotificationsPref.isChecked()) {
                NotificationsWorker.schedule(getContext(), Integer.parseInt((String) newValue));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == mAboutPref) {
            boolean loggedIn = Gh4Application.get().isAuthorized();
            AboutDialogFragment.newInstance(getAppName(), loggedIn)
                    .show(getChildFragmentManager(), "about");
            return true;
        } else if (pref == mOpenSourcePref) {
            new OpenSourceComponentListDialogFragment()
                    .show(getChildFragmentManager(), "opensource");
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

    public static class AboutDialogFragment extends DialogFragment {
        public static AboutDialogFragment newInstance(String title, boolean loggedIn) {
            AboutDialogFragment f = new AboutDialogFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putBoolean("loggedIn", loggedIn);
            f.setArguments(args);
            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString("title");
            boolean loggedIn = getArguments().getBoolean("loggedIn");
            return new AboutDialog(getContext(), title, loggedIn);
        }
    }

    private static class AboutDialog extends AppCompatDialog implements View.OnClickListener {
        public AboutDialog(Context context, String title, boolean loggedIn) {
            super(context);

            setContentView(R.layout.about_dialog);
            setTitle(title);

            TextView tvCopyright = findViewById(R.id.copyright);
            tvCopyright.setText(R.string.copyright_notice);

            findViewById(R.id.btn_by_email).setOnClickListener(this);

            View newIssueButton = findViewById(R.id.btn_by_gh4a);
            if (loggedIn) {
                newIssueButton.setOnClickListener(this);
            } else {
                newIssueButton.setVisibility(View.GONE);
            }

            findViewById(R.id.btn_gh4a).setOnClickListener(this);
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
                Intent intent = IssueListActivity.makeIntent(context,
                        context.getString(R.string.my_username),
                        context.getString(R.string.my_repo));
                context.startActivity(intent);
            } else if (id == R.id.btn_gh4a) {
                Intent intent = RepositoryActivity.makeIntent(context,
                        context.getString(R.string.my_username),
                        context.getString(R.string.my_repo));
                context.startActivity(intent);
            }
        }
    }

    public static class OpenSourceComponentListDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            RecyclerView rv = (RecyclerView) inflater.inflate(R.layout.open_source_component_list, null);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setAdapter(new OpenSourceComponentAdapter(getContext()));

            return new AlertDialog.Builder(getContext())
                    .setView(rv)
                    .setTitle(R.string.open_source_components)
                    .setPositiveButton(R.string.ok, null)
                    .create();
        }
    }

    private static class OpenSourceComponentAdapter extends RecyclerView.Adapter<OpenSourceComponentViewHolder> {
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

        @NonNull
        @Override
        public OpenSourceComponentViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.open_source_component_item, parent, false);
            return new OpenSourceComponentViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull OpenSourceComponentViewHolder holder, int position) {
            final String[] item = COMPONENTS[position];
            holder.bind(item[0], item[1]);
        }

        @Override
        public int getItemCount() {
            return COMPONENTS.length;
        }
    }

    private static class OpenSourceComponentViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitleView;
        private final TextView mUrlView;

        public OpenSourceComponentViewHolder(@NonNull View itemView) {
            super(itemView);
            mTitleView = itemView.findViewById(R.id.title);
            mUrlView = itemView.findViewById(R.id.url);
        }

        public void bind(String title, String url) {
            mTitleView.setText(title);
            mUrlView.setText(url);
        }
    }
}
