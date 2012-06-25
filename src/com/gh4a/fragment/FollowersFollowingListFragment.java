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

import org.eclipse.egit.github.core.User;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.loader.FollowersFollowingListLoader;

public class FollowersFollowingListFragment extends SherlockFragment 
    implements LoaderManager.LoaderCallbacks<List<User>>, OnItemClickListener {

    private String mLogin;
    private boolean mFindFollowers;
    private ListView mListView;
    private UserAdapter mAdapter;
    private List<User> mUsers;
    
    public static FollowersFollowingListFragment newInstance(String login, boolean mFindFollowers) {
        FollowersFollowingListFragment f = new FollowersFollowingListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putBoolean("FIND_FOLLOWER", mFindFollowers);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mFindFollowers = getArguments().getBoolean("FIND_FOLLOWER");
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
        
        mAdapter = new UserAdapter(getSherlockActivity(), new ArrayList<User>(), 
                R.layout.row_gravatar_1, false);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }

    private void fillData() {
        mAdapter.addAll(mUsers);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Gh4Application context = ((BaseSherlockFragmentActivity) getActivity()).getApplicationContext();
        User user = (User) adapterView.getAdapter().getItem(position);
        context.openUserInfoActivity(getActivity(), user.getLogin(), user.getName());
    }

    @Override
    public Loader<List<User>> onCreateLoader(int id, Bundle args) {
        if (mFindFollowers) {
            return new FollowersFollowingListLoader(getSherlockActivity(), 
                    mLogin, -1, true);
        }
        else {
            return new FollowersFollowingListLoader(getSherlockActivity(), 
                    mLogin, -1, false);
        }
    }

    @Override
    public void onLoadFinished(Loader<List<User>> loader, List<User> users) {
        if (users != null) {
            mUsers = users;
            fillData();
        }
    }

    @Override
    public void onLoaderReset(Loader<List<User>> users) {
        // TODO Auto-generated method stub
        
    }
}