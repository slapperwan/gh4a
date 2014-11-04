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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.User;

import java.util.List;

public abstract class UserListActivity extends BaseActivity implements
        AdapterView.OnItemClickListener {
    private UserAdapter mUserAdapter;

    private LoaderCallbacks<List<User>> mUserListCallback = new LoaderCallbacks<List<User>>() {
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
        super.onCreate(savedInstanceState);

        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.generic_list);
        setContentShown(false);
        setRequestData();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getActionBarTitle());
        actionBar.setSubtitle(getSubTitle());
        actionBar.setDisplayHomeAsUpEnabled(true);

        mUserAdapter = new UserAdapter(this);

        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        listView.setAdapter(mUserAdapter);
        listView.setBackgroundResource(
                UiUtils.resolveDrawable(this, R.attr.listBackground));

        getSupportLoaderManager().initLoader(0, null, mUserListCallback);
    }

    protected void fillData(List<User> users) {
        if (users != null) {
            mUserAdapter.addAll(users);
        }
        mUserAdapter.notifyDataSetChanged();
    }

    protected abstract Loader<LoaderResult<List<User>>> getUserListLoader();

    protected abstract void setRequestData();

    protected abstract String getActionBarTitle();
    protected abstract String getSubTitle();

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        User user = (User) adapterView.getAdapter().getItem(position);
        Intent intent = IntentUtils.getUserActivityIntent(this, user);
        if (intent != null) {
            startActivity(intent);
        }
    }
}