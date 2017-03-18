package com.gh4a.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        mItems.clear();
        mChildFrame.removeAllViews();
        addCrumb("", "/");

        String[] paths = path.split("/");
        StringBuilder combinedPath = new StringBuilder();
        for (String splitPath : paths) {
            combinedPath.append(splitPath);
            addCrumb(combinedPath.toString(), splitPath);
            combinedPath.append("/");
        }

        int active = mItems.indexOf(path);
        if (active >= 0) {
            mActive = active;
            for (int i = 0; i < mChildFrame.getChildCount(); i++) {
                ViewGroup child = (ViewGroup) mChildFrame.getChildAt(i);
                TextView tv = (TextView) child.getChildAt(0);
                tv.setActivated(i == mActive);
            }
        }
    }

    private void addCrumb(@NonNull String path, String title) {
        ViewGroup view = (ViewGroup) mInflater.inflate(R.layout.breadcrumb, mChildFrame, false);
        view.setTag(mItems.size());
        view.setOnClickListener(this);

        TextView tv = (TextView) view.getChildAt(0);
        tv.setText(title);

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

    public void setCallback(SelectionCallback callback) {
        mCallback = callback;
    }

    @Override
    public void onClick(View v) {
        if (mCallback != null) {
            int index = (Integer) v.getTag();
            int size = mItems.size();
            if (index >= 0 && index < size) {
                mCallback.onCrumbSelection(mItems.get(index), index, size);
            }
        }
    }

    public interface SelectionCallback {
        void onCrumbSelection(String absolutePath, int index, int count);
    }
}