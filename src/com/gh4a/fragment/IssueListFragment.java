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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.IssueActivity;
import com.gh4a.adapter.IssueAdapter;
import com.gh4a.loader.PageIteratorLoader;

public class IssueListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<Issue>>, OnItemClickListener, OnScrollListener {

    private String mRepoOwner;
    private String mRepoName;
    private Map<String, String> mFilterData;
    private ListView mListView;
    private IssueAdapter mAdapter;
    private PageIterator<Issue> mDataIterator;
    private boolean isLoadMore;
    private boolean isLoadCompleted;
    private boolean isFirstTimeLoad;
    private TextView mLoadingView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
        
        mFilterData = new HashMap<String, String>();
        
        Bundle args = getArguments();
        Iterator<String> i = args.keySet().iterator();
        while (i.hasNext()) {
            String key = i.next();
            if (Constants.Repository.REPO_OWNER != key 
                    && Constants.Repository.REPO_NAME != key) {
                mFilterData.put(key, args.getString(key));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        
        mListView = (ListView) v.findViewById(R.id.list_view);
        mListView.setFastScrollEnabled(true);
        
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        LayoutInflater vi = getSherlockActivity().getLayoutInflater();
        mLoadingView = (TextView) vi.inflate(R.layout.row_simple, null);
        mLoadingView.setText(R.string.loading_msg);
        mLoadingView.setTextColor(getResources().getColor(R.color.highlight));
        
        mAdapter = new IssueAdapter(getSherlockActivity());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);
        
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (!isFirstTimeLoad) {
            loadData();
            
            if (getLoaderManager().getLoader(0) == null) {
                getLoaderManager().initLoader(0, null, this);    
            }
            else {
                getLoaderManager().restartLoader(0, null, this);
            }
            getLoaderManager().getLoader(0).forceLoad();
        }
    }
    
    public void loadData() {
        IssueService issueService = (IssueService)
                Gh4Application.get(getActivity()).getService(Gh4Application.ISSUE_SERVICE);
        mDataIterator = issueService.pageIssues(new RepositoryId(mRepoOwner, mRepoName), mFilterData);
    }
    
    private void fillData(List<Issue> issues) {
        if (issues != null && !issues.isEmpty()) {
            if (mListView.getFooterViewsCount() == 0) {
                mListView.addFooterView(mLoadingView);
                mListView.setAdapter(mAdapter);
            }
            if (isLoadMore) {
                mAdapter.addAll(mAdapter.getCount(), issues);
                mAdapter.notifyDataSetChanged();
            }
            else {
                mAdapter.clear();
                mAdapter.addAll(issues);
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(0);
            }
        }
        else {
            mListView.removeFooterView(mLoadingView);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {

        boolean loadMore = firstVisible + visibleCount >= totalCount;

        if(loadMore) {
            if (getLoaderManager().getLoader(0) != null
                    && isLoadCompleted) {
                isLoadMore = true;
                isLoadCompleted = false;
                getLoaderManager().getLoader(0).forceLoad();
            }
        }
    }
    
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}
    
    @Override
    public Loader<List<Issue>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<Issue>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<Issue>> loader, List<Issue> issues) {
        isLoadCompleted = true;
        isFirstTimeLoad = true;
        hideLoading();
        fillData(issues);
    }

    @Override
    public void onLoaderReset(Loader<List<Issue>> arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Issue issue = (Issue) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(getSherlockActivity(), IssueActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Issue.ISSUE_NUMBER, issue.getNumber());
        intent.putExtra(Constants.Issue.ISSUE_STATE, issue.getState());
        startActivity(intent);
    }

        
}