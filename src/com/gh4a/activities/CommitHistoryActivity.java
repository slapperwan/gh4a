package com.gh4a.activities;

import java.util.List;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.loader.PageIteratorLoader;

public class CommitHistoryActivity extends BaseSherlockFragmentActivity 
    implements LoaderManager.LoaderCallbacks<List<RepositoryCommit>>, OnItemClickListener, OnScrollListener {

    private String mRepoOwner;
    private String mRepoName;
    private String mFilePath;
    private String mRef;
    private PageIterator<RepositoryCommit> mDataIterator;
    private boolean isLoadCompleted;
    private CommitAdapter mCommitAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mFilePath = getIntent().getExtras().getString(Constants.Object.PATH);
        mRef = getIntent().getExtras().getString(Constants.Object.REF);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.history);
        actionBar.setSubtitle(mFilePath);
        
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);

        mCommitAdapter = new CommitAdapter(this);
        listView.setAdapter(mCommitAdapter);
        
        loadData();
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().getLoader(0).forceLoad();
    }
    
    public void loadData() {
        CommitService commitService = (CommitService)
                Gh4Application.get(this).getService(Gh4Application.COMMIT_SERVICE);
        mDataIterator = commitService.pageCommits(new RepositoryId(mRepoOwner, mRepoName), 
                mRef, mFilePath);
    }
    
    protected void fillData(List<RepositoryCommit> commits) {
        if (commits != null && commits.size() > 0) {
            mCommitAdapter.addAll(commits);
        }
        mCommitAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        RepositoryCommit commit = (RepositoryCommit) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(CommitHistoryActivity.this, CommitActivity.class);

        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Object.OBJECT_SHA, commit.getSha());
        intent.putExtra(Constants.Object.TREE_SHA, commit.getCommit().getTree().getSha());

        startActivity(intent);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
        boolean loadMore = firstVisible + visibleCount >= totalCount;

        if(loadMore) {
            if (getLoaderManager().getLoader(0) != null
                    && isLoadCompleted) {
                isLoadCompleted = false;
                getLoaderManager().getLoader(0).forceLoad();
            }
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView arg0, int arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Loader<List<RepositoryCommit>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<RepositoryCommit>(this, mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<RepositoryCommit>> loader,
            List<RepositoryCommit> commits) {
        isLoadCompleted = true;
        hideLoading();
        fillData(commits);
    }

    @Override
    public void onLoaderReset(Loader<List<RepositoryCommit>> arg0) {
        // TODO Auto-generated method stub
        
    }
}
