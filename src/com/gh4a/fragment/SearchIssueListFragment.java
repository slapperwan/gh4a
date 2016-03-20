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

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.IssueAdapter;
import com.gh4a.adapter.RepositoryIssueAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;

import java.util.HashMap;
import java.util.Map;

public class SearchIssueListFragment extends PagedDataBaseFragment<Issue> {
    private Map<String, String> mFilterData;
    private int mEmptyTextResId;
    private boolean mShowRepository;
    private boolean mShowingClosed;
    private boolean mShowingPullRequests;

    public static SearchIssueListFragment newInstance(Map<String, String> filterData,
            boolean showingClosed, int emptyTextResId,
            boolean showRepository, boolean showingPullRequests) {
        SearchIssueListFragment f = new SearchIssueListFragment();

        Bundle args = new Bundle();
        if (filterData != null) {
            for (String key : filterData.keySet()) {
                args.putString(key, filterData.get(key));
            }
        }
        args.putInt("emptytext", emptyTextResId);
        args.putBoolean("closed", showingClosed);
        args.putBoolean("withrepo", showRepository);
        args.putBoolean("pr", showingPullRequests);

        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilterData = new HashMap<>();

        Bundle args = getArguments();
        for (String key : args.keySet()) {
            if (!key.equals(Constants.User.LOGIN)
                    && !key.equals(Constants.Repository.NAME)) {
                String arg = args.getString(key);
                if (arg != null) {
                    mFilterData.put(key, arg);
                }
            }
        }
        mEmptyTextResId = args.getInt("emptytext");
        mShowingClosed = args.getBoolean("closed");
        mShowRepository = args.getBoolean("withrepo");
        mShowingPullRequests = args.getBoolean("pr");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mShowingClosed) {
            setHighlightColors(R.attr.colorIssueClosed, R.attr.colorIssueClosedDark);
        } else {
            setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
        }
    }

    @Override
    public void onItemClick(Issue issue) {
        String[] urlPart = issue.getUrl().split("/");

        if (mShowingPullRequests && issue.getPullRequest() != null) {
            startActivity(IntentUtils.getPullRequestActivityIntent(getActivity(), urlPart[4],
                    urlPart[5], issue.getNumber()));
        } else {
            startActivity(IntentUtils.getIssueActivityIntent(getActivity(), urlPart[4],
                    urlPart[5], issue.getNumber()));
        }
    }

    @Override
    protected RootAdapter<Issue, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return mShowRepository
                ? new RepositoryIssueAdapter(getActivity())
                : new IssueAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return mEmptyTextResId;
    }

    @Override
    protected PageIterator<Issue> onCreateIterator() {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        return issueService.pageSearchIssues(mFilterData);
    }
}