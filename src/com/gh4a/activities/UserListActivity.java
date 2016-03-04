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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;

import org.eclipse.egit.github.core.User;

import java.util.List;

public abstract class UserListActivity extends BaseActivity implements
        RootAdapter.OnItemClickListener<User> {
    private UserAdapter mUserAdapter;

    private LoaderCallbacks<List<User>> mUserListCallback = new LoaderCallbacks<List<User>>(this) {
        @Override
        protected Loader<LoaderResult<List<User>>> onCreateLoader() {
            return getUserListLoader();
        }

        @Override
        protected void onResultReady(List<User> result) {
            fillData(result);
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setContentShown(false);
        setRequestData();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getActionBarTitle());
        actionBar.setSubtitle(getSubTitle());
        actionBar.setDisplayHomeAsUpEnabled(true);

        mUserAdapter = new UserAdapter(this);
        mUserAdapter.setOnItemClickListener(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        recyclerView.setAdapter(mUserAdapter);
        recyclerView.setBackgroundResource(
                UiUtils.resolveDrawable(this, R.attr.listBackground));

        getSupportLoaderManager().initLoader(0, null, mUserListCallback);
    }

    @Override
    public void onRefresh() {
        mUserAdapter.clear();
        setContentShown(false);
        getSupportLoaderManager().getLoader(0).onContentChanged();
        super.onRefresh();
    }

    protected void fillData(List<User> users) {
        if (users != null) {
            mUserAdapter.addAll(users);
        }
    }

    protected abstract Loader<LoaderResult<List<User>>> getUserListLoader();

    protected abstract void setRequestData();

    protected abstract String getActionBarTitle();
    protected abstract String getSubTitle();

    @Override
    public void onItemClick(User user) {
        Intent intent = IntentUtils.getUserActivityIntent(this, user);
        if (intent != null) {
            startActivity(intent);
        }
    }
}