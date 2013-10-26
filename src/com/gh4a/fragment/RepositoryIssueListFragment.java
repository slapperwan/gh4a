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

import org.eclipse.egit.github.core.RepositoryIssue;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.RepositoryIssueAdapter;
import com.gh4a.loader.PageIteratorLoader;

public class RepositoryIssueListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<RepositoryIssue>>, OnItemClickListener {

    private Map<String, String> mFilterData;
    private ListView mListView;
    private RepositoryIssueAdapter mAdapter;
    private PageIterator<RepositoryIssue> mDataIterator;
    private boolean mDataLoaded; 
    
    public static RepositoryIssueListFragment newInstance(Map<String, String> filterData) {
        
        RepositoryIssueListFragment f = new RepositoryIssueListFragment();

        Bundle args = new Bundle();
        if (filterData != null) {
            Iterator<String> i = filterData.keySet().iterator();
            while (i.hasNext()) {
                String key = i.next();
                args.putString(key, filterData.get(key));
            }
        }
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilterData = new HashMap<String, String>();
        
        Bundle args = getArguments();
        Iterator<String> i = args.keySet().iterator();
        while (i.hasNext()) {
            String key = i.next();
            if (Constants.User.USER_LOGIN != key 
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
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mAdapter = new RepositoryIssueAdapter(getSherlockActivity());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mDataLoaded) {
            refresh();
        }
    }
    
    public void refresh() {
        loadData();
        if (getLoaderManager().getLoader(0) == null) {
            getLoaderManager().initLoader(0, null, this);
        }
        else {
            getLoaderManager().restartLoader(0, null, this);
        }
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    public void loadData() {
        IssueService issueService = (IssueService)
                getActivity().getApplicationContext().getSystemService(Gh4Application.ISSUE_SERVICE);
        mDataIterator = issueService.pageIssues(mFilterData);
    }
    
    private void fillData(List<RepositoryIssue> issues) {
        UserActivity activity = (UserActivity) getSherlockActivity();
        activity.invalidateOptionsMenu();
        if (issues != null && issues.size() > 0) {
            mAdapter.clear();
            mAdapter.addAll(issues);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<List<RepositoryIssue>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<RepositoryIssue>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<RepositoryIssue>> loader, List<RepositoryIssue> issues) {
        mDataLoaded = true;
        hideLoading();
        fillData(issues);
    }

    @Override
    public void onLoaderReset(Loader<List<RepositoryIssue>> arg0) {
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View arg1, int position, long id) {
        RepositoryIssue issue = (RepositoryIssue) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(getSherlockActivity(), IssueActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, issue.getRepository().getOwner().getLogin());
        intent.putExtra(Constants.Repository.REPO_NAME, issue.getRepository().getName());
        intent.putExtra(Constants.Issue.ISSUE_NUMBER, issue.getNumber());
        intent.putExtra(Constants.Issue.ISSUE_STATE, issue.getState());
        startActivity(intent);
    }
}