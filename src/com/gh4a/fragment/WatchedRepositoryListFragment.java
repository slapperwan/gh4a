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

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.WatcherService;

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
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.loader.PageIteratorLoader;

public class WatchedRepositoryListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<Repository>>, OnItemClickListener, OnScrollListener {

    private String mLogin;
    private ListView mListView;
    private RepositoryAdapter mAdapter;
    private PageIterator<Repository> mDataIterator;
    private boolean isLoadMore;
    private boolean isLoadCompleted;
    private boolean isFirstTimeLoad;
    private TextView mLoadingView;
    
    public static WatchedRepositoryListFragment newInstance(String login) {
        WatchedRepositoryListFragment f = new WatchedRepositoryListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
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
        
        mAdapter = new RepositoryAdapter(getSherlockActivity());
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
        WatcherService watcherService = (WatcherService)
                Gh4Application.get(getActivity()).getService(Gh4Application.WATCHER_SERVICE);
        try {
            mDataIterator = watcherService.pageWatched(mLogin);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void fillData(List<Repository> repositories) {
        if (repositories != null && !repositories.isEmpty()) {
            if (mListView.getFooterViewsCount() == 0) {
                mListView.addFooterView(mLoadingView);
                mListView.setAdapter(mAdapter);
            }
            if (isLoadMore) {
                mAdapter.addAll(mAdapter.getCount(), repositories);
                mAdapter.notifyDataSetChanged();
            }
            else {
                mAdapter.clear();
                mAdapter.addAll(repositories);
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(0);
            }
        }
        else {
            mListView.removeFooterView(mLoadingView);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Gh4Application app = Gh4Application.get(getActivity());
        Repository repository = (Repository) adapterView.getAdapter().getItem(position);
        
        Intent intent = new Intent(getActivity(), RepositoryActivity.class);
        Bundle data = app.populateRepository(repository);
        intent.putExtra(Constants.DATA_BUNDLE, data);
        startActivity(intent);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
        boolean loadMore = firstVisible + visibleCount >= totalCount;

        if (loadMore) {
            if (getLoaderManager().getLoader(0) != null && isLoadCompleted) {
                isLoadMore = true;
                isLoadCompleted = false;
                getLoaderManager().getLoader(0).forceLoad();
            }
        }
    }
    
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}
    
    @Override
    public Loader<List<Repository>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<Repository>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<Repository>> loader, List<Repository> repositories) {
        isLoadCompleted = true;
        isFirstTimeLoad = true;
        hideLoading();
        fillData(repositories);
    }

    @Override
    public void onLoaderReset(Loader<List<Repository>> arg0) {
        // TODO Auto-generated method stub
        
    }
}