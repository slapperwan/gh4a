package com.gh4a.widget;

import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;

import com.gh4a.BaseActivity;
import com.gh4a.resolver.LinkClickHandler;

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
        new LinkClickHandler(activity).handleClick(clickedUri);
    }
}
