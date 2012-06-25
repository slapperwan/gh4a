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
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;
import org.eclipse.egit.github.core.service.IssueService;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.IssueAdapter;
import com.gh4a.loader.IssueListLoader;

public class IssueListFragment extends SherlockFragment 
    implements LoaderManager.LoaderCallbacks<List<Issue>> {

    private String mState;
    private String mRepoOwner;
    private String mRepoName;
    private Map<String, String> mFilterData;
    private ListView mListView;
    private IssueAdapter mAdapter;
    private PageIterator<Event> mDataIterator;
    
    static IssueListFragment newInstance(String repoOwner, String repoName, 
            Map<String, String> filterData) {
        
        IssueListFragment f = new IssueListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        
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

        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.User.USER_LOGIN);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);

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
    
    public void loadData() {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        IssueService issueService = new IssueService(client);
        issueService.pageIssues(new RepositoryId(mRepoOwner, mRepoName), new HashMap<String, String>());
    }
    
    private void fillData(List<Issue> issues) {
        if (issues != null && issues.size() > 0) {
            mAdapter.addAll(issues);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<List<Issue>> onCreateLoader(int id, Bundle args) {
        return new IssueListLoader(getSherlockActivity(), mRepoOwner, mRepoName, mFilterData);
    }

    @Override
    public void onLoadFinished(Loader<List<Issue>> loader, List<Issue> issues) {
        fillData(issues);
    }

    @Override
    public void onLoaderReset(Loader<List<Issue>> arg0) {
        // TODO Auto-generated method stub
        
    }
}