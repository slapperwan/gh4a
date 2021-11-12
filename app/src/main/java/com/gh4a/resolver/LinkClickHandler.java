package com.gh4a.resolver;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.gh4a.BaseActivity;
import com.gh4a.R;
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

        if (uriScheme.equals("mailto")) {
            tryStartViewIntent(clickedUri);
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

    private void tryStartViewIntent(Uri clickedUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, clickedUri);
            mActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mActivity, R.string.link_not_openable, Toast.LENGTH_SHORT).show();
        }
    }
}
