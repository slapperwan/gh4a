package com.gh4a.widget;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.text.InputType;
import android.util.AttributeSet;

import com.gh4a.R;
import com.gh4a.utils.MarkdownUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.User;

import java.util.Set;

public class CommentEditor extends AppCompatMultiAutoCompleteTextView {
    private DropDownUserAdapter mMentionAdapter;
    private boolean mLocked;
    private @StringRes int mCommentEditorHintResId;

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

    public void setLocked(boolean locked) {
        mLocked = locked;
        updateLockState();
    }

    public boolean isLocked() {
        return mLocked;
    }

    public void addQuote(CharSequence text) {
        if (mLocked) {
            return;
        }

        MarkdownUtils.addQuote(this, text);

        requestFocus();
        setSelection(length());
        UiUtils.showImeForView(this);
    }

    public boolean isEmpty() {
        return getText() == null || getText().length() == 0;
    }

    private void updateLockState() {
        setEnabled(!mLocked);
        if (mLocked) {
            setHint(R.string.comment_editor_locked_hint);
        } else if (mCommentEditorHintResId != 0) {
            setHint(mCommentEditorHintResId);
        } else {
            setHint(null);
        }
    }
}