package com.gh4a.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.TooltipCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.UiUtils;

public class OverviewRow extends LinearLayoutCompat implements View.OnClickListener {
    public interface OnIconClickListener {
        void onIconClick(OverviewRow row);
    }

    private final ImageView mIcon;
    private final TextView mLabel;
    private final View mRedirectNotice;
    private final View mProgress;

    private Intent mClickIntent;
    private OnIconClickListener mIconClickListener;

    private String mActionHintChecked;
    private String mActionHintUnchecked;

    public OverviewRow(Context context) {
        this(context, null);
    }

    public OverviewRow(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.overviewRowStyle);
    }

    public OverviewRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        inflate(getContext(), R.layout.overview_row, this);
        mIcon = findViewById(R.id.icon);
        mLabel = findViewById(R.id.label);
        mRedirectNotice = findViewById(R.id.forward_notice);
        mProgress = findViewById(R.id.progress);

        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.OverviewRow, defStyle, 0);

        setText(a.getString(R.styleable.OverviewRow_rowText));
        setIcon(a.getDrawable(R.styleable.OverviewRow_rowIcon));
        setIconClickListener(null);

        mActionHintChecked = a.getString(R.styleable.OverviewRow_rowIconActionHintOn);
        mActionHintUnchecked = a.getString(R.styleable.OverviewRow_rowIconActionHintOff);

        a.recycle();
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
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

    public void setIconClickListener(OnIconClickListener l) {
        mIcon.setOnClickListener(l != null ? this : null);
        mIcon.setEnabled(l != null);
        mIconClickListener = l;
        updateIconTint();
    }

    public void setToggleState(boolean active) {
        mIcon.setImageState(
                active ? new int[] { android.R.attr.state_checked } : new int[0], true);
        TooltipCompat.setTooltipText(mIcon, active ? mActionHintChecked : mActionHintUnchecked);
    }

    private void setIcon(Drawable icon) {
        if (icon == null) {
            mIcon.setVisibility(View.GONE);
        } else {
            Drawable wrapped = DrawableCompat.wrap(icon).mutate();
            mIcon.setImageDrawable(wrapped);
            mIcon.setVisibility(View.VISIBLE);
            updateIconTint();
        }
    }

    private void updateIconTint() {
        Drawable drawable = mIcon.getDrawable();
        if (drawable == null) {
            return;
        }
        int tintColor = UiUtils.resolveColor(getContext(),
                mIconClickListener != null ? R.attr.colorAccent : R.attr.colorIconForeground);
        DrawableCompat.setTint(drawable, tintColor);
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void onClick(View view) {
        if (view == mIcon) {
            mIconClickListener.onIconClick(this);
        } else if (mClickIntent != null) {
            getContext().startActivity(mClickIntent);
        }
    }
}
