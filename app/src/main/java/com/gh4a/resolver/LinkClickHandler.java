package com.gh4a.resolver;

import android.net.Uri;

import com.gh4a.BaseActivity;
import com.gh4a.utils.IntentUtils;

public class LinkClickHandler {
    private final BaseActivity mActivity;

    public LinkClickHandler(BaseActivity mActivity) {
        this.mActivity = mActivity;
    }

    public void handleClick(Uri clickedUri) {
        String uriScheme = clickedUri.getScheme();
        if (uriScheme == null || uriScheme.equals("file") || uriScheme.equals("content")) {
            // We can't do anything about relative or anchor URLs here, and there are no good reasons to
            // try to open file or content provider URIs (the former ones would raise an exception on API 24+)
            return;
        }

        LinkParser.ParseResult result = LinkParser.parseUri(mActivity, clickedUri, null);
        if (result == null) {
            IntentUtils.openInCustomTabOrBrowser(mActivity, clickedUri, mActivity.getCurrentHeaderColor());
        } else if (result.intent != null) {
            mActivity.startActivity(result.intent);
        } else if (result.loadTask != null) {
            result.loadTask.setOpenUnresolvedUriInCustomTab(mActivity.getCurrentHeaderColor());
            result.loadTask.execute();
        }
    }
}
