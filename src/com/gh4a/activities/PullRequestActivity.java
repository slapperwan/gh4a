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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.CommitCompareFragment;
import com.gh4a.fragment.PullRequestFilesFragment;
import com.gh4a.fragment.PullRequestFragment;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.PullRequest;

public class PullRequestActivity extends LoadingFragmentPagerActivity implements
        PullRequestFilesFragment.CommentUpdateListener {
    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;

    private PullRequest mPullRequest;
    private PullRequestFragment mPullRequestFragment;

    private static final int[] TITLES = new int[] {
        R.string.pull_request_conversation, R.string.commits, R.string.pull_request_files
    };

    private LoaderCallbacks<PullRequest> mPullRequestCallback = new LoaderCallbacks<PullRequest>() {
        @Override
        public Loader<LoaderResult<PullRequest>> onCreateLoader(int id, Bundle args) {
            return new PullRequestLoader(PullRequestActivity.this,
                    mRepoOwner, mRepoName, mPullRequestNumber);
        }
        @Override
        public void onResultReady(LoaderResult<PullRequest> result) {
            boolean success = !result.handleError(PullRequestActivity.this);
            if (success) {
                mPullRequest = result.getData();
                setTabsEnabled(true);
            }
            setContentEmpty(!success);
            setContentShown(true);
            invalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mPullRequestNumber = data.getInt(Constants.PullRequest.NUMBER);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.pull_request_title) + " #" + mPullRequestNumber);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentShown(false);
        setTabsEnabled(false);

        getSupportLoaderManager().initLoader(0, null, mPullRequestCallback);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        if (position == 1) {
            return CommitCompareFragment.newInstance(mRepoOwner, mRepoName,
                    mPullRequest.getBase().getSha(), mPullRequest.getHead().getSha());
        } else if (position == 2) {
            return PullRequestFilesFragment.newInstance(mRepoOwner, mRepoName, mPullRequestNumber);
        } else {
            mPullRequestFragment = PullRequestFragment.newInstance(mPullRequest);
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
}
