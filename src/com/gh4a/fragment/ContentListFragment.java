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

import org.eclipse.egit.github.core.TreeEntry;

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
import com.gh4a.adapter.FileAdapter;
import com.gh4a.loader.ContentListLoader;

public class ContentListFragment extends SherlockFragment 
    implements LoaderManager.LoaderCallbacks<List<TreeEntry>> {

    protected String mRepoOwner;
    protected String mRepoName;
    private ListView mListView;
    private FileAdapter mAdapter;

    public static ContentListFragment newInstance(String repoOwner, String repoName) {
        ContentListFragment f = new ContentListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
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
        
        mAdapter = new FileAdapter(getSherlockActivity(), new ArrayList<TreeEntry>());
        mListView.setAdapter(mAdapter);
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }

    private void fillData(List<TreeEntry> entries) {
        if (entries != null && entries.size() > 0) {
            mAdapter.addAll(entries);
        }
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public Loader<List<TreeEntry>> onCreateLoader(int id, Bundle args) {
        return new ContentListLoader(getSherlockActivity(), mRepoOwner, mRepoName);
    }

    @Override
    public void onLoadFinished(Loader<List<TreeEntry>> loader, List<TreeEntry> entries) {
        fillData(entries);
    }

    @Override
    public void onLoaderReset(Loader<List<TreeEntry>> arg0) {
        // TODO Auto-generated method stub
    }
}