package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import com.gh4a.R;
import com.gh4a.fragment.ReviewFragment;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.Review;

public class ReviewActivity extends FragmentContainerActivity {

    private String mTitle;

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int issueNumber, Review review, IntentUtils.InitialCommentMarker initialComment) {
        return new Intent(context, ReviewActivity.class)
                .putExtra("repo_owner", repoOwner)
                .putExtra("repo_name", repoName)
                .putExtra("issue_number", issueNumber)
                .putExtra("review", review)
                .putExtra("initial_comment", initialComment);
    }

    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private Review mReview;
    private IntentUtils.InitialCommentMarker mInitialComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        mTitle = getResources().getString(R.string.pull_request_title) + " #" + mIssueNumber + " - Review";
        actionBar.setTitle(mTitle);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("repo_owner");
        mRepoName = extras.getString("repo_name");
        mIssueNumber = extras.getInt("issue_number");
        mReview = (Review) extras.getSerializable("review");
        mInitialComment = extras.getParcelable("initial_comment");
        extras.remove("initial_comment");
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
                IntentUtils.share(this, mTitle, mReview.getHtmlUrl());
                return true;

            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(mReview.getHtmlUrl()));
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
                new IntentUtils.InitialCommentMarker(mReview.getId()));
    }
}
