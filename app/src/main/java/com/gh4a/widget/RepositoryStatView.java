package com.gh4a.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TextView;

import com.gh4a.R;

public class RepositoryStatView extends LinearLayoutCompat {
    private final TextView mMainTextView;
    private final Button mActionButton;

    public RepositoryStatView(Context context) {
        this(context, null);
    }

    public RepositoryStatView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RepositoryStatView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        inflate(getContext(), R.layout.view_repository_stat, this);
        mMainTextView = findViewById(R.id.main_text_view);
        mActionButton = findViewById(R.id.action_button);

        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.RepositoryStatView, defStyle, 0);

        setMainText(a.getString(R.styleable.RepositoryStatView_mainText));
        setButtonText(a.getString(R.styleable.RepositoryStatView_buttonText));
        setButtonDrawable(a.getDrawable(R.styleable.RepositoryStatView_buttonDrawable));

        a.recycle();
    }

    public void setMainText(CharSequence text) {
        mMainTextView.setText(text);
    }

    public void setButtonText(CharSequence text) {
        mActionButton.setText(text);
    }

    public void setButtonDrawable(Drawable drawable) {
        mActionButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }

    // TODO: Click listeners
}
