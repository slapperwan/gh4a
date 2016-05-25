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

public class IssueListFragment extends PagedDataBaseFragment<Issue> {
    private static final int REQUEST_ISSUE = 1000;

    private Map<String, String> mFilterData;
    private int mEmptyTextResId;
    private boolean mShowRepository;
    private boolean mShowingClosed;

    public static IssueListFragment newInstance(Map<String, String> filterData,
            boolean showingClosed, int emptyTextResId, boolean showRepository) {
        IssueListFragment f = new IssueListFragment();

        Bundle args = new Bundle();
        if (filterData != null) {
            for (String key : filterData.keySet()) {
                args.putString("filter_" + key, filterData.get(key));
            }
        }
        args.putInt("emptytext", emptyTextResId);
        args.putBoolean("closed", showingClosed);
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
        mShowingClosed = args.getBoolean("closed");
        mShowRepository = args.getBoolean("withrepo");
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
        Intent intent = issue.getPullRequest() != null
                ? IntentUtils.getPullRequestActivityIntent(getActivity(),
                        urlPart[4], urlPart[5], issue.getNumber())
                : IntentUtils.getIssueActivityIntent(getActivity(),
                        urlPart[4], urlPart[5], issue.getNumber());
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
    protected PageIterator<Issue> onCreateIterator() {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        return issueService.pageSearchIssues(mFilterData);
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

        public String getSortDirection() {
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