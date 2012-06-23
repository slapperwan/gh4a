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

import org.eclipse.egit.github.core.RepositoryIssue;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.RepositoryIssueAdapter;
import com.gh4a.loader.RepositoryIssueListLoader;

public class RepositoryIssueListFragment extends SherlockFragment 
    implements LoaderManager.LoaderCallbacks<List<RepositoryIssue>> {

    private Map<String, String> mFilterData;
    private ListView mListView;
    private RepositoryIssueAdapter mAdapter;
    
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
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onCreate RepositoryIssueListFragment");
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
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onCreateView RepositoryIssueListFragment");
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onActivityCreated RepositoryIssueListFragment");
        super.onActivityCreated(savedInstanceState);
        
        mAdapter = new RepositoryIssueAdapter(getSherlockActivity(), new ArrayList<RepositoryIssue>());
        mListView.setAdapter(mAdapter);
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    private void fillData(List<RepositoryIssue> issues) {
        if (issues != null && issues.size() > 0) {
            mAdapter.addAll(issues);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<List<RepositoryIssue>> onCreateLoader(int id, Bundle args) {
        return new RepositoryIssueListLoader(getSherlockActivity(), mFilterData);
    }

    @Override
    public void onLoadFinished(Loader<List<RepositoryIssue>> loader, List<RepositoryIssue> issues) {
        fillData(issues);
    }

    @Override
    public void onLoaderReset(Loader<List<RepositoryIssue>> arg0) {
        // TODO Auto-generated method stub
        
    }
}