package com.gh4a.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gh4a.R;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.User;

import java.util.Set;

import io.reactivex.Single;
import retrofit2.Response;

public class EditorBottomSheet extends FrameLayout implements View.OnClickListener,
        View.OnTouchListener, AppBarLayout.OnOffsetChangedListener {

    public interface Callback {
        @StringRes int getCommentEditorHintResId();
        @StringRes int getEditorErrorMessageResId();
        Single<?> onEditorDoSend(String comment);
        void onEditorTextSent();
        FragmentActivity getActivity();
        CoordinatorLayout getRootLayout();
    }

    public interface Listener {
        void onToggleAdvancedMode(boolean advancedMode);
        void onScrollingInBasicEditor(boolean scrolling);
    }

    private TabLayout mTabs;
    private ToggleableBottomSheetBehavior mBehavior;
    private View mAdvancedEditorContainer;
    private CommentEditor mBasicEditor;
    private CommentEditor mAdvancedEditor;
    private MarkdownButtonsBar mMarkdownButtons;
    private ImageView mAdvancedEditorToggle;
    private Listener mListener;
    private NestedScrollView mBasicEditorScrollView;
    private ViewGroup mContainer;

    private Callback mCallback;
    private View mResizingView;
    private @ColorInt int mHighlightColor = Color.TRANSPARENT;
    private boolean mIsCollapsible;

    private int mBasicPeekHeight;
    private int mAdvancedPeekHeight;
    private int mLatestOffset;
    private int mTopShadowHeight;

    private final BottomSheetBehavior.BottomSheetCallback mBehaviorCallback =
            new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (isInAdvancedMode()
                    && mIsCollapsible
                    && newState == BottomSheetBehavior.STATE_COLLAPSED) {
                setAdvancedEditorVisible(false);
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    public EditorBottomSheet(Context context) {
        super(context);
        initialize(null);
    }

    public EditorBottomSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs);
    }

    public EditorBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs);
    }

    private void initialize(AttributeSet attrs) {
        final Resources res = getResources();
        mBasicPeekHeight = res.getDimensionPixelSize(R.dimen.comment_editor_peek_height);
        mAdvancedPeekHeight = res.getDimensionPixelSize(R.dimen.comment_advanced_editor_peek_height);
        mTopShadowHeight = res.getDimensionPixelSize(R.dimen.bottom_sheet_top_shadow_height);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EditorBottomSheet);
            mIsCollapsible = a.getBoolean(R.styleable.EditorBottomSheet_collapsible, true);
            a.recycle();
        } else {
            mIsCollapsible = true;
        }

        View view = View.inflate(getContext(), R.layout.editor_bottom_sheet, this);

        mAdvancedEditorToggle = view.findViewById(R.id.iv_advanced_editor_toggle);
        mAdvancedEditorToggle.setOnClickListener(this);
        if (!mIsCollapsible) {
            mAdvancedEditorToggle.setVisibility(View.GONE);
        }

        View sendButton = view.findViewById(R.id.send_button);
        sendButton.setOnClickListener(this);

        mBasicEditor = view.findViewById(R.id.et_basic_editor);
        mBasicEditor.addTextChangedListener(
                new UiUtils.ButtonEnableTextWatcher(mBasicEditor, sendButton));
        mBasicEditor.setOnTouchListener(this);

        mBasicEditorScrollView = view.findViewById(R.id.basic_editor_scroll);
        mBasicEditorScrollView.setOnTouchListener(this);

        mContainer = view.findViewById(R.id.bottom_sheet_header_container);

        post(() -> {
            getBehavior().setBottomSheetCallback(mBehaviorCallback);
            resetPeekHeight(0);

            if (!mIsCollapsible && !isInAdvancedMode()) {
                setAdvancedMode(true);
            }
        });
    }

    public void addHeaderView(View view) {
        mContainer.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mContainer.setVisibility(View.VISIBLE);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
        updateHint();
    }

    public void updateHint() {
        if (mCallback == null) {
            return;
        }
        mBasicEditor.setCommentEditorHintResId(mCallback.getCommentEditorHintResId());
        if (mAdvancedEditor != null) {
            mAdvancedEditor.setCommentEditorHintResId(mCallback.getCommentEditorHintResId());
        }
    }

    public void setLocked(boolean locked, @StringRes int lockedHintResId) {
        mBasicEditor.setLocked(locked, lockedHintResId);
        if (mAdvancedEditor != null) {
            mAdvancedEditor.setLocked(locked, lockedHintResId);
        }
        mAdvancedEditorToggle.setVisibility(locked ? View.GONE : View.VISIBLE);
        setAdvancedMode(false);
    }

    public void setMentionUsers(Set<User> users) {
        mBasicEditor.setMentionUsers(users);
        if (mAdvancedEditor != null) {
            mAdvancedEditor.setMentionUsers(users);
        }
    }

    public void setHighlightColor(@AttrRes int colorAttrId) {
        mHighlightColor = UiUtils.resolveColor(getContext(), colorAttrId);
        if (mMarkdownButtons != null) {
            mMarkdownButtons.setButtonsBackgroundColor(mHighlightColor);
        }
     }

    public void addQuote(CharSequence text) {
        if (isInAdvancedMode()) {
            mAdvancedEditor.addQuote(text);
        }
        mBasicEditor.addQuote(text);
    }

    public void addText(CharSequence text) {
        if (isInAdvancedMode()) {
            mAdvancedEditor.addText(text);
        }
        mBasicEditor.addText(text);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP) {
            return false;
        }

        boolean down = action == MotionEvent.ACTION_DOWN;
        if (view != mBasicEditor && view != mBasicEditorScrollView) {
            getBehavior().setEnabled(!down);
        }
        if ((view == mBasicEditor || view == mBasicEditorScrollView) && mListener != null) {
            mListener.onScrollingInBasicEditor(down);
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_advanced_editor_toggle:
                setAdvancedMode(!isInAdvancedMode());
                break;
            case R.id.send_button:
                send(getCommentText().toString());
                UiUtils.hideImeForView(mCallback.getActivity().getCurrentFocus());
                break;
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        mLatestOffset = appBarLayout.getTotalScrollRange() + verticalOffset;
        if (mLatestOffset >= 0) {
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

    public void resetPeekHeight(int totalScrollRange) {
        mLatestOffset = totalScrollRange;
        updatePeekHeight(isInAdvancedMode());
    }

    private void updatePeekHeight(boolean isInAdvancedMode) {
        // Set the bottom padding to make the bottom appear as not moving while the
        // AppBarLayout pushes it down or up.
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), mLatestOffset);

        final int peekHeight = isInAdvancedMode ? mAdvancedPeekHeight : mBasicPeekHeight;

        if (mResizingView != null) {
            int paddingBottom = 0;
            if (getVisibility() != View.GONE) {
                paddingBottom = peekHeight + mLatestOffset - mTopShadowHeight;
            }
            mResizingView.setPadding(mResizingView.getPaddingLeft(), mResizingView.getPaddingTop(),
                    mResizingView.getPaddingRight(), paddingBottom);
        }

        // Update peek height to keep the bottom sheet at unchanged position
        getBehavior().setPeekHeight(peekHeight + mLatestOffset);
    }

    public void setAdvancedMode(final boolean visible) {
        if (mAdvancedEditor == null) {
            if (!visible) {
                return;
            }
            initAdvancedMode();
        }

        if (visible) {
            // Showing editor has to be done before updating peek height so when it expands the
            // user can immediately see the content
            setAdvancedEditorVisible(true);
        }

        // Expand bottom sheet through message queue so the animation can play.
        post(() -> {
            updatePeekHeight(visible);

            getBehavior().setState(visible
                    ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
        });
    }

    public boolean isInAdvancedMode() {
        return mAdvancedEditorContainer != null
                && mAdvancedEditorContainer.getVisibility() == View.VISIBLE;
    }

    public void setCommentText(CharSequence text, boolean clearFocus) {
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

    private Editable getCommentText() {
        if (isInAdvancedMode()) {
            return mAdvancedEditor.getText();
        }
        return mBasicEditor.getText();
    }

    private void setAdvancedEditorVisible(boolean visible) {
        mAdvancedEditorContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        mBasicEditorScrollView.setVisibility(visible ? View.GONE : View.VISIBLE);
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

        if (mListener != null) {
            mListener.onToggleAdvancedMode(visible);
        }
    }

    private void initAdvancedMode() {
        ViewStub stub = findViewById(R.id.advanced_editor_stub);
        mAdvancedEditorContainer = stub.inflate();

        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new AdvancedEditorPagerAdapter(getContext()));

        mTabs = findViewById(R.id.tabs);
        mTabs.setupWithViewPager(viewPager);

        LinearLayout tabStrip = (LinearLayout) mTabs.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View tab = tabStrip.getChildAt(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tab.getLayoutParams();
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            lp.weight = 1;
            tab.setLayoutParams(lp);
        }

        mAdvancedEditor = mAdvancedEditorContainer.findViewById(R.id.editor);
        mAdvancedEditor.addTextChangedListener(new UiUtils.ButtonEnableTextWatcher(mAdvancedEditor,
                findViewById(R.id.send_button)));

        updateHint();
        mAdvancedEditor.setLocked(mBasicEditor.isLocked(), mBasicEditor.getLockedHintResId());
        mAdvancedEditor.setMentionUsers(mBasicEditor.getMentionUsers());

        mMarkdownButtons =
                mAdvancedEditorContainer.findViewById(R.id.markdown_buttons);
        mMarkdownButtons.setEditText(mAdvancedEditor);
        if (mHighlightColor != Color.TRANSPARENT) {
            mMarkdownButtons.setButtonsBackgroundColor(mHighlightColor);
        }

        MarkdownPreviewWebView previewWebView = findViewById(R.id.preview);
        previewWebView.setEditText(mAdvancedEditor);

        mMarkdownButtons.setBottomSheetBehavior(getBehavior());
        mAdvancedEditorContainer.findViewById(R.id.editor_scroller).setOnTouchListener(this);
        previewWebView.setOnTouchListener(this);
        mAdvancedEditor.setOnTouchListener(this);
        viewPager.setOnTouchListener(this);
    }

    private ToggleableBottomSheetBehavior getBehavior() {
        if (mBehavior == null) {
            mBehavior = (ToggleableBottomSheetBehavior) BottomSheetBehavior.from(this);
        }
        return mBehavior;
    }

    private void send(String comment) {
        FragmentActivity activity = mCallback.getActivity();
        CoordinatorLayout rootLayout = mCallback.getRootLayout();

        mCallback.onEditorDoSend(comment)
                .compose(RxUtils.wrapForBackgroundTask(activity, rootLayout,
                        R.string.saving_comment, R.string.issue_error_comment))
                .subscribe(result -> {
                    mCallback.onEditorTextSent();
                    setCommentText(null, true);
                    setAdvancedMode(false);
                }, error -> {});
    }

    private static class AdvancedEditorPagerAdapter extends PagerAdapter {
        private static final int[] TITLES = new int[] {
            R.string.edit, R.string.preview
        };

        private final Context mContext;

        public AdvancedEditorPagerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            @IdRes
            int resId = 0;
            switch (position) {
                case 0:
                    resId = R.id.comment_container;
                    break;
                case 1:
                    resId = R.id.preview;
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
            return mContext.getString(TITLES[position]);
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState);

        ss.isInAdvancedMode = isInAdvancedMode();

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setAdvancedMode(ss.isInAdvancedMode);
    }

    private static class SavedState extends BaseSavedState {
        boolean isInAdvancedMode;

        public SavedState(Parcel source) {
            super(source);
            isInAdvancedMode = source.readByte() == 1;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (isInAdvancedMode ? 1 : 0));
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
