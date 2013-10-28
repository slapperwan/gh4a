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

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.StarService;

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
import com.gh4a.adapter.UserAdapter;
import com.gh4a.loader.PageIteratorLoader;

public class StargazerListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<User>>, OnItemClickListener {

    private String mRepoOwner;
    private String mRepoName;
    private ListView mListView;
    private UserAdapter mAdapter;
    private PageIterator<User> mDataIterator;
    
    public static StargazerListFragment newInstance(String repoOwner, String repoName) {
        StargazerListFragment f = new StargazerListFragment();

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
        
        mAdapter = new UserAdapter(getSherlockActivity(), false);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        
        loadData();

        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    private void loadData() {
        StarService starService = (StarService)
                Gh4Application.get(getActivity()).getService(Gh4Application.STAR_SERVICE);
        mDataIterator = starService.pageStargazers(new RepositoryId(mRepoOwner, mRepoName));
    }

    private void fillData(List<User> users) {
        mAdapter.addAll(users);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Gh4Application app = Gh4Application.get(getActivity());
        User user = (User) adapterView.getAdapter().getItem(position);
        app.openUserInfoActivity(getActivity(), user.getLogin(), user.getName());
    }

    @Override
    public Loader<List<User>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<User>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<User>> loader, List<User> users) {
        hideLoading();
        if (users != null) {
            fillData(users);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<User>> users) {
        // TODO Auto-generated method stub
        
    }
}