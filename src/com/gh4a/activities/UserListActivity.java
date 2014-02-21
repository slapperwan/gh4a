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
package com.gh4a.activities;

import java.util.List;

import org.eclipse.egit.github.core.User;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;

public abstract class UserListActivity extends LoadingFragmentActivity implements OnItemClickListener {
    protected String mSearchKey;
    protected UserAdapter mUserAdapter;
    protected ListView mListViewUsers;

    protected LoaderCallbacks<List<User>> mUserListCallback = new LoaderCallbacks<List<User>>() {
        @Override
        public Loader<LoaderResult<List<User>>> onCreateLoader(int id, Bundle args) {
            return getUserListLoader();
        }

        @Override
        public void onResultReady(LoaderResult<List<User>> result) {
            boolean success = !result.handleError(UserListActivity.this);
            if (success) {
                fillData(result.getData());
            }
            setContentEmpty(!success);
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        if (!isOnline()) {
            setErrorView();
            return;
        }

        setContentView(R.layout.generic_list);
        setContentShown(false);
        setRequestData();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getActionBarTitle());
        actionBar.setSubtitle(getSubTitle());
        actionBar.setDisplayHomeAsUpEnabled(true);

        mListViewUsers = (ListView) findViewById(R.id.list_view);
        mListViewUsers.setOnItemClickListener(this);

        mUserAdapter = new UserAdapter(this, getShowExtraData());
        mListViewUsers.setAdapter(mUserAdapter);

        getSupportLoaderManager().initLoader(0, null, mUserListCallback);
    }

    protected void fillData(List<User> users) {
        if (users != null) {
            mUserAdapter.addAll(users);
        }
        mUserAdapter.notifyDataSetChanged();
    }

    protected abstract Loader<LoaderResult<List<User>>> getUserListLoader();

    protected void setRequestData() {
        mSearchKey = getIntent().getExtras().getString("searchKey");
    }

    protected abstract String getActionBarTitle();
    protected abstract String getSubTitle();

    protected boolean getShowExtraData() {
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        User user = (User) adapterView.getAdapter().getItem(position);
        IntentUtils.openUserInfoActivity(this, user);
    }
}