package com.gh4a.widget;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gh4a.BaseActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.User;

import java.io.IOException;
import java.util.Set;

public class EditorBottomSheet extends FrameLayout implements View.OnClickListener,
        AppBarLayout.OnOffsetChangedListener {

    public interface Callback {
        @StringRes
        int getCommentEditorHintResId();

        void onSendCommentInBackground(String comment) throws IOException;

        void onCommentSent();

        BaseActivity getBaseActivity();
    }

    private TabLayout mTabs;
    private BottomSheetBehavior mBehavior;
    private View mAdvancedEditorContainer;
    private CommentEditor mBasicEditor;
    private CommentEditor mAdvancedEditor;
    private Callback mCallback;
    private MarkdownPreviewWebView mPreviewWebView;
    private ImageView mAdvancedEditorToggle;
    private int mBasicPeekHeight;
    private int mAdvancedPeekHeight;
    private int mLatestOffset;
    private View mResizingView;
    private int mTopShadowHeight;

    public EditorBottomSheet(Context context) {
        super(context);
        initialize();
    }

    public EditorBottomSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public EditorBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        mBasicPeekHeight = getResources().getDimensionPixelSize(R.dimen.comment_editor_peek_height);
        mAdvancedPeekHeight =
                getResources().getDimensionPixelSize(R.dimen.comment_advanced_editor_peek_height);
        mTopShadowHeight =
                getResources().getDimensionPixelSize(R.dimen.bottom_sheet_top_shadow_height);

        View view = View.inflate(getContext(), R.layout.editor_bottom_sheet, this);

        mAdvancedEditorToggle = (ImageView) view.findViewById(R.id.iv_advanced_editor_toggle);
        mAdvancedEditorToggle.setOnClickListener(this);

        View sendButton = view.findViewById(R.id.send_button);
        sendButton.setOnClickListener(this);

        mBasicEditor = (CommentEditor) view.findViewById(R.id.et_basic_editor);
        mBasicEditor.addTextChangedListener(
                new UiUtils.ButtonEnableTextWatcher(mBasicEditor, sendButton));
    }

    public void setCallback(Callback callback) {
        mCallback = callback;

        mBasicEditor.setCommentEditorHintResId(mCallback.getCommentEditorHintResId());
        if (mAdvancedEditor != null) {
            mAdvancedEditor.setCommentEditorHintResId(mCallback.getCommentEditorHintResId());
        }
    }

    public void setLocked(boolean locked) {
        mBasicEditor.setLocked(locked);
        if (mAdvancedEditor != null) {
            mAdvancedEditor.setLocked(locked);
        }
    }

    public void setMentionUsers(Set<User> users) {
        mBasicEditor.setMentionUsers(users);
        if (mAdvancedEditor != null) {
            mAdvancedEditor.setMentionUsers(users);
        }
    }

    public void addQuote(CharSequence text) {
        if (isInAdvancedMode()) {
            mAdvancedEditor.addQuote(text);
        }
        mBasicEditor.addQuote(text);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_advanced_editor_toggle:
                toggleAdvancedEditor();
                break;
            case R.id.send_button:
                new CommentTask(getCommentText().toString()).schedule();
                UiUtils.hideImeForView(mCallback.getBaseActivity().getCurrentFocus());
                break;
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        mLatestOffset = appBarLayout.getTotalScrollRange() + verticalOffset;
        if (mLatestOffset >= 0) {
            // Set the bottom padding to make the bottom appear as not moving while the
            // AppBarLayout pushes it down or up.
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), mLatestOffset);

            // Update peek height to keep the bottom sheet at unchanged position
            updatePeekHeight(isInAdvancedMode());
        }
    }

    public void setResizingView(View view) {
        mResizingView = view;
    }

    public boolean isExpanded() {
        return getBehavior().getState() == BottomSheetBehavior.STATE_EXPANDED
                && getBehavior().getPeekHeight() != getHeight();
    }

    private void updatePeekHeight(boolean isInAdvancedMode) {
        final int peekHeight = isInAdvancedMode ? mAdvancedPeekHeight : mBasicPeekHeight;

        if (mResizingView != null) {
            mResizingView.setPadding(mResizingView.getPaddingLeft(), mResizingView.getPaddingTop(),
                    mResizingView.getPaddingRight(), peekHeight + mLatestOffset - mTopShadowHeight);
        }

        getBehavior().setPeekHeight(peekHeight + mLatestOffset);
    }

    private void toggleAdvancedEditor() {
        final boolean visible = !isInAdvancedMode();

        if (visible) {
            setAdvancedEditorVisible(true);
        } else {
            // When leaving advanced mode delay hiding it so the bottom sheet can finish collapse
            // animation
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAdvancedEditorVisible(false);
                }
            }, 250);
        }

        // Expand bottom sheet through message queue so the animation can play.
        post(new Runnable() {
            @Override
            public void run() {
                updatePeekHeight(visible);

                getBehavior().setState(visible
                        ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    private Editable getCommentText() {
        if (isInAdvancedMode()) {
            return mAdvancedEditor.getText();
        }
        return mBasicEditor.getText();
    }

    private void setCommentText(Editable text, boolean clearFocus) {
        if (isInAdvancedMode()) {
            mAdvancedEditor.setText(text);
            if (clearFocus) {
                mAdvancedEditor.clearFocus();
            }
        } else {
            mBasicEditor.setText(text);
            if (clearFocus) {
                mBasicEditor.clearFocus();
            }
        }
    }

    private boolean isInAdvancedMode() {
        return mAdvancedEditorContainer != null
                && mAdvancedEditorContainer.getVisibility() == View.VISIBLE;
    }

    private void setAdvancedEditorVisible(boolean visible) {
        if (mAdvancedEditor == null) {
            initAdvancedMode();
        }

        mAdvancedEditorContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        mBasicEditor.setVisibility(visible ? View.GONE : View.VISIBLE);
        mTabs.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (visible) {
            mAdvancedEditor.setText(mBasicEditor.getText());
            mAdvancedEditor.requestFocus();
            mAdvancedEditor.setSelection(mAdvancedEditor.getText().length());
        } else {
            mBasicEditor.setText(mAdvancedEditor.getText());
            mBasicEditor.requestFocus();
            mBasicEditor.setSelection(mBasicEditor.getText().length());
        }

        mAdvancedEditorToggle.setImageResource(UiUtils.resolveDrawable(getContext(),
                visible ? R.attr.collapseIcon : R.attr.expandIcon));
    }

    private void initAdvancedMode() {
        ViewStub stub = (ViewStub) findViewById(R.id.advanced_editor_stub);
        mAdvancedEditorContainer = stub.inflate();

        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new AdvancedEditorPagerAdapter());

        mTabs = (TabLayout) findViewById(R.id.tabs);
        mTabs.setupWithViewPager(viewPager);

        LinearLayout tabStrip = (LinearLayout) mTabs.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View tab = tabStrip.getChildAt(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tab.getLayoutParams();
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            lp.weight = 1;
            tab.setLayoutParams(lp);
        }

        mAdvancedEditor = (CommentEditor) mAdvancedEditorContainer.findViewById(R.id.editor);
        mAdvancedEditor.addTextChangedListener(
                new UiUtils.ButtonEnableTextWatcher(mAdvancedEditor, findViewById(R.id.send_button)));

        if (mCallback != null) {
            mAdvancedEditor.setCommentEditorHintResId(mCallback.getCommentEditorHintResId());
        }
        mAdvancedEditor.setLocked(mBasicEditor.isLocked());
        mAdvancedEditor.setMentionUsers(mBasicEditor.getMentionUsers());

        MarkdownButtonsBar markdownButtons =
                (MarkdownButtonsBar) mAdvancedEditorContainer.findViewById(R.id.markdown_buttons);
        markdownButtons.setEditText(mAdvancedEditor);

        mPreviewWebView = (MarkdownPreviewWebView) findViewById(R.id.wv_preview);
        mPreviewWebView.setEditText(mAdvancedEditor);
    }

    private BottomSheetBehavior getBehavior() {
        if (mBehavior == null) {
            mBehavior = BottomSheetBehavior.from(this);
        }
        return mBehavior;
    }

    private class CommentTask extends ProgressDialogTask<Void> {
        private final String mText;

        public CommentTask(String text) {
            super(mCallback.getBaseActivity(), R.string.saving_comment);
            mText = text;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new CommentTask(mText);
        }

        @Override
        protected Void run() throws IOException {
            mCallback.onSendCommentInBackground(mText);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mCallback.onCommentSent();

            setCommentText(null, true);
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.issue_error_comment);
        }
    }

    private class AdvancedEditorPagerAdapter extends PagerAdapter {

        private final int[] TITLES = new int[] {
                R.string.edit, R.string.preview
        };

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            @IdRes
            int resId = 0;
            switch (position) {
                case 0:
                    resId = R.id.comment_container;
                    break;
                case 1:
                    resId = R.id.wv_preview;
                    break;
            }
            return container.findViewById(resId);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getContext().getString(TITLES[position]);
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }
    }
}
