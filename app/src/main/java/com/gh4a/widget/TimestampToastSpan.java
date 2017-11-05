package com.gh4a.widget;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;

import com.gh4a.utils.StringUtils;

import java.util.Date;

public class TimestampToastSpan extends ClickableSpan {
    private final Date mTime;

    public TimestampToastSpan(Date time) {
        mTime = time;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        // No special styling
    }

    @Override
    public void onClick(View widget) {
        Context context = widget.getContext();
        CharSequence longTime = StringUtils.formatExactTime(context, mTime);
        Toast.makeText(context, longTime, Toast.LENGTH_LONG).show();
    }
}
