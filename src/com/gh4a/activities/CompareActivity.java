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

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryCommitCompare;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.loader.CommitCompareLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;

public class CompareActivity extends LoadingFragmentActivity implements OnItemClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private String mBase;
    private String mHead;
    private CommitAdapter mAdapter;
    private ListView mListView;

    private LoaderCallbacks<RepositoryCommitCompare> mCompareCallback =
            new LoaderCallbacks<RepositoryCommitCompare>() {
        @Override
        public Loader<LoaderResult<RepositoryCommitCompare>> onCreateLoader(int id, Bundle args) {
            return new CommitCompareLoader(CompareActivity.this, mRepoOwner, mRepoName, mBase, mHead);
        }
        @Override
        public void onResultReady(LoaderResult<RepositoryCommitCompare> result) {
            setContentEmpty(true);
            if (!result.handleError(CompareActivity.this)) {
                List<RepositoryCommit> commits = result.getData().getCommits();
                if (commits != null && !commits.isEmpty()) {
                    mAdapter.addAll(commits);
                    mAdapter.notifyDataSetChanged();
                    setContentEmpty(false);
                }
            }
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setContentShown(false);

        mListView = (ListView) findViewById(R.id.list_view);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.NAME);
        mBase = getIntent().getExtras().getString(Constants.Repository.BASE);
        mHead = getIntent().getExtras().getString(Constants.Repository.HEAD);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.commit_compare);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        mAdapter = new CommitAdapter(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        
        getSupportLoaderManager().initLoader(0, null, mCompareCallback);
    }
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        RepositoryCommit commit = (RepositoryCommit) mAdapter.getItem(position);
        IntentUtils.openCommitInfoActivity(this, mRepoOwner, mRepoName, commit.getSha(), 0);
    }
    
    @Override
    protected void navigateUp() {
        IntentUtils.openRepositoryInfoActivity(this, mRepoOwner, mRepoName,
                null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}