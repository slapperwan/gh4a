package com.gh4a.widget;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import com.gh4a.utils.UiUtils;

public abstract class IntentSpan extends ClickableSpan {
    private final Context mContext;

    public IntentSpan(Context context) {
        mContext = context;
    }

    @Override
    public void onClick(View view) {
        Intent intent = getIntent();
        if (intent != null) {
            mContext.startActivity(intent);
        }
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setUnderlineText(true);
        ds.setColor(UiUtils.resolveColor(mContext, android.R.attr.textColorLink));
    }

    protected abstract Intent getIntent();
}
