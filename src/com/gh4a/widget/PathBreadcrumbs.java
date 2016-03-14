package com.gh4a.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.gh4a.R;

import java.util.ArrayList;
import java.util.List;

public class PathBreadcrumbs extends HorizontalScrollView implements View.OnClickListener {
    private List<String> mItems;
    private LinearLayout mChildFrame;
    private int mActive;
    private SelectionCallback mCallback;
    private LayoutInflater mInflater;

    public PathBreadcrumbs(Context context) {
        super(context);
        init();
    }

    public PathBreadcrumbs(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PathBreadcrumbs(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClipToPadding(false);
        mInflater = LayoutInflater.from(getContext());
        mItems = new ArrayList<>();
        mChildFrame = new LinearLayout(getContext());
        mChildFrame.setMinimumHeight((int) getResources().getDimension(R.dimen.breadcrumb_height));
        addView(mChildFrame, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setPath(@NonNull String path) {
        initRootCrumb();

        String[] paths = path.split("/");
        StringBuilder combinedPath = new StringBuilder();
        for (String splitPath : paths) {
            combinedPath.append(splitPath);
            addCrumb(combinedPath.toString(), splitPath);
            combinedPath.append("/");
        }
        setActive(path);
    }

    private void addCrumb(@NonNull String path, String title) {
        ViewGroup view = (ViewGroup) mInflater.inflate(R.layout.breadcrumb, mChildFrame, false);
        view.setTag(mItems.size());
        view.setOnClickListener(this);

        ActivatableStyledTextView tv = (ActivatableStyledTextView) view.getChildAt(0);
        tv.setText(title);
        tv.setActivatable(true);

        if (mChildFrame.getChildCount() > 0) {
            ViewGroup lastChild = (ViewGroup) mChildFrame.getChildAt(mChildFrame.getChildCount() - 1);
            lastChild.getChildAt(1).setVisibility(View.VISIBLE);
        }

        mChildFrame.addView(view);
        mItems.add(path);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        //RTL works fine like this
        View child = mChildFrame.getChildAt(mActive);
        if (child != null) {
            smoothScrollTo(child.getLeft(), 0);
        }
    }

    public void clear() {
        mItems.clear();
        mChildFrame.removeAllViews();
    }

    public void setCallback(SelectionCallback callback) {
        mCallback = callback;
    }

    public void setActive(String path) {
        int active = mItems.indexOf(path);
        if (active < 0) {
            return;
        }

        mActive = active;
        for (int i = 0; i < mChildFrame.getChildCount(); i++) {
            ViewGroup child = (ViewGroup) mChildFrame.getChildAt(i);
            ActivatableStyledTextView tv = (ActivatableStyledTextView) child.getChildAt(0);
            tv.setChecked(i == mActive);
        }
    }

    public int size() {
        return mItems.size();
    }

    @Override
    public void onClick(View v) {
        if (mCallback != null) {
            int index = (Integer) v.getTag();
            if (index >= 0 && index < size()) {
                mCallback.onCrumbSelection(mItems.get(index), index, size());
            }
        }
    }

    public void initRootCrumb() {
        clear();
        addCrumb("", "/");
    }

    public interface SelectionCallback {
        void onCrumbSelection(String absolutePath, int index, int count);
    }
}