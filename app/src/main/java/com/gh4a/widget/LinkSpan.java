package com.gh4a.widget;

import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;

import com.gh4a.BaseActivity;
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
}
