package com.gh4a.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.UiUtils;

public class OverviewRow extends LinearLayoutCompat implements View.OnClickListener {
    private final ImageView mIcon;
    private final TextView mLabel;
    private final View mRedirectNotice;
    private final View mProgress;

    private Intent mClickIntent;
    private boolean mToggleActive;

    public OverviewRow(Context context) {
        this(context, null);
    }

    public OverviewRow(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverviewRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        inflate(getContext(), R.layout.overview_row, this);
        mIcon = findViewById(R.id.icon);
        mLabel = findViewById(R.id.label);
        mRedirectNotice = findViewById(R.id.forward_notice);
        mProgress = findViewById(R.id.progress);

        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.RepositoryStatView, defStyle, 0);

        setText(a.getString(R.styleable.RepositoryStatView_rowText));
        setIcon(a.getDrawable(R.styleable.RepositoryStatView_rowIcon),
                a.getBoolean(R.styleable.RepositoryStatView_toggleable, false));

        a.recycle();
    }

    public void setText(CharSequence text) {
        mLabel.setText(text);
        mProgress.setVisibility(text != null ? View.GONE : View.VISIBLE);
    }

    public void setClickIntent(Intent intent) {
        mClickIntent = intent;
        if (intent != null) {
            mRedirectNotice.setVisibility(View.VISIBLE);
            setOnClickListener(this);
        } else {
            mRedirectNotice.setVisibility(View.GONE);
            setClickable(false);
        }
    }

    private void setIcon(Drawable icon, boolean toggleable) {
        if (icon == null) {
            mIcon.setVisibility(View.GONE);
            return;
        }

        Drawable wrapped = DrawableCompat.wrap(icon);
        int tintColor = UiUtils.resolveColor(getContext(),
                toggleable ? R.attr.colorAccent : R.attr.colorIconForeground);
        DrawableCompat.setTint(wrapped, tintColor);
        DrawableCompat.setTintMode(wrapped, PorterDuff.Mode.SRC_IN);

        mIcon.setImageDrawable(wrapped);
        mIcon.setVisibility(View.VISIBLE);
        mIcon.setOnClickListener(this);
        mIcon.setClickable(toggleable);
    }

    @Override
    public void onClick(View view) {
        if (view == mIcon) {
            mToggleActive = !mToggleActive;
            mIcon.setImageState(
                    mToggleActive ? new int[] { android.R.attr.state_checked } : new int[0], true);
            // TODO: callback
        } else if (mClickIntent != null) {
            getContext().startActivity(mClickIntent);
        }
    }
}
