package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.gh4a.R;
import com.gh4a.fragment.ReviewCommentsFragment;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.IntentUtils;

public class ReviewCommentsActivity extends FragmentContainerActivity {

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int issueNumber, boolean isPullRequest, TimelineItem.TimelineReview review) {
        return new Intent(context, ReviewCommentsActivity.class)
                .putExtra("repo_owner", repoOwner)
                .putExtra("repo_name", repoName)
                .putExtra("issue_number", issueNumber)
                .putExtra("is_pr", isPullRequest)
                .putExtra("review", review);
    }

    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private boolean mIsPullRequest;
    private TimelineItem.TimelineReview mReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.pull_request_title) + " #" + mIssueNumber + " - Review");
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("repo_owner");
        mRepoName = extras.getString("repo_name");
        mIssueNumber = extras.getInt("issue_number");
        mIsPullRequest = extras.getBoolean("is_pr");
        mReview = (TimelineItem.TimelineReview) extras.getSerializable("review");
    }

    @Override
    protected Fragment onCreateFragment() {
        return ReviewCommentsFragment.newInstance(mRepoOwner, mRepoName, mIssueNumber,
                mIsPullRequest, mReview);
    }

    @Override
    protected Intent navigateUp() {
        return PullRequestActivity.makeIntent(this, mRepoOwner, mRepoName, mIssueNumber,
                PullRequestActivity.PAGE_CONVERSATION,
                new IntentUtils.InitialCommentMarker(mReview.review.getId()));
    }
}
