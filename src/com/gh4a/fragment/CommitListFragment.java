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

import android.content.Intent;
import android.os.Bundle;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CommitActivity;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.StringUtils;

public class CommitListFragment extends PagedDataBaseFragment<RepositoryCommit> {
    private String mRepoOwner;
    private String mRepoName;
    private String mRef;
    private String mFilePath;

    public static CommitListFragment newInstance(Repository repo, String ref) {
        return newInstance(repo.getOwner().getLogin(), repo.getName(),
                StringUtils.isBlank(ref) ? repo.getMasterBranch() : ref, null);
    }

    public static CommitListFragment newInstance(String repoOwner, String repoName,
            String ref, String filePath) {
        CommitListFragment f = new CommitListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putString(Constants.Object.REF, ref);
        args.putString(Constants.Object.PATH, filePath);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);
        mRef = getArguments().getString(Constants.Object.REF);
        mFilePath = getArguments().getString(Constants.Object.PATH);
    }

    @Override
    protected RootAdapter<RepositoryCommit> onCreateAdapter() {
        return new CommitAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_commits_found;
    }

    @Override
    protected void onItemClick(RepositoryCommit commit) {
        Intent intent = new Intent(getActivity(), CommitActivity.class);
        String[] urlPart = commit.getUrl().split("/");

        intent.putExtra(Constants.Repository.OWNER, urlPart[4]);
        intent.putExtra(Constants.Repository.NAME, urlPart[5]);
        intent.putExtra(Constants.Object.OBJECT_SHA, commit.getSha());
        intent.putExtra(Constants.Object.REF, commit.getCommit().getTree().getSha());

        startActivity(intent);
    }

    @Override
    protected PageIterator<RepositoryCommit> onCreateIterator() {
        CommitService commitService = (CommitService)
                Gh4Application.get(getActivity()).getService(Gh4Application.COMMIT_SERVICE);
        return commitService.pageCommits(
                new RepositoryId(mRepoOwner, mRepoName), mRef, mFilePath);
    }
}