package com.gh4a.widget;

import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;

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
        var activity = (FragmentActivity) widget.getContext();
        IntentUtils.openLinkInternallyOrExternally(activity, clickedUri);
    }
}
