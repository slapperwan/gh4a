package com.gh4a.widget;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.resolver.LinkParser;
import com.gh4a.utils.IntentUtils;

import androidx.annotation.NonNull;

public class LinkSpan extends ClickableSpan {
    private final String mUrl;

    public LinkSpan(String url) {
        mUrl = url;
    }

    @Override
    public void onClick(@NonNull View widget) {
        Uri clickedUri = Uri.parse(mUrl);
        BaseActivity activity = (BaseActivity) widget.getContext();
        handleClick(clickedUri, activity);
    }

    public static void handleClick(Uri clickedUri, BaseActivity activity) {
        String uriScheme = clickedUri.getScheme();
        if (uriScheme == null || uriScheme.equals("file") || uriScheme.equals("content")) {
            // We can't do anything about relative or anchor URLs here, and there are no good reasons to
            // try to open file or content provider URIs (the former ones would raise an exception on API 24+)
            return;
        }

        if (uriScheme.equals("mailto")) {
            tryOpenViewerIntent(activity, clickedUri);
            return;
        }

        LinkParser.ParseResult result = LinkParser.parseUri(activity, clickedUri, null);
        if (result == null) {
            IntentUtils.openInCustomTabOrBrowser(activity, clickedUri, activity.getCurrentHeaderColor());
        } else if (result.intent != null) {
            activity.startActivity(result.intent);
        } else if (result.loadTask != null) {
            result.loadTask.setOpenUnresolvedUriInCustomTab(activity.getCurrentHeaderColor());
            result.loadTask.execute();
        }
    }

    private static void tryOpenViewerIntent(Context context, Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.link_not_openable, Toast.LENGTH_SHORT).show();
        }
    }
}
