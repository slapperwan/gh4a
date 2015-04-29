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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.ReleaseAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.ReleaseLoader;
import com.gh4a.utils.IntentUtils;

public class ReleaseListActivity extends BaseActivity implements AdapterView.OnItemClickListener {
    private String mUserLogin;
    private String mRepoName;
    private ReleaseAdapter mAdapter;

    private LoaderCallbacks<List<Release>> mReleaseCallback = new LoaderCallbacks<List<Release>>() {
        @Override
        public Loader<LoaderResult<List<Release>>> onCreateLoader(int id, Bundle args) {
            return new ReleaseLoader(ReleaseListActivity.this, mUserLogin, mRepoName);
        }

        @Override
        public void onResultReady(LoaderResult<List<Release>> result) {
            boolean success = !result.handleError(ReleaseListActivity.this);
            setContentEmpty(true);
            if (success) {
                fillData(result.getData());
            }
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserLogin = getIntent().getStringExtra(Constants.Repository.OWNER);
        mRepoName = getIntent().getStringExtra(Constants.Repository.NAME);

        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.generic_list);
        setContentShown(false);
        setEmptyText(R.string.no_releases_found);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.releases);
        actionBar.setSubtitle(mUserLogin + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ListView listView = (ListView) findViewById(android.R.id.list);
        mAdapter = new ReleaseAdapter(this);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        getSupportLoaderManager().initLoader(0, null, mReleaseCallback);
    }

    private void fillData(List<Release> result) {
        if (result != null && !result.isEmpty()) {
            mAdapter.addAll(result);
            mAdapter.notifyDataSetChanged();
            setContentEmpty(false);
        }
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mUserLogin, mRepoName, null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Release release = mAdapter.getItem(position);
        Intent intent = new Intent(this, ReleaseInfoActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mUserLogin);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.putExtra(Constants.Release.RELEASE, release);
        startActivity(intent);
    }
}
