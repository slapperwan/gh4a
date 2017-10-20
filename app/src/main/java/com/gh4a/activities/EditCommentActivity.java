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

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;

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
    public void onSendCommentInBackground(String body) throws IOException {
        Bundle extras = getIntent().getExtras();
        RepositoryId repoId = new RepositoryId(extras.getString("owner"), extras.getString("repo"));
        long id = extras.getLong("id", 0L);

        CommitComment comment = new CommitComment();
        comment.setId(id);
        comment.setBody(body);
        if (id == 0L) {
            createComment(repoId, comment, extras.getLong("reply_to"));
        } else {
            editComment(repoId, comment);
        }
    }

    @Override
    public void onCommentSent() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public FragmentActivity getActivity() {
        return this;
    }

    @Override
    public CoordinatorLayout getRootLayout() {
        return mRootLayout;
    }

    protected abstract void createComment(RepositoryId repoId,
            CommitComment comment, long replyToCommentId) throws IOException;
    protected abstract void editComment(RepositoryId repoId, CommitComment comment) throws IOException;
}