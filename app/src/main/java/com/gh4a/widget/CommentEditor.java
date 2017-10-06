package com.gh4a.widget;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.User;

import java.util.Set;

import me.thanel.markdownedit.MarkdownEdit;
import me.thanel.markdownedit.SelectionUtils;

public class CommentEditor extends AppCompatMultiAutoCompleteTextView
        implements TextView.OnEditorActionListener {
    private DropDownUserAdapter mMentionAdapter;
    private boolean mLocked;
    @StringRes
    private int mCommentEditorHintResId;
    @StringRes
    private int mLockedHintResId;

    public CommentEditor(Context context) {
        super(context);
        initialize(context);
    }

    public CommentEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public CommentEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        int inputType = (getInputType() | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
                & ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
        setInputType(inputType);

        mMentionAdapter = new DropDownUserAdapter(context);
        setAdapter(mMentionAdapter);
        setTokenizer(new UiUtils.WhitespaceTokenizer());
        setThreshold(1);

        updateLockState();

        setOnEditorActionListener(this);
    }

    public void setMentionUsers(Set<User> users) {
        mMentionAdapter.replace(users);
    }

    public Set<User> getMentionUsers() {
        return mMentionAdapter.getUnfilteredUsers();
    }

    public void setCommentEditorHintResId(@StringRes int resId) {
        mCommentEditorHintResId = resId;
        updateLockState();
    }

    @StringRes
    public int getLockedHintResId() {
        return mLockedHintResId;
    }

    public void setLocked(boolean locked, @StringRes int lockedHintResId) {
        mLocked = locked;
        mLockedHintResId = lockedHintResId;
        updateLockState();
    }

    public boolean isLocked() {
        return mLocked;
    }

    public void addQuote(CharSequence text) {
        if (mLocked) {
            return;
        }

        MarkdownEdit.addQuote(this, text);
        focusEnd();
    }

    public void addText(CharSequence text) {
        if (mLocked) {
            return;
        }

        SelectionUtils.replaceSelectedText(getText(), text);
        focusEnd();
    }

    private void focusEnd() {
        requestFocus();
        setSelection(length());
        UiUtils.showImeForView(this);
    }

    private void updateLockState() {
        setEnabled(!mLocked);
        if (mLocked) {
            setHint(mLockedHintResId != 0 ? mLockedHintResId : R.string.comment_editor_locked_hint);
        } else if (mCommentEditorHintResId != 0) {
            setHint(mCommentEditorHintResId);
        } else {
            setHint(null);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            UiUtils.hideImeForView(this);
            return true;
        }
        return false;
    }
}