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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.gh4a.BaseFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.CommitFragment;
import com.gh4a.fragment.CommitNoteFragment;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.CommitLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.widget.BottomSheetCompatibleScrollingViewBehavior;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.git.GitComment;

import java.util.List;

public class CommitActivity extends BaseFragmentPagerActivity implements
        CommitFragment.CommentUpdateListener, CommitNoteFragment.CommentUpdateListener {
    public static Intent makeIntent(Context context, String repoOwner, String repoName, String sha) {
        return makeIntent(context, repoOwner, repoName, -1, sha, null);
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int pullRequestNumber, String sha) {
        return makeIntent(context, repoOwner, repoName, pullRequestNumber, sha, null);
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String sha, IntentUtils.InitialCommentMarker initialComment) {
        return makeIntent(context, repoOwner, repoName, -1, sha, initialComment);
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int pullRequestNumber, String sha, IntentUtils.InitialCommentMarker initialComment) {
        return new Intent(context, CommitActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("pr", pullRequestNumber)
                .putExtra("sha", sha)
                .putExtra("initial_comment", initialComment);
    }

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private int mPullRequestNumber;

    private Commit mCommit;
    private List<GitComment> mComments;
    private IntentUtils.InitialCommentMarker mInitialComment;

    private static final int[] TITLES = new int[] {
        R.string.commit, R.string.issue_comments
    };

    private final LoaderCallbacks<Commit> mCommitCallback =
            new LoaderCallbacks<Commit>(this) {
        @Override
        protected Loader<LoaderResult<Commit>> onCreateLoader() {
            return new CommitLoader(CommitActivity.this, mRepoOwner, mRepoName, mObjectSha);
        }

        @Override
        protected void onResultReady(Commit result) {
            mCommit = result;
            showContentIfReady();
        }
    };

    private final LoaderCallbacks<List<GitComment>> mCommentCallback =
            new LoaderCallbacks<List<GitComment>>(this) {
        @Override
        protected Loader<LoaderResult<List<GitComment>>> onCreateLoader() {
            return new CommitCommentListLoader(CommitActivity.this, mRepoOwner, mRepoName,
                    mObjectSha, true, true);
        }

        @Override
        protected void onResultReady(List<GitComment> result) {
            mComments = result;
            if (result.isEmpty()) {
                mInitialComment = null;
            }
            showContentIfReady();
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentShown(false);

        getSupportLoaderManager().initLoader(0, null, mCommitCallback);
        getSupportLoaderManager().initLoader(1, null, mCommentCallback);
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.commit_title, mObjectSha.substring(0, 7));
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected AppBarLayout.ScrollingViewBehavior onCreateSwipeLayoutBehavior() {
        return new BottomSheetCompatibleScrollingViewBehavior();
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mObjectSha = extras.getString("sha");
        mPullRequestNumber = extras.getInt("pr", -1);
        mInitialComment = extras.getParcelable("initial_comment");
        extras.remove("initial_comment");
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mCommit != null && mComments != null ? TITLES : null;
    }

    @Override
    public void onRefresh() {
        mCommit = null;
        mComments = null;
        setContentShown(false);
        forceLoaderReload(0, 1);
        super.onRefresh();
    }

    @Override
    protected Fragment makeFragment(int position) {
        if (position == 1) {
            Fragment f = CommitNoteFragment.newInstance(mRepoOwner, mRepoName, mObjectSha,
                    mCommit, mComments, mInitialComment);
            mInitialComment = null;
            return f;
        } else {
            return CommitFragment.newInstance(mRepoOwner, mRepoName, mObjectSha,
                    mCommit, mComments);
        }
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment object) {
        return true;
    }

    @Override
    public boolean displayDetachAction() {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.commit_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected Intent navigateUp() {
        if (mPullRequestNumber > 0) {
            return PullRequestActivity.makeIntent(this, mRepoOwner, mRepoName, mPullRequestNumber);
        }
        return RepositoryActivity.makeIntent(this, mRepoOwner, mRepoName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String diffUrl = "https://github.com/" + mRepoOwner + "/" + mRepoName + "/commit/" + mObjectSha;
        switch (item.getItemId()) {
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(diffUrl));
                return true;
            case R.id.share:
                IntentUtils.share(this, getString(R.string.share_commit_subject,
                        mObjectSha.substring(0, 7), mRepoOwner + "/" + mRepoName), diffUrl);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCommentsUpdated() {
        mComments = null;
        setResult(RESULT_OK);
        setContentShown(false);
        forceLoaderReload(1);
    }

    private void showContentIfReady() {
        if (mCommit != null && mComments != null) {
            setContentShown(true);
            invalidateTabs();
            if (mInitialComment != null) {
                getPager().setCurrentItem(1);
            }
        }
    }
}