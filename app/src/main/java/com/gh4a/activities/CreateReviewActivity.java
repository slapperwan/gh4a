package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.widget.EditorBottomSheet;
import com.gh4a.widget.StyleableTextView;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.request.pull_request.CreateReview;
import com.meisolsson.githubsdk.model.request.pull_request.SubmitReview;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;

import io.reactivex.Single;
import retrofit2.Response;

public class CreateReviewActivity extends AppCompatActivity implements
        EditorBottomSheet.Callback {
    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_PR_NUMBER = "pr_number";
    private static final String EXTRA_DRAFT_PR = "draft_pr";
    private static final String EXTRA_PENDING_REVIEW = "pending_review";

    protected static Intent makeIntent(Context context, String repoOwner, String repoName,
            int pullRequestNumber, boolean isDraftPR, Review pendingReview) {
        return new Intent(context, CreateReviewActivity.class)
                .putExtra(EXTRA_OWNER, repoOwner)
                .putExtra(EXTRA_REPO, repoName)
                .putExtra(EXTRA_PR_NUMBER, pullRequestNumber)
                .putExtra(EXTRA_DRAFT_PR, isDraftPR)
                .putExtra(EXTRA_PENDING_REVIEW, pendingReview);
    }

    private ArrayAdapter<ReviewEventDesc> mReviewEventAdapter;
    private CoordinatorLayout mRootLayout;
    private EditorBottomSheet mEditorSheet;
    private Spinner mReviewEventSpinner;

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private Review mPendingReview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.BottomSheetTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.comment_editor);

        Bundle extras = getIntent().getExtras();
        mRepoOwner = extras.getString(EXTRA_OWNER);
        mRepoName = extras.getString(EXTRA_REPO);
        mPullRequestNumber = extras.getInt(EXTRA_PR_NUMBER);
        mPendingReview = (Review) extras.getSerializable(EXTRA_PENDING_REVIEW);

        mRootLayout = findViewById(R.id.coordinator_layout);
        mEditorSheet = findViewById(R.id.bottom_sheet);

        View header = getLayoutInflater().inflate(R.layout.create_review_header, null);
        mEditorSheet.addHeaderView(header);
        mEditorSheet.setAllowEmpty(true);
        boolean isDraftPR = extras.getBoolean(EXTRA_DRAFT_PR);
        if (isDraftPR) {
            mEditorSheet.setHighlightColor(R.attr.colorPullRequestDraft);
        }

        mReviewEventAdapter = new ArrayAdapter<>(this, R.layout.spinner_item);
        mReviewEventAdapter.add(new ReviewEventDesc(R.string.pull_request_review_event_comment,
                CreateReview.Event.Comment, SubmitReview.Event.Comment));
        mReviewEventAdapter.add(new ReviewEventDesc(R.string.pull_request_review_event_approve,
                CreateReview.Event.Approve, SubmitReview.Event.Approve));
        mReviewEventAdapter.add(new ReviewEventDesc(R.string.pull_request_review_event_request_changes,
                CreateReview.Event.RequestChanges, SubmitReview.Event.RequestChanges));

        mReviewEventSpinner = header.findViewById(R.id.pull_request_review_event);
        mReviewEventSpinner.setAdapter(mReviewEventAdapter);

        StyleableTextView titleView = header.findViewById(R.id.review_dialog_title);
        titleView.setText(getString(R.string.pull_request_review_dialog_title, mPullRequestNumber));

        mEditorSheet.setCallback(this);
        if (mPendingReview != null && savedInstanceState == null) {
            mEditorSheet.setCommentText(mPendingReview.body(), false);
        }

        setResult(RESULT_CANCELED);
    }

    @Override
    public int getCommentEditorHintResId() {
        return 0;
    }

    @Override
    public Single<?> onEditorDoSend(String body) {
        int position = mReviewEventSpinner.getSelectedItemPosition();
        ReviewEventDesc desc = mReviewEventAdapter.getItem(position);

        PullRequestReviewService service = ServiceFactory.get(PullRequestReviewService.class, false);
        final Single<Response<Review>> resultSingle;

        if (mPendingReview == null) {
            CreateReview request = CreateReview.builder()
                    .body(body)
                    .event(desc.mCreateEvent)
                    .build();
            resultSingle = service.createReview(mRepoOwner, mRepoName, mPullRequestNumber, request);
        } else {
            SubmitReview request = SubmitReview.builder()
                    .body(body)
                    .event(desc.mSubmitEvent)
                    .build();
            resultSingle = service.submitReview(mRepoOwner, mRepoName,
                    mPullRequestNumber, mPendingReview.id(), request);
        }
        return resultSingle.map(ApiHelpers::throwOnFailure);
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
        public final CreateReview.Event mCreateEvent;
        public final SubmitReview.Event mSubmitEvent;

        public ReviewEventDesc(@StringRes int textResId, CreateReview.Event createEvent,
                SubmitReview.Event submitEvent) {
            mTextResId = textResId;
            mCreateEvent = createEvent;
            mSubmitEvent = submitEvent;
        }

        @Override
        public String toString() {
            return getString(mTextResId);
        }
    }
}
