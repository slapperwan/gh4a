package com.gh4a.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.resolver.LinkParser;
import com.gh4a.fragment.SettingsFragment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.FragmentActivity;

public class IntentUtils {
    private static final String EXTRA_NEW_TASK = "IntentUtils.new_task";
    private static final String RAW_URL_FORMAT = "https://raw.githubusercontent.com/%s/%s/%s/%s";

    private IntentUtils() {
    }

    public static void openLinkInternallyOrExternally(FragmentActivity activity, Uri uri) {
        String uriScheme = uri.getScheme();
        if (uriScheme == null || uriScheme.equals("file") || uriScheme.equals("content")) {
            // We can't do anything about relative or anchor URLs here, and there are no good reasons to
            // try to open file or content provider URIs (the former ones would raise an exception on API 24+)
            return;
        }

        if (uriScheme.equals("mailto")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                activity.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, R.string.link_not_openable, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        LinkParser.ParseResult result = LinkParser.parseUri(activity, uri, null);
        int headerColor = activity instanceof BaseActivity ? ((BaseActivity) activity).getCurrentHeaderColor() : 0;
        if (result == null) {
            openInCustomTabOrBrowser(activity, uri, headerColor);
        } else if (result.intent != null) {
            activity.startActivity(result.intent);
        } else if (result.loadTask != null) {
            result.loadTask.setOpenUnresolvedUriInCustomTab(headerColor);
            result.loadTask.execute();
        }
    }

    public static void launchBrowser(Context context, Uri uri) {
        launchBrowser(context, uri, 0);
    }

    public static void launchBrowser(Context context, Uri uri, int flags) {
        if (uri == null) {
            return;
        }
        Intent intent = createBrowserIntent(context, uri);
        if (intent != null) {
            try {
                intent.addFlags(flags);
                context.startActivity(intent);
                return;
            } catch (ActivityNotFoundException e) {
                // just show toast
            }
        }
        Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_LONG).show();
    }

    // We want to forward the URI to a browser, but our own intent filter matches
    // the browser's intent filters. We therefore resolve the intent by ourselves,
    // strip our own entry from the list and pass the result to the system's
    // activity chooser.
    // When doing that, pass a dummy URI to the resolver and swap in our real URI
    // later, as otherwise the system might return our package only if it's set
    // to handle the Github URIs by default
    private static Uri buildDummyUri(Uri uri) {
        return uri.buildUpon().authority("www.somedummy.com").build();
    }

    private static Intent createBrowserIntent(Context context, Uri uri) {
        final Uri dummyUri = buildDummyUri(uri);
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, dummyUri)
                .addCategory(Intent.CATEGORY_BROWSABLE);
        return createActivityChooserIntent(context, browserIntent, uri);
    }

    public static Intent createViewerOrBrowserIntent(Context context, Uri uri, String mime) {
        final Uri dummyUri = buildDummyUri(uri);
        final Intent viewIntent = new Intent(Intent.ACTION_VIEW).setDataAndType(dummyUri, mime);
        final Intent resolvedViewIntent = createActivityChooserIntent(context, viewIntent, uri);
        if (resolvedViewIntent != null) {
            return resolvedViewIntent;
        }
        return createBrowserIntent(context, uri);
    }

    public static void openInCustomTabOrBrowser(Activity activity, Uri uri) {
        openInCustomTabOrBrowser(activity, uri, 0);
    }

    public static void openInCustomTabOrBrowser(Activity activity, Uri uri, int headerColor) {
        SharedPreferences prefs = activity.getSharedPreferences(SettingsFragment.PREF_NAME,
                Context.MODE_PRIVATE);
        boolean customTabsEnabled = prefs.getBoolean(SettingsFragment.KEY_CUSTOM_TABS, true);
        String pkg = CustomTabsHelper.getPackageNameToUse(activity);

        if (pkg != null && customTabsEnabled) {
            if (headerColor == 0) {
                headerColor = UiUtils.resolveColor(activity, androidx.appcompat.R.attr.colorPrimary);
            }
            CustomTabColorSchemeParams colorParams = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(headerColor)
                    .build();
            CustomTabsIntent i = new CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(colorParams)
                    .build();
            i.intent.setPackage(pkg);
            i.launchUrl(activity, uri);
        } else {
            launchBrowser(activity, uri);
        }
    }

    public static Uri.Builder createBaseUriForUser(String user) {
        return new Uri.Builder()
                .scheme("https")
                .authority("github.com")
                .appendPath(user);
    }

    public static Uri.Builder createBaseUriForRepo(String repoOwner, String repoName) {
        return createBaseUriForUser(repoOwner)
                .appendPath(repoName);
    }

    public static String createRawFileUrl(String repoOwner, String repoName, String ref, String path) {
        return String.format(Locale.US, RAW_URL_FORMAT, repoOwner, repoName, ref, path);
    }

    public static void share(Context context, String subject, Uri url) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, url.toString());
        context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.share_title)));
    }

    public static void copyToClipboard(Context context, CharSequence label, CharSequence text) {
        ClipboardManager clipboardManager =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(label, text);
        clipboardManager.setPrimaryClip(clipData);
    }

    private static Intent createActivityChooserIntent(Context context, Intent intent, Uri uri) {
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> activities = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        final ArrayList<Intent> chooserIntents = new ArrayList<>();
        final String ourPackageName = context.getPackageName();

        Collections.sort(activities, new ResolveInfo.DisplayNameComparator(pm));

        for (ResolveInfo resInfo : activities) {
            ActivityInfo info = resInfo.activityInfo;
            if (!info.enabled || !info.exported) {
                continue;
            }
            if (info.packageName.equals(ourPackageName)) {
                continue;
            }

            Intent targetIntent = new Intent(intent);
            targetIntent.setPackage(info.packageName);
            targetIntent.setDataAndType(uri, intent.getType());
            chooserIntents.add(targetIntent);
        }

        if (chooserIntents.isEmpty()) {
            return null;
        }

        final Intent lastIntent = chooserIntents.remove(chooserIntents.size() - 1);
        if (chooserIntents.isEmpty()) {
            // there was only one, no need to show the chooser
            return lastIntent;
        }

        return Intent.createChooser(lastIntent, null)
                .putExtra(Intent.EXTRA_ALTERNATE_INTENTS, chooserIntents.toArray(new Intent[0]));
    }

    public static void startNewTask(@NonNull Context context, @NonNull Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(EXTRA_NEW_TASK, true);
        context.startActivity(intent);
    }

    public static boolean isNewTaskIntent(@Nullable Intent intent) {
        return intent != null && intent.getBooleanExtra(EXTRA_NEW_TASK, false);
    }

    public static void putCompressedExtra(Intent intent, String key, Object object) {
        Bundle extras = intent.getExtras();
        Bundle updatedExtras = extras != null ? extras : new Bundle();
        putCompressedValueToBundle(updatedExtras, key, object);
        intent.replaceExtras(updatedExtras);
    }

    public static <T> T getCompressedExtra(Intent intent, String key) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }
        return readCompressedValueFromBundle(extras, key);
    }

    public static void putCompressedValueToBundle(Bundle bundle, String key, Object value) {
        Parcel parcel = Parcel.obtain();
        parcel.writeValue(value);
        byte[] compressedData = compressBytes(parcel.marshall());
        parcel.recycle();
        bundle.putByteArray(key, compressedData);
    }

    public static <T> T readCompressedValueFromBundle(Bundle bundle, String key) {
        byte[] compressedData = bundle.getByteArray(key);
        if (compressedData == null) {
            return null;
        }

        byte[] uncompressedData = uncompressBytes(compressedData);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(uncompressedData, 0, uncompressedData.length);
        parcel.setDataPosition(0);
        T value = (T) parcel.readValue(IntentUtils.class.getClassLoader());
        parcel.recycle();
        return value;
    }

    private static byte[] compressBytes(@NonNull byte[] dataToCompress) {
        int compressionLevel = 3;  // on a 1-9 scale, best compromise for speed without giving up too much on size
        try (var byteOutputStream = new ByteArrayOutputStream();
             var deflaterStream = new DeflaterOutputStream(byteOutputStream, new Deflater(compressionLevel))) {
            deflaterStream.write(dataToCompress);
            deflaterStream.close();
            return byteOutputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] uncompressBytes(@NonNull byte[] compressedData) {
        try (var byteInputStream = new ByteArrayInputStream(compressedData);
             var byteOutputStream = new ByteArrayOutputStream();
             var inflaterStream = new InflaterInputStream(byteInputStream)) {
            byte[] buffer = new byte[2048];
            int readBytes;
            while ((readBytes = inflaterStream.read(buffer)) != -1) {
                byteOutputStream.write(buffer, 0, readBytes);
            }
            return byteOutputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static class InitialCommentMarker implements Parcelable {
        public final long commentId;
        public final Date date;

        public InitialCommentMarker(long commentId) {
            this(commentId, null);
        }

        public InitialCommentMarker(Date date) {
            this(-1, date);
        }

        private InitialCommentMarker(long commentId, Date date) {
            this.commentId = commentId;
            this.date = date;
        }

        public boolean matches(long id, Date dateToMatch) {
            if (commentId >= 0 && id >= 0) {
                return commentId == id;
            }
            if (dateToMatch != null && this.date != null) {
                // We consider the date matching even if it's slightly behind this marker's date (which
                // is likely to come from a notification timestamp), because GH notification timestamps
                // tend to be ~10 seconds ahead the event that triggered them
                long dateDiff = this.date.getTime() - dateToMatch.getTime();
                return dateDiff >= 0 && dateDiff < TimeUnit.SECONDS.toMillis(30);
            }
            return false;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeLong(commentId);
            out.writeLong(date != null ? date.getTime() : -1);
        }

        public static final Parcelable.Creator<InitialCommentMarker> CREATOR =
                new Parcelable.ClassLoaderCreator<InitialCommentMarker>() {
            @Override
            public InitialCommentMarker createFromParcel(Parcel in, ClassLoader loader) {
                return createFromParcel(in);
            }

            @Override
            public InitialCommentMarker createFromParcel(Parcel in) {
                long commentId = in.readLong();
                long timeMillis = in.readLong();
                return new InitialCommentMarker(commentId,
                        timeMillis != -1 ? new Date(timeMillis) : null);
            }
            @Override
            public InitialCommentMarker[] newArray(int size) {
                return new InitialCommentMarker[size];
            }
        };
    }
}
