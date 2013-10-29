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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.activities.CommitActivity;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.RepositoryCommitsLoader;

public class PullRequestCommitListFragment extends ListDataBaseFragment<RepositoryCommit> {
    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;

    public static PullRequestCommitListFragment newInstance(String repoOwner,
            String repoName, int pullRequestNumber) {
        PullRequestCommitListFragment f = new PullRequestCommitListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        args.putInt(Constants.Issue.ISSUE_NUMBER, pullRequestNumber);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
        mPullRequestNumber = getArguments().getInt(Constants.Issue.ISSUE_NUMBER);
    }

    @Override
    protected RootAdapter<RepositoryCommit> onCreateAdapter() {
        return new CommitAdapter(getSherlockActivity());
    }

    @Override
    protected void onItemClick(RepositoryCommit commit) {
        Intent intent = new Intent(getSherlockActivity(), CommitActivity.class);
        
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Object.OBJECT_SHA, commit.getSha());
        intent.putExtra(Constants.Object.TREE_SHA, commit.getCommit().getTree().getSha());

        startActivity(intent);
    }

    @Override
    public Loader<LoaderResult<List<RepositoryCommit>>> onCreateLoader(int id, Bundle args) {
        return new RepositoryCommitsLoader(getSherlockActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
    }
}