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
package com.gh4a;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.adapter.PullRequestAdapter;
import com.gh4a.loader.PageIteratorLoader;

public class PullRequestListActivity extends BaseSherlockFragmentActivity 
    implements OnItemClickListener, LoaderManager.LoaderCallbacks<List<PullRequest>> {

    private String mRepoOwner;
    private String mRepoName;
    private String mState;
    private PullRequestAdapter mPullRequestAdapter;
    private PageIterator<PullRequest> mDataIterator;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();

        ListView listView = (ListView) findViewById(R.id.list_view);

        listView.setOnItemClickListener(this);

        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mState = getIntent().getExtras().getString(Constants.PullRequest.STATE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.pull_requests);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        mPullRequestAdapter = new PullRequestAdapter(this, new ArrayList<PullRequest>());
        listView.setAdapter(mPullRequestAdapter);

        loadData(mState);
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().getLoader(0).forceLoad();
    }
    
    private void loadData(String state) {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        PullRequestService pullRequestService = new PullRequestService(client);
        mDataIterator = pullRequestService.pagePullRequests(new RepositoryId(mRepoOwner, mRepoName), mState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        PullRequest pullRequest = (PullRequest) mPullRequestAdapter.getItem(position);
        getApplicationContext().openPullRequestActivity(PullRequestListActivity.this, mRepoOwner,
                mRepoName, pullRequest.getNumber());
    }

    private void fillData(List<PullRequest> pullRequests) {
        if (pullRequests != null && !pullRequests.isEmpty()) {
            for (PullRequest pullRequest : pullRequests) {
                mPullRequestAdapter.add(pullRequest);
            }
            mPullRequestAdapter.notifyDataSetChanged();
        }
        else {
            getApplicationContext().notFoundMessage(this, "Pull Requests");
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.pull_requests_menu, menu);
        if ("open".equals(mState)) {
            menu.removeItem(R.id.view_open_issues);
        }
        else {
            menu.removeItem(R.id.view_closed_issues);
        }
        return true;
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        mPullRequestAdapter.getObjects().clear();
        
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openRepositoryInfoActivity(this, mRepoOwner, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.view_open_issues:
                getApplicationContext().openPullRequestListActivity(this, mRepoOwner, mRepoName,
                        Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.view_closed_issues:
                getApplicationContext().openPullRequestListActivity(this, mRepoOwner, mRepoName,
                        Constants.Issue.ISSUE_STATE_CLOSED, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            default:
                return true;
        }
    }

    @Override
    public Loader<List<PullRequest>> onCreateLoader(int id, Bundle arg1) {
        return new PageIteratorLoader<PullRequest>(this, mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<PullRequest>> loader, List<PullRequest> pullRequests) {
        hideLoading();
        fillData(pullRequests);
    }

    @Override
    public void onLoaderReset(Loader<List<PullRequest>> arg0) {
        // TODO Auto-generated method stub
        
    }
}
