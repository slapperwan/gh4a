package com.gh4a.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SearchView extends androidx.appcompat.widget.SearchView {
    private boolean mIsExpanded = false;

    public SearchView(@NonNull Context context) {
        super(context);
    }

    public SearchView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onActionViewCollapsed() {
        mIsExpanded = false;
        super.onActionViewCollapsed();
    }

    @Override
    public void onActionViewExpanded() {
        super.onActionViewExpanded();
        mIsExpanded = true;
    }

    public boolean isExpanded() {
        return mIsExpanded;
    }
}
