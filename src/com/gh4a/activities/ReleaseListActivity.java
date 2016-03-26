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

import org.eclipse.egit.github.core.Release;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.ReleaseAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.ReleaseListLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;
import com.gh4a.widget.SwipeRefreshLayout;

public class ReleaseListActivity extends BaseActivity implements
        SwipeRefreshLayout.ChildScrollDelegate, RootAdapter.OnItemClickListener<Release> {
    private String mUserLogin;
    private String mRepoName;
    private RecyclerView mRecyclerView;
    private ReleaseAdapter mAdapter;

    private LoaderCallbacks<List<Release>> mReleaseCallback = new LoaderCallbacks<List<Release>>(this) {
        @Override
        protected Loader<LoaderResult<List<Release>>> onCreateLoader() {
            return new ReleaseListLoader(ReleaseListActivity.this, mUserLogin, mRepoName);
        }

        @Override
        protected void onResultReady(List<Release> result) {
            fillData(result);
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setContentShown(false);
        setEmptyText(R.string.no_releases_found);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.releases);
        actionBar.setSubtitle(mUserLogin + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mAdapter = new ReleaseAdapter(this);
        mAdapter.setOnItemClickListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this));
        mRecyclerView.setAdapter(mAdapter);
        setChildScrollDelegate(this);

        getSupportLoaderManager().initLoader(0, null, mReleaseCallback);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mUserLogin = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(mRecyclerView);
    }

    @Override
    public void onRefresh() {
        mAdapter.clear();
        setContentShown(false);
        getSupportLoaderManager().getLoader(0).onContentChanged();
        super.onRefresh();
    }

    private void fillData(List<Release> result) {
        setContentEmpty(result == null || result.isEmpty());
        mAdapter.addAll(result);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mUserLogin, mRepoName, null);
    }

    @Override
    public void onItemClick(Release release) {
        Intent intent = new Intent(this, ReleaseInfoActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mUserLogin);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.putExtra(Constants.Release.RELEASE, release);
        startActivity(intent);
    }
}
