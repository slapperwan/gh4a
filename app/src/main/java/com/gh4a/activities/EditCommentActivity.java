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

import java.io.IOException;

public abstract class EditCommentActivity extends AppCompatActivity implements
        EditorBottomSheet.Callback {

    protected static Intent fillInIntent(Intent baseIntent, String repoOwner, String repoName,
            long id, long replyToId, String body, @AttrRes int highlightColorAttr) {
        return baseIntent.putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("id", id)
                .putExtra("reply_to", replyToId)
                .putExtra("body", body)
                .putExtra("highlight_color_attr", highlightColorAttr);
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

        if (getIntent().getLongExtra("id", 0L) != 0L) {
            ImageView saveButton = mEditorSheet.findViewById(R.id.send_button);
            saveButton.setImageResource(UiUtils.resolveDrawable(this, R.attr.saveIcon));
        }

        mEditorSheet.setCallback(this);
        if (getIntent().hasExtra("body")) {
            mEditorSheet.setCommentText(getIntent().getStringExtra("body"), false);
            getIntent().removeExtra("body");
        }

        @AttrRes int highlightColorAttr = getIntent().getIntExtra("highlight_color_attr", 0);
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
    public void onEditorSendInBackground(String body) throws IOException {
        Bundle extras = getIntent().getExtras();
        String repoOwner = extras.getString("owner");
        String repoName = extras.getString("repo");
        long id = extras.getLong("id", 0L);

        if (id == 0L) {
            createComment(repoOwner, repoName, body, extras.getLong("reply_to"));
        } else {
            editComment(repoOwner, repoName, id, body);
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

    protected abstract void createComment(String repoOwner, String repoName,
            String body, long replyToCommentId) throws IOException;
    protected abstract void editComment(String repoOwner, String repoName,
            long commentId, String body) throws IOException;
}