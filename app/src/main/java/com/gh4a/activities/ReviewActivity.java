package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.gh4a.R;
import com.gh4a.fragment.ReviewFragment;
import com.gh4a.utils.IntentUtils;
import com.meisolsson.githubsdk.model.Review;

public class ReviewActivity extends FragmentContainerActivity {

    private static final String EXTRA_REPO_OWNER = "repo_owner";
    private static final String EXTRA_REPO_NAME = "repo_name";
    private static final String EXTRA_ISSUE_NUMBER = "issue_number";
    private static final String EXTRA_REVIEW = "review";
    private static final String EXTRA_INITIAL_COMMENT = "initial_comment";

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int issueNumber, Review review, IntentUtils.InitialCommentMarker initialComment) {
        return new Intent(context, ReviewActivity.class)
                .putExtra(EXTRA_REPO_OWNER, repoOwner)
                .putExtra(EXTRA_REPO_NAME, repoName)
                .putExtra(EXTRA_ISSUE_NUMBER, issueNumber)
                .putExtra(EXTRA_REVIEW, review)
                .putExtra(EXTRA_INITIAL_COMMENT, initialComment);
    }

    private String mTitle;
    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private Review mReview;
    private IntentUtils.InitialCommentMarker mInitialComment;

    @Nullable
    @Override
    protected String getActionBarTitle() {
        mTitle = getString(R.string.review_title_format, mIssueNumber);
        return mTitle;
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(EXTRA_REPO_OWNER);
        mRepoName = extras.getString(EXTRA_REPO_NAME);
        mIssueNumber = extras.getInt(EXTRA_ISSUE_NUMBER);
        mReview = extras.getParcelable(EXTRA_REVIEW);
        mInitialComment = extras.getParcelable(EXTRA_INITIAL_COMMENT);
        extras.remove(EXTRA_INITIAL_COMMENT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.review_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                IntentUtils.share(this, mTitle, mReview.htmlUrl());
                return true;

            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(mReview.htmlUrl()));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Fragment onCreateFragment() {
        return ReviewFragment.newInstance(mRepoOwner, mRepoName, mIssueNumber, mReview,
                mInitialComment);
    }

    @Override
    protected Intent navigateUp() {
        return PullRequestActivity.makeIntent(this, mRepoOwner, mRepoName, mIssueNumber,
                PullRequestActivity.PAGE_CONVERSATION,
                new IntentUtils.InitialCommentMarker(mReview.id()));
    }
}
