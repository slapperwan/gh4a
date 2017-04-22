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

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CommitActivity;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.StringUtils;

public class CommitListFragment extends PagedDataBaseFragment<RepositoryCommit> {
    private static final int REQUEST_COMMIT = 2000;

    private String mRepoOwner;
    private String mRepoName;
    private String mRef;
    private String mFilePath;

    public static CommitListFragment newInstance(Repository repo, String ref) {
        return newInstance(repo.getOwner().getLogin(), repo.getName(),
                StringUtils.isBlank(ref) ? repo.getDefaultBranch() : ref, null);
    }

    public static CommitListFragment newInstance(String repoOwner, String repoName,
            String ref, String filePath) {
        CommitListFragment f = new CommitListFragment();

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putString("ref", ref);
        args.putString("path", filePath);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
        mRef = getArguments().getString("ref");
        mFilePath = getArguments().getString("path");
    }

    @Override
    protected RootAdapter<RepositoryCommit, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new CommitAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_commits_found;
    }

    @Override
    public void onItemClick(RepositoryCommit commit) {
        String[] urlPart = commit.getUrl().split("/");
        Intent intent = CommitActivity.makeIntent(getActivity(),
                urlPart[4], urlPart[5], commit.getSha());
        startActivityForResult(intent, REQUEST_COMMIT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COMMIT) {
            if (resultCode == Activity.RESULT_OK) {
                // comments were updated
                onRefresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected PageIterator<RepositoryCommit> onCreateIterator() {
        CommitService commitService = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        return commitService.pageCommits(
                new RepositoryId(mRepoOwner, mRepoName), mRef, mFilePath);
    }
}