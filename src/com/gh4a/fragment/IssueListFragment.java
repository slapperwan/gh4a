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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Issue;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.Constants.Repository;
import com.gh4a.Constants.User;
import com.gh4a.R.id;
import com.gh4a.R.layout;
import com.gh4a.adapter.IssueAdapter;
import com.gh4a.loader.IssueListLoader;

public class IssueListFragment extends SherlockFragment 
    implements LoaderManager.LoaderCallbacks<List<Issue>> {

    private String mState;
    private String mLogin;
    private String mRepo;
    private Map<String, String> mFilterData;
    private ListView mListView;
    private IssueAdapter mAdapter;
    
    static IssueListFragment newInstance(String login, String repo, 
            Map<String, String> filterData) {
        
        IssueListFragment f = new IssueListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putString(Constants.Repository.REPO_NAME, repo);
        
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
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mRepo = getArguments().getString(Constants.Repository.REPO_NAME);

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
    
    private void fillData(List<Issue> issues) {
        if (issues != null && issues.size() > 0) {
            for (Issue issue : issues) {
                mAdapter.add(issue);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<List<Issue>> onCreateLoader(int id, Bundle args) {
        return new IssueListLoader(getSherlockActivity(), mLogin, mRepo, mFilterData);
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