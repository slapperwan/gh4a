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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.graphics.Color;
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
import com.gh4a.adapter.PullRequestAdapter;
import com.gh4a.loader.PageIteratorLoader;

public class PullRequestListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<PullRequest>>, OnItemClickListener, OnScrollListener {

    private String mRepoOwner;
    private String mRepoName;
    private String mState;
    private ListView mListView;
    private PullRequestAdapter mAdapter;
    private PageIterator<PullRequest> mDataIterator;
    private boolean isLoadMore;
    private boolean isLoadCompleted;
    private TextView mLoadingView;
    
    public static PullRequestListFragment newInstance(String repoOwner, String repoName, String state) {
        
        PullRequestListFragment f = new PullRequestListFragment();
        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        args.putString(Constants.Issue.ISSUE_STATE, state);
        
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
        mState = getArguments().getString(Constants.Issue.ISSUE_STATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        LayoutInflater vi = getSherlockActivity().getLayoutInflater();
        mLoadingView = (TextView) vi.inflate(R.layout.row_simple, null);
        mLoadingView.setText("Loading...");
        mLoadingView.setTextColor(Color.parseColor("#0099cc"));
        
        mAdapter = new PullRequestAdapter(getSherlockActivity(), new ArrayList<PullRequest>());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);
        
        loadData();
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    public void loadData() {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        PullRequestService pullRequestService = new PullRequestService(client);
        mDataIterator = pullRequestService.pagePullRequests(new RepositoryId(mRepoOwner, mRepoName), mState);
    }
    
    private void fillData(List<PullRequest> pullRequests) {
        if (pullRequests != null && !pullRequests.isEmpty()) {
            if (mListView.getFooterViewsCount() == 0) {
                mListView.addFooterView(mLoadingView);
                mListView.setAdapter(mAdapter);
            }
            if (isLoadMore) {
                mAdapter.addAll(mAdapter.getCount(), pullRequests);
                mAdapter.notifyDataSetChanged();
            }
            else {
                mAdapter.clear();
                mAdapter.addAll(pullRequests);
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
    public Loader<List<PullRequest>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<PullRequest>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<PullRequest>> loader, List<PullRequest> pullRequests) {
        isLoadCompleted = true;
        hideLoading();
        fillData(pullRequests);
    }

    @Override
    public void onLoaderReset(Loader<List<PullRequest>> arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        PullRequest pullRequest = (PullRequest) mAdapter.getItem(position);
        app.openPullRequestActivity(getSherlockActivity(), mRepoOwner,
                mRepoName, pullRequest.getNumber());
    }

        
}