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

import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;

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
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.StringUtils;

public class CommitListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<RepositoryCommit>>, OnItemClickListener, OnScrollListener {

    private Repository mRepository;
    private String mRef;
    private ListView mListView;
    private CommitAdapter mAdapter;
    private PageIterator<RepositoryCommit> mDataIterator;
    private boolean isLoadMore;
    private boolean isLoadCompleted;
    private TextView mLoadingView;
    private boolean mDataLoaded;
    
    public static CommitListFragment newInstance(Repository repository, String ref) {
        
        CommitListFragment f = new CommitListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Object.REF, ref);
        args.putSerializable("REPOSITORY", repository);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = (Repository) getArguments().getSerializable("REPOSITORY");
        mRef = getArguments().getString(Constants.Object.REF);
        if (StringUtils.isBlank(mRef)) {
            mRef = mRepository.getMasterBranch();
        }
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
        mLoadingView.setText(R.string.loading_msg);
        mLoadingView.setTextColor(getResources().getColor(R.color.highlight));
        
        mAdapter = new CommitAdapter(getSherlockActivity());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (!mDataLoaded) {
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
        CommitService commitService = (CommitService)
                Gh4Application.get(getActivity()).getService(Gh4Application.COMMIT_SERVICE);
        mDataIterator = commitService.pageCommits(new RepositoryId(mRepository.getOwner().getLogin(),
                mRepository.getName()), mRef, null);
    }
    
    private void fillData(List<RepositoryCommit> commits) {
        // FIXME
        RepositoryActivity activity = (RepositoryActivity) getSherlockActivity();
        activity.hideLoading();
        if (commits != null && !commits.isEmpty()) {
            if (mListView.getFooterViewsCount() == 0) {
                mListView.addFooterView(mLoadingView);
                mListView.setAdapter(mAdapter);
            }
            if (isLoadMore) {
                mAdapter.addAll(mAdapter.getCount(), commits);
                mAdapter.notifyDataSetChanged();
            }
            else {
                mAdapter.clear();
                mAdapter.addAll(commits);
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
    public Loader<List<RepositoryCommit>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<RepositoryCommit>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<RepositoryCommit>> loader, List<RepositoryCommit> commits) {
        isLoadCompleted = true;
        mDataLoaded = true;
        hideLoading();
        fillData(commits);
    }

    @Override
    public void onLoaderReset(Loader<List<RepositoryCommit>> arg0) {
        // TODO Auto-generated method stub
    }
    
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        RepositoryCommit commit = (RepositoryCommit) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(getSherlockActivity(), CommitActivity.class);
        String[] urlPart = commit.getUrl().split("/");
        
        intent.putExtra(Constants.Repository.REPO_OWNER, urlPart[4]);
        intent.putExtra(Constants.Repository.REPO_NAME, urlPart[5]);
        intent.putExtra(Constants.Object.OBJECT_SHA, commit.getSha());
        intent.putExtra(Constants.Object.TREE_SHA, commit.getCommit().getTree().getSha());

        startActivity(intent);
    }
}