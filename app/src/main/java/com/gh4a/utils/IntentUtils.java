package com.gh4a.utils;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.customtabs.CustomTabsIntent;
import android.widget.Toast;

import com.gh4a.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class IntentUtils {
    public static void launchBrowser(Context context, Uri uri) {
        launchBrowser(context, uri, 0);
    }

    public static void launchBrowser(Context context, Uri uri, int flags) {
        Intent intent = createBrowserIntent(context, uri);
        if (intent != null) {
            intent.addFlags(flags);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_LONG).show();
        }
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
        String pkg = CustomTabsHelper.getPackageNameToUse(activity);
        if (pkg != null) {
            int color = UiUtils.resolveColor(activity, R.attr.colorPrimary);
            CustomTabsIntent i = new CustomTabsIntent.Builder()
                    .setToolbarColor(color)
                    .build();
            i.intent.setPackage(pkg);
            i.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.launchUrl(activity, uri);
        } else {
            launchBrowser(activity, uri, Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }

    public static void share(Context context, String subject, String url) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
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

        Intent chooserIntent = Intent.createChooser(lastIntent, null);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                chooserIntents.toArray(new Intent[chooserIntents.size()]));
        return chooserIntent;
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

        public boolean matches(long id, Date date) {
            if (commentId >= 0 && id >= 0) {
                return commentId == id;
            }
            if (date != null && this.date != null) {
                return date.after(this.date);
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
