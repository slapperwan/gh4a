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
package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.RepositoryCommit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.RepositoryCommitsLoader;
import com.gh4a.utils.IntentUtils;

public class PullRequestCommitListFragment extends ListDataBaseFragment<RepositoryCommit> {
    private static final int REQUEST_COMMIT = 2000;

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;

    public static PullRequestCommitListFragment newInstance(String repoOwner,
            String repoName, int pullRequestNumber) {
        PullRequestCommitListFragment f = new PullRequestCommitListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putInt(Constants.Issue.NUMBER, pullRequestNumber);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);
        mPullRequestNumber = getArguments().getInt(Constants.Issue.NUMBER);
    }

    @Override
    protected RootAdapter<RepositoryCommit> onCreateAdapter() {
        return new CommitAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_commits_in_pullrequest_found;
    }

    @Override
    protected void onItemClick(RepositoryCommit commit) {
        Intent intent = IntentUtils.getCommitInfoActivityIntent(getActivity(),
                mRepoOwner, mRepoName, commit.getSha());
        startActivityForResult(intent, REQUEST_COMMIT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COMMIT) {
            if (resultCode == Activity.RESULT_OK) {
                // comments were updated
                refresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public Loader<LoaderResult<List<RepositoryCommit>>> onCreateLoader(int id, Bundle args) {
        return new RepositoryCommitsLoader(getActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
    }
}