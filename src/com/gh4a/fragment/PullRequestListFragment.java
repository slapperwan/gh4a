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

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.os.Bundle;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.PullRequestAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.IntentUtils;

public class PullRequestListFragment extends PagedDataBaseFragment<PullRequest> {
    private String mRepoOwner;
    private String mRepoName;
    private String mState;

    public static PullRequestListFragment newInstance(String repoOwner, String repoName, String state) {
        PullRequestListFragment f = new PullRequestListFragment();
        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putString(Constants.Issue.STATE, state);

        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);
        mState = getArguments().getString(Constants.Issue.STATE);
    }

    @Override
    protected void onItemClick(PullRequest pullRequest) {
        startActivity(IntentUtils.getPullRequestActivityIntent(getActivity(),
                mRepoOwner, mRepoName, pullRequest.getNumber()));
    }

    @Override
    protected RootAdapter<PullRequest> onCreateAdapter() {
        return new PullRequestAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_pull_requests_found;
    }

    @Override
    protected PageIterator<PullRequest> onCreateIterator() {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        return pullRequestService.pagePullRequests(new RepositoryId(mRepoOwner, mRepoName), mState);
    }
}