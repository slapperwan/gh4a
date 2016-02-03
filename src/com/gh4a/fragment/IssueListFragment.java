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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.IssueEditActivity;
import com.gh4a.adapter.IssueAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.UiUtils;
import com.shamanland.fab.FloatingActionButton;
import com.shamanland.fab.ShowHideOnScroll;

public class IssueListFragment extends PagedDataBaseFragment<Issue> implements
        View.OnClickListener {
    private static final int REQUEST_ISSUE = 1000;
    private static final int REQUEST_ISSUE_CREATE = 1001;

    private String mRepoOwner;
    private String mRepoName;
    private Map<String, String> mFilterData;

    public static IssueListFragment newInstance(String repoOwner, String repoName,
            Map<String, String> filterData) {

        IssueListFragment f = new IssueListFragment();
        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);

        if (filterData != null) {
            for (String key : filterData.keySet()) {
                args.putString(key, filterData.get(key));
            }
        }
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (TextUtils.equals(mFilterData.get(Constants.Issue.STATE), Constants.Issue.STATE_OPEN)) {
            View wrapper = inflater.inflate(R.layout.fab_list_wrapper, container, false);
            ViewGroup listContainer = (ViewGroup) wrapper.findViewById(R.id.container);
            listContainer.addView(super.onCreateView(inflater, listContainer, savedInstanceState));
            return wrapper;
        } else {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String stateFilter = mFilterData.get(Constants.Issue.STATE);
        int stateColorAttr = TextUtils.equals(stateFilter, Constants.Issue.STATE_OPEN)
                ? R.attr.colorIssueOpen : TextUtils.equals(stateFilter, Constants.Issue.STATE_CLOSED)
                ? R.attr.colorIssueClosed : 0;

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_add);
        if (fab != null) {
            if (Gh4Application.get().isAuthorized()) {
                RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
                recyclerView.setOnTouchListener(new ShowHideOnScroll(fab));
                fab.setOnClickListener(this);
            } else {
                fab.setVisibility(View.GONE);
            }
        }

        if (stateColorAttr != 0) {
            UiUtils.trySetListOverscrollColor(getRecyclerView(),
                    UiUtils.resolveColor(getActivity(), stateColorAttr));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);

        mFilterData = new HashMap<>();

        Bundle args = getArguments();
        for (String key : args.keySet()) {
            if (!key.equals(Constants.Repository.OWNER)
                    && !key.equals(Constants.Repository.NAME)) {
                mFilterData.put(key, args.getString(key));
            }
        }
    }

    @Override
    protected RootAdapter<Issue, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new IssueAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_issues_found;
    }

    @Override
    public void onItemClick(Issue issue) {
        Intent intent = new Intent(getActivity(), IssueActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.putExtra(Constants.Issue.NUMBER, issue.getNumber());
        intent.putExtra(Constants.Issue.STATE, issue.getState());
        startActivityForResult(intent, REQUEST_ISSUE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ISSUE || requestCode == REQUEST_ISSUE_CREATE) {
            if (resultCode == Activity.RESULT_OK) {
                super.refresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected PageIterator<Issue> onCreateIterator() {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        return issueService.pageIssues(new RepositoryId(mRepoOwner, mRepoName), mFilterData);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(getActivity(), IssueEditActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        startActivityForResult(intent, REQUEST_ISSUE_CREATE);
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