package com.gh4a.widget;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.UiUtils;

public class EditorBottomSheet extends FrameLayout implements View.OnClickListener {

    private ViewPager mViewPager;
    private TabLayout mTabs;
    private BottomSheetBehavior mBehavior;
    private View mAdvancedEditorContainer;
    private TextView mEditor;
    private View mMarkdownButtons;

    public EditorBottomSheet(Context context) {
        super(context);
        initialize(context);
    }

    public EditorBottomSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public EditorBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        View view = View.inflate(context, R.layout.view_pager_bottom_sheet, this);

        view.findViewById(R.id.iv_advanced_editor).setOnClickListener(this);

        mViewPager = (ViewPager) view.findViewById(R.id.view_pager);

        mTabs = (TabLayout) view.findViewById(R.id.tabs);
        mTabs.setupWithViewPager(mViewPager);

        mAdvancedEditorContainer = view.findViewById(R.id.advanced_editor);
        mEditor = (TextView) view.findViewById(R.id.et_comment);
        mMarkdownButtons = view.findViewById(R.id.markdown_buttons);
    }

    public void setPagerAdapter(PagerAdapter adapter) {
        mViewPager.setAdapter(adapter);

        LinearLayout tabStrip = (LinearLayout) mTabs.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View tab = tabStrip.getChildAt(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tab.getLayoutParams();
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            lp.weight = 1;
            tab.setLayoutParams(lp);
        }
    }

    public BottomSheetBehavior getBehavior() {
        if (mBehavior == null) {
            mBehavior = BottomSheetBehavior.from(this);
        }
        return mBehavior;
    }

    public void setPagerColors(int colorAttrId) {
        mTabs.setBackgroundColor(UiUtils.resolveColor(getContext(), colorAttrId));
    }

    public void setAdvancedEditorVisible(final boolean visible) {
        if (visible) {
            toggleVisibility(true);
        } else {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    toggleVisibility(false);
                }
            }, 500);
        }

        if (visible) {
            post(new Runnable() {
                @Override
                public void run() {
                    setExpanded(true);
                }
            });
        } else {
            setExpanded(false);
        }
    }

    private void setExpanded(boolean visible) {
        getBehavior().setState(visible
                ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void toggleVisibility(boolean visible) {
        mAdvancedEditorContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        mMarkdownButtons.setVisibility(visible ? View.VISIBLE : View.GONE);
        mEditor.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_advanced_editor) {
            setAdvancedEditorVisible(mAdvancedEditorContainer.getVisibility() != View.VISIBLE);
        }
    }
}
