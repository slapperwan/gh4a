package com.gh4a.widget;

import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;

import com.gh4a.resolver.LinkParser;
import com.gh4a.utils.IntentUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public class LinkSpan extends ClickableSpan {
    private final String mUrl;

    public LinkSpan(String url) {
        mUrl = url;
    }

    @Override
    public void onClick(@NonNull View widget) {
        Uri clickedUri = Uri.parse(mUrl);
        FragmentActivity activity = (FragmentActivity) widget.getContext();
        LinkParser.ParseResult result = LinkParser.parseUri(activity, clickedUri, null);
        if (result != null) {
            launchActivity(result, activity);
        } else {
            openWebPage(clickedUri, activity);
        }
    }

    private void launchActivity(LinkParser.ParseResult result, FragmentActivity activity) {
        if (result.intent != null) {
            activity.startActivity(result.intent);
        } else if (result.loadTask != null) {
            result.loadTask.execute();
        }
    }

    private void openWebPage(Uri clickedUri, FragmentActivity activity) {
        String hostname = clickedUri.getHost();
        if (hostname.endsWith("github.com") || hostname.endsWith("githubusercontent.com")) {
            IntentUtils.openInCustomTabOrBrowser(activity, clickedUri);
        } else {
            IntentUtils.launchBrowser(activity, clickedUri);
        }
    }
}
