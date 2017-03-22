package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.Space;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.EditorBottomSheet;

import org.eclipse.egit.github.core.RepositoryId;

import java.io.IOException;

public abstract class EditCommentActivity extends AppCompatActivity implements
        EditorBottomSheet.Callback {
    protected static Intent fillInIntent(Intent baseIntent, String repoOwner, String repoName,
            long id, String body, @AttrRes int highlightColorAttr) {
        return baseIntent.putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("id", id)
                .putExtra("body", body)
                .putExtra("highlight_color_attr", highlightColorAttr);
    }

    private EditorBottomSheet mEditorSheet;
    private CoordinatorLayout mRootLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME == R.style.DarkTheme
                ? R.style.BottomSheetDarkTheme : R.style.BottomSheetLightTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.comment_editor);

        mRootLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mEditorSheet = (EditorBottomSheet) findViewById(R.id.bottom_sheet);

        View advancedToggle = mEditorSheet.findViewById(R.id.iv_advanced_editor_toggle);
        advancedToggle.setVisibility(View.GONE);

        mEditorSheet.toggleAdvancedEditor();
        mEditorSheet.setCallback(this);
        mEditorSheet.setCommentText(getIntent().getStringExtra("body"), false);

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
    public void onSendCommentInBackground(String comment) throws IOException {
        Bundle extras = getIntent().getExtras();
        RepositoryId repoId = new RepositoryId(extras.getString("owner"), extras.getString("repo"));
        editComment(repoId, extras.getLong("id"), comment);
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

    protected abstract void editComment(RepositoryId repoId, long id, String body) throws IOException;
}
