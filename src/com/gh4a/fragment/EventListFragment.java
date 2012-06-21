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

import org.eclipse.egit.github.core.event.Event;

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
import com.gh4a.Constants.User;
import com.gh4a.R.id;
import com.gh4a.R.layout;
import com.gh4a.adapter.FeedAdapter;
import com.gh4a.loader.EventListLoader;

public class EventListFragment extends SherlockFragment 
    implements LoaderManager.LoaderCallbacks<List<Event>> {

    private String mLogin;
    private boolean mIsPrivate;
    private ListView mListView;
    private FeedAdapter mAdapter;

    public static EventListFragment newInstance(String login, boolean isPrivate) {
        EventListFragment f = new EventListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putBoolean(Constants.Event.IS_PRIVATE, isPrivate);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mIsPrivate = getArguments().getBoolean(Constants.Event.IS_PRIVATE);
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
        
        mAdapter = new FeedAdapter(getSherlockActivity(), new ArrayList<Event>());
        mListView.setAdapter(mAdapter);
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }

    private void fillData(List<Event> events) {
        if (events != null && events.size() > 0) {
            mAdapter.addAll(events);
        }
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public Loader<List<Event>> onCreateLoader(int id, Bundle args) {
        return new EventListLoader(getSherlockActivity(), mLogin, mIsPrivate);
    }

    @Override
    public void onLoadFinished(Loader<List<Event>> loader, List<Event> events) {
        fillData(events);
    }

    @Override
    public void onLoaderReset(Loader<List<Event>> arg0) {
        // TODO Auto-generated method stub
        
    }
}