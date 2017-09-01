package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.widget.EditorBottomSheet;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

public class ReviewChangesActivity extends AppCompatActivity implements EditorBottomSheet.Callback {

    public static Intent makeEditIntent(Context context, String repoOwner, String repoName,
            int prNumber, String body) {
        return makeIntent(context, repoOwner, repoName, prNumber)
                .putExtra("body", body);
    }


    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int prNumber) {
        return new Intent(context, ReviewChangesActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("pr", prNumber);
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
    public void onSendCommentInBackground(String body) throws IOException {
        Bundle extras = getIntent().getExtras();
        RepositoryId repoId = new RepositoryId(extras.getString("owner"), extras.getString("repo"));

        PullRequestService service =
                (PullRequestService) Gh4Application.get().getService(Gh4Application.PULL_SERVICE);

        service.createReview(repoId, extras.getInt("pr"), body);
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
}