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
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.adapter.PullRequestAdapter;
import com.gh4a.adapter.RootAdapter;

public class PullRequestListFragment extends PagedDataBaseFragment<PullRequest> {
    private String mRepoOwner;
    private String mRepoName;
    private boolean mShowClosed;

    public static PullRequestListFragment newInstance(String repoOwner, String repoName, boolean showClosed) {
        PullRequestListFragment f = new PullRequestListFragment();
        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putBoolean("closed", showClosed);

        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRepoOwner = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
        mShowClosed = getArguments().getBoolean("closed");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHighlightColors(
                mShowClosed ? R.attr.colorIssueClosed : R.attr.colorIssueOpen,
                mShowClosed ? R.attr.colorIssueClosedDark : R.attr.colorIssueOpenDark);
    }

    @Override
    public void onItemClick(PullRequest pullRequest) {
        startActivity(PullRequestActivity.makeIntent(getActivity(),
                mRepoOwner, mRepoName, pullRequest.getNumber()));
    }

    @Override
    protected RootAdapter<PullRequest, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
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
        return pullRequestService.pagePullRequests(new RepositoryId(mRepoOwner, mRepoName),
                mShowClosed ? "closed" : "open");
    }
}