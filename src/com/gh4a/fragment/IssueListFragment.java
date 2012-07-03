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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.IssueActivity;
import com.gh4a.R;
import com.gh4a.adapter.IssueAdapter;
import com.gh4a.loader.PageIteratorLoader;

public class IssueListFragment extends SherlockFragment 
    implements LoaderManager.LoaderCallbacks<List<Issue>>, OnItemClickListener {

    private String mRepoOwner;
    private String mRepoName;
    private String mState;
    private Map<String, String> mFilterData;
    private ListView mListView;
    private IssueAdapter mAdapter;
    private PageIterator<Issue> mDataIterator;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
        mState = getArguments().getString("state");
        
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
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mAdapter = new IssueAdapter(getSherlockActivity(), new ArrayList<Issue>());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        
        if (mState == null || "open".equals(mState)) {
            getSherlockActivity().getSupportActionBar().setTitle(R.string.issue_open);
        }
        else {
            getSherlockActivity().getSupportActionBar().setTitle(R.string.issue_closed);
        }
        
        loadData();
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    public void loadData() {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        IssueService issueService = new IssueService(client);
        mDataIterator = issueService.pageIssues(new RepositoryId(mRepoOwner, mRepoName), mFilterData);
    }
    
    private void fillData(List<Issue> issues) {
        if (issues != null && issues.size() > 0) {
            mAdapter.addAll(issues);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<List<Issue>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<Issue>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<Issue>> loader, List<Issue> issues) {
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
        Bundle data = ((Gh4Application) getSherlockActivity().getApplication()).populateIssue(issue);
        // extra data
        data.putString(Constants.Repository.REPO_OWNER, mRepoOwner);
        data.putString(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.DATA_BUNDLE, data);
        startActivity(intent);
    }

        
}