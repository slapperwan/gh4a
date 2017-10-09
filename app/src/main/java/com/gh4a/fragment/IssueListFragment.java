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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.adapter.IssueAdapter;
import com.gh4a.adapter.RepositoryIssueAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.issues.IssueService;

import java.util.HashMap;
import java.util.Map;

public class IssueListFragment extends PagedDataBaseFragment<Issue> {
    private static final int REQUEST_ISSUE = 1000;

    private Map<String, Object> mFilterData;
    private int mEmptyTextResId;
    private boolean mShowRepository;
    private String mIssueState;

    public static IssueListFragment newInstance(Map<String, String> filterData, String state,
            int emptyTextResId, boolean showRepository) {
        IssueListFragment f = new IssueListFragment();

        Bundle args = new Bundle();
        if (filterData != null) {
            for (String key : filterData.keySet()) {
                args.putString("filter_" + key, filterData.get(key));
            }
        }
        args.putInt("emptytext", emptyTextResId);
        args.putString("state", state);
        args.putBoolean("withrepo", showRepository);

        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilterData = new HashMap<>();

        Bundle args = getArguments();
        for (String key : args.keySet()) {
            if (key.startsWith("filter_")) {
                mFilterData.put(key.substring(7), args.getString(key));
            }
        }
        mEmptyTextResId = args.getInt("emptytext");
        mIssueState = args.getString("state");
        mShowRepository = args.getBoolean("withrepo");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switch (mIssueState) {
            case ApiHelpers.IssueState.CLOSED:
                setHighlightColors(R.attr.colorIssueClosed, R.attr.colorIssueClosedDark);
                break;
            case ApiHelpers.IssueState.MERGED:
                setHighlightColors(R.attr.colorPullRequestMerged,
                        R.attr.colorPullRequestMergedDark);
                break;
            default:
                setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
                break;
        }
    }

    @Override
    public void onItemClick(Issue issue) {
        String[] urlPart = issue.url().split("/");
        Intent intent = issue.pullRequest() != null
                ? PullRequestActivity.makeIntent(getActivity(), urlPart[4], urlPart[5], issue.number())
                : IssueActivity.makeIntent(getActivity(), urlPart[4], urlPart[5], issue.number());
        startActivityForResult(intent, REQUEST_ISSUE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ISSUE) {
            if (resultCode == Activity.RESULT_OK) {
                super.onRefresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
    protected PageIteratorLoader<Issue> onCreateLoader() {
        final IssueService service = Gh4Application.get().getGitHubService(IssueService.class);
        return new PageIteratorLoader<Issue>(getActivity()) {
            @Override
            protected Page<Issue> loadPage(int page) throws ApiRequestException {
                return ApiHelpers.throwOnFailure(
                        service.getIssues(mFilterData, page).blockingGet());
            }
        };
    }

    public static class SortDrawerHelper {
        private String mSortMode;
        private boolean mSortAscending;

        private static final String SORT_MODE_CREATED = "created";
        private static final String SORT_MODE_UPDATED = "updated";
        private static final String SORT_MODE_COMMENTS = "comments";

        public SortDrawerHelper() {
            mSortMode = SORT_MODE_CREATED;
            mSortAscending = false;
        }

        public static int getMenuResId() {
            return R.menu.issue_list_sort;
        }

        public String getSortMode() {
            return mSortMode;
        }

        public String getSortOrder() {
            return mSortAscending ? "asc" : "desc";
        }

        public boolean handleItemSelection(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.sort_created_asc:
                    updateSortMode(SORT_MODE_CREATED, true);
                    return true;
                case R.id.sort_created_desc:
                    updateSortMode(SORT_MODE_CREATED, false);
                    return true;
                case R.id.sort_updated_asc:
                    updateSortMode(SORT_MODE_UPDATED, true);
                    return true;
                case R.id.sort_updated_desc:
                    updateSortMode(SORT_MODE_UPDATED, false);
                    return true;
                case R.id.sort_comments_asc:
                    updateSortMode(SORT_MODE_COMMENTS, true);
                    return true;
                case R.id.sort_comments_desc:
                    updateSortMode(SORT_MODE_COMMENTS, false);
                    return true;
            }

            return false;
        }

        protected void updateSortMode(String sortMode, boolean ascending) {
            mSortAscending = ascending;
            mSortMode = sortMode;
        }
    }
}