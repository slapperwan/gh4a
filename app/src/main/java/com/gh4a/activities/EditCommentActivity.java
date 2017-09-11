package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.EditorBottomSheet;
import com.meisolsson.githubsdk.model.GitHubCommentBase;

import io.reactivex.Single;

public abstract class EditCommentActivity extends AppCompatActivity implements
        EditorBottomSheet.Callback {

    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_ID = "id";
    private static final String EXTRA_REPLY_TO = "reply_to";
    private static final String EXTRA_BODY = "body";
    private static final String EXTRA_HIGHLIGHT_COLOR_ATTR = "highlight_color_attr";

    protected static Intent fillInIntent(Intent baseIntent, String repoOwner, String repoName,
            long id, long replyToId, String body, @AttrRes int highlightColorAttr) {
        return baseIntent.putExtra(EXTRA_OWNER, repoOwner)
                .putExtra(EXTRA_REPO, repoName)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_REPLY_TO, replyToId)
                .putExtra(EXTRA_BODY, body)
                .putExtra(EXTRA_HIGHLIGHT_COLOR_ATTR, highlightColorAttr);
    }

    private CoordinatorLayout mRootLayout;
    protected EditorBottomSheet mEditorSheet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME == R.style.DarkTheme
                ? R.style.BottomSheetDarkTheme : R.style.BottomSheetLightTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.comment_editor);

        mRootLayout = findViewById(R.id.coordinator_layout);
        mEditorSheet = findViewById(R.id.bottom_sheet);

        if (getIntent().getLongExtra(EXTRA_ID, 0L) != 0L) {
            ImageView saveButton = mEditorSheet.findViewById(R.id.send_button);
            saveButton.setImageResource(UiUtils.resolveDrawable(this, R.attr.saveIcon));
        }

        mEditorSheet.setCallback(this);
        if (getIntent().hasExtra(EXTRA_BODY)) {
            mEditorSheet.setCommentText(getIntent().getStringExtra(EXTRA_BODY), false);
            getIntent().removeExtra(EXTRA_BODY);
        }

        @AttrRes int highlightColorAttr = getIntent().getIntExtra(EXTRA_HIGHLIGHT_COLOR_ATTR, 0);
        if (highlightColorAttr != 0) {
            mEditorSheet.setHighlightColor(highlightColorAttr);
        }

        setResult(RESULT_CANCELED);
    }

    @Override
    public int getCommentEditorHintResId() {
        return 0;
    }

    @Override
    public Single<?> onEditorDoSend(String body) {
        Bundle extras = getIntent().getExtras();
        String repoOwner = extras.getString(EXTRA_OWNER);
        String repoName = extras.getString(EXTRA_REPO);
        long id = extras.getLong(EXTRA_ID, 0L);

        if (id == 0L) {
            return createComment(repoOwner, repoName, body, extras.getLong(EXTRA_REPLY_TO));
        } else {
            return editComment(repoOwner, repoName, id, body);
        }
    }

    @Override
    public void onEditorTextSent() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public int getEditorErrorMessageResId() {
        return R.string.issue_error_comment;
    }

    @Override
    public FragmentActivity getActivity() {
        return this;
    }

    @Override
    public CoordinatorLayout getRootLayout() {
        return mRootLayout;
    }

    protected abstract Single<GitHubCommentBase> createComment(
            String repoOwner, String repoName, String body, long replyToCommentId);
    protected abstract Single<GitHubCommentBase> editComment(
            String repoOwner, String repoName, long commentId, String body);
}