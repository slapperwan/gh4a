package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.widget.EditorBottomSheet;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

public class CreateReviewActivity extends AppCompatActivity implements
        EditorBottomSheet.Callback {
    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_PR_NUMBER = "pr_number";
    private static final String EXTRA_BODY = "body";

    protected static Intent makeIntent(Context context, String repoOwner, String repoName,
            int pullRequestNumber, String body) {
        return new Intent(context, CreateReviewActivity.class)
                .putExtra(EXTRA_OWNER, repoOwner)
                .putExtra(EXTRA_REPO, repoName)
                .putExtra(EXTRA_PR_NUMBER, pullRequestNumber)
                .putExtra(EXTRA_BODY, body);
    }

    private ArrayAdapter<ReviewEventDesc> mReviewEventAdapter;
    private CoordinatorLayout mRootLayout;
    private EditorBottomSheet mEditorSheet;
    private Spinner mReviewEventSpinner;

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME == R.style.DarkTheme
                ? R.style.BottomSheetDarkTheme : R.style.BottomSheetLightTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.comment_editor);

        Bundle extras = getIntent().getExtras();
        mRepoOwner = extras.getString(EXTRA_OWNER);
        mRepoName = extras.getString(EXTRA_REPO);
        mPullRequestNumber = extras.getInt(EXTRA_PR_NUMBER);

        mRootLayout = findViewById(R.id.coordinator_layout);
        mEditorSheet = findViewById(R.id.bottom_sheet);

        View header = getLayoutInflater().inflate(R.layout.create_review_header, null);
        mEditorSheet.addHeaderView(header);

        mReviewEventAdapter = new ArrayAdapter<>(this, R.layout.spinner_item);
        mReviewEventAdapter.add(new ReviewEventDesc(R.string.pull_request_review_event_comment,
                PullRequestService.REVIEW_EVENT_COMMENT));
        mReviewEventAdapter.add(new ReviewEventDesc(R.string.pull_request_review_event_approve,
                PullRequestService.REVIEW_EVENT_APPROVE));
        mReviewEventAdapter.add(new ReviewEventDesc(R.string.pull_request_review_event_request_changes,
                PullRequestService.REVIEW_EVENT_REQUEST_CHANGES));

        mReviewEventSpinner = header.findViewById(R.id.pull_request_review_event);
        mReviewEventSpinner.setAdapter(mReviewEventAdapter);

        StyleableTextView titleView = header.findViewById(R.id.review_dialog_title);
        titleView.setText(getString(R.string.pull_request_review_dialog_title, mPullRequestNumber));

        mEditorSheet.setCallback(this);
        if (getIntent().hasExtra("body")) {
            mEditorSheet.setCommentText(getIntent().getStringExtra("body"), false);
            getIntent().removeExtra("body");
        }

        setResult(RESULT_CANCELED);
    }

    @Override
    public int getCommentEditorHintResId() {
        return 0;
    }

    @Override
    public void onEditorSendInBackground(String body) throws IOException {
        int position = mReviewEventSpinner.getSelectedItemPosition();
        @SuppressWarnings("ConstantConditions")
        String eventType = mReviewEventAdapter.getItem(position).mEventType;

        PullRequestService pullService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
        pullService.createReview(repoId, mPullRequestNumber, eventType, body);
    }

    @Override
    public void onEditorTextSent() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public int getEditorErrorMessageResId() {
        return R.string.review_create_error;
    }

    @Override
    public FragmentActivity getActivity() {
        return this;
    }

    @Override
    public CoordinatorLayout getRootLayout() {
        return mRootLayout;
    }

    private class ReviewEventDesc {
        @StringRes
        public final int mTextResId;
        public final String mEventType;

        public ReviewEventDesc(@StringRes int textResId, String eventType) {
            mTextResId = textResId;
            mEventType = eventType;
        }

        @Override
        public String toString() {
            return getString(mTextResId);
        }
    }
}
