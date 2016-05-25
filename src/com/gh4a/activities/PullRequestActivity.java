/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.BasePagerActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.CommitCompareFragment;
import com.gh4a.fragment.PullRequestFilesFragment;
import com.gh4a.fragment.PullRequestFragment;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.widget.IssueStateTrackingFloatingActionButton;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.User;

import java.util.Locale;

public class PullRequestActivity extends BasePagerActivity implements
        View.OnClickListener, PullRequestFragment.StateChangeListener,
        PullRequestFilesFragment.CommentUpdateListener {
    private static final int REQUEST_EDIT_ISSUE = 1001;

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private boolean mIsCollaborator;

    private Issue mIssue;
    private PullRequest mPullRequest;
    private PullRequestFragment mPullRequestFragment;
    private IssueStateTrackingFloatingActionButton mEditFab;

    private ViewGroup mHeader;
    private int[] mHeaderColorAttrs;

    private static final int[] TITLES = new int[] {
        R.string.pull_request_conversation, R.string.commits, R.string.pull_request_files
    };

    private LoaderCallbacks<PullRequest> mPullRequestCallback = new LoaderCallbacks<PullRequest>(this) {
        @Override
        protected Loader<LoaderResult<PullRequest>> onCreateLoader() {
            return new PullRequestLoader(PullRequestActivity.this,
                    mRepoOwner, mRepoName, mPullRequestNumber);
        }
        @Override
        protected void onResultReady(PullRequest result) {
            mPullRequest = result;
            fillHeader();
            setContentShown(true);
            invalidateTabs();
            updateFabVisibility();
            supportInvalidateOptionsMenu();
        }
    };

    private LoaderCallbacks<Issue> mIssueCallback = new LoaderCallbacks<Issue>(this) {
        @Override
        protected Loader<LoaderResult<Issue>> onCreateLoader() {
            return new IssueLoader(PullRequestActivity.this,
                    mRepoOwner, mRepoName, mPullRequestNumber);
        }
        @Override
        protected void onResultReady(Issue result) {
            mIssue = result;
            updateFabVisibility();
            invalidateTabs();
        }
    };

    private LoaderCallbacks<Boolean> mCollaboratorCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new IsCollaboratorLoader(PullRequestActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        protected void onResultReady(Boolean result) {
            mIsCollaborator = result;
            updateFabVisibility();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();

        mHeader = (ViewGroup) inflater.inflate(R.layout.issue_header, null);
        mHeader.setClickable(false);
        mHeader.setVisibility(View.GONE);
        addHeaderView(mHeader, true);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.pull_request_title) + " #" + mPullRequestNumber);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentShown(false);

        getSupportLoaderManager().initLoader(0, null, mPullRequestCallback);
        getSupportLoaderManager().initLoader(1, null, mIssueCallback);
        getSupportLoaderManager().initLoader(2, null, mCollaboratorCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT_ISSUE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK);
                onRefresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
        mPullRequestNumber = extras.getInt(Constants.PullRequest.NUMBER);
    }

    @Override
    public void onRefresh() {
        mIssue = null;
        mPullRequest = null;
        setContentShown(false);
        mHeader.setVisibility(View.GONE);
        mHeaderColorAttrs = null;
        LoaderManager lm = getSupportLoaderManager();
        for (int i = 0; i < 3; i++) {
            lm.getLoader(i).onContentChanged();
        }
        invalidateTabs();
        super.onRefresh();
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mPullRequest != null && mIssue != null ? TITLES : null;
    }

    @Override
    protected int[] getHeaderColors() {
        return mHeaderColorAttrs;
    }

    @Override
    protected Fragment getFragment(int position) {
        if (position == 1) {
            return CommitCompareFragment.newInstance(mRepoOwner, mRepoName,
                    mPullRequest.getBase().getSha(), mPullRequest.getHead().getSha());
        } else if (position == 2) {
            return PullRequestFilesFragment.newInstance(mRepoOwner, mRepoName,
                    mPullRequestNumber, mPullRequest.getHead().getSha());
        } else {
            mPullRequestFragment = PullRequestFragment.newInstance(mPullRequest, mIssue);
            return mPullRequestFragment;
        }
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getPullRequestListActivityIntent(this, mRepoOwner, mRepoName,
                Constants.Issue.STATE_OPEN);
    }

    @Override
    public void onCommentsUpdated() {
        if (mPullRequestFragment != null) {
            mPullRequestFragment.refreshComments();
        }
    }

    @Override
    public void onPullRequestStateChanged(PullRequest newState) {
        mPullRequest = newState;
        fillHeader();
        updateFabVisibility();
        transitionHeaderToColor(mHeaderColorAttrs[0], mHeaderColorAttrs[1]);
    }

    @Override
    public void onClick(View v) {
        if (v == mEditFab) {
            Intent editIntent = new Intent(this, IssueEditActivity.class);
            editIntent.putExtra(Constants.Repository.OWNER, mRepoOwner);
            editIntent.putExtra(Constants.Repository.NAME, mRepoName);
            editIntent.putExtra(IssueEditActivity.EXTRA_ISSUE, mIssue);
            startActivityForResult(editIntent, REQUEST_EDIT_ISSUE);
        } else if (v.getId() == R.id.iv_gravatar) {
            User user = (User) v.getTag();
            Intent intent = IntentUtils.getUserActivityIntent(this, user);
            if (intent != null) {
                startActivity(intent);
            }
        }
    }

    private void updateTabRightMargin(int dimensionResId) {
        int margin = dimensionResId != 0
                ? getResources().getDimensionPixelSize(dimensionResId) : 0;

        View tabs = findViewById(R.id.tabs);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tabs.getLayoutParams();
        lp.rightMargin = margin;
        tabs.setLayoutParams(lp);
    }

    private void updateFabVisibility() {
        boolean shouldHaveFab = mIsCollaborator && mPullRequest != null && mIssue != null;
        CoordinatorLayout rootLayout = getRootLayout();

        if (shouldHaveFab && mEditFab == null) {
            mEditFab = (IssueStateTrackingFloatingActionButton)
                    getLayoutInflater().inflate(R.layout.issue_edit_fab, rootLayout, false);
            mEditFab.setOnClickListener(this);
            rootLayout.addView(mEditFab);
            updateTabRightMargin(R.dimen.mini_fab_size_with_margin);
        } else if (!shouldHaveFab && mEditFab != null) {
            rootLayout.removeView(mEditFab);
            updateTabRightMargin(0);
            mEditFab = null;
        }
        if (mEditFab != null) {
            mEditFab.setState(mPullRequest.getState());
            mEditFab.setMerged(mPullRequest.isMerged());
        }
    }

    private void fillHeader() {
        final int stateTextResId;

        if (mPullRequest.isMerged()) {
            stateTextResId = R.string.pull_request_merged;
            mHeaderColorAttrs = new int[] {
                R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark
            };
        } else if (Constants.Issue.STATE_CLOSED.equals(mPullRequest.getState())) {
            stateTextResId = R.string.closed;
            mHeaderColorAttrs = new int[] {
                R.attr.colorIssueClosed, R.attr.colorIssueClosedDark
            };
        } else {
            stateTextResId = R.string.open;
            mHeaderColorAttrs = new int[] {
                R.attr.colorIssueOpen, R.attr.colorIssueOpenDark
            };
        }

        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        tvState.setText(getString(stateTextResId).toUpperCase(Locale.getDefault()));

        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        tvTitle.setText(mPullRequest.getTitle());

        mHeader.setVisibility(View.VISIBLE);
    }
}
