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

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.PageIterator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.CommitActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.loader.RepositoryCommitsLoader;

public class PullRequestCommitListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<RepositoryCommit>>, OnItemClickListener {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private ListView mListView;
    private CommitAdapter mAdapter;
    private PageIterator<RepositoryCommit> mDataIterator;
    
    public static PullRequestCommitListFragment newInstance(String repoOwner, String repoName, int pullRequestNumber) {
        
        PullRequestCommitListFragment f = new PullRequestCommitListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        args.putInt(Constants.Issue.ISSUE_NUMBER, pullRequestNumber);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
        mPullRequestNumber = getArguments().getInt(Constants.Issue.ISSUE_NUMBER);
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
        
        mAdapter = new CommitAdapter(getSherlockActivity(), new ArrayList<RepositoryCommit>());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    private void fillData(List<RepositoryCommit> commits) {
        BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) getSherlockActivity();
        activity.hideLoading();
        if (commits != null && !commits.isEmpty()) {
            mAdapter.clear();
            mAdapter.addAll(commits);
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
        }
    }

    @Override
    public Loader<List<RepositoryCommit>> onCreateLoader(int id, Bundle args) {
        return new RepositoryCommitsLoader(getSherlockActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
    }

    @Override
    public void onLoadFinished(Loader<List<RepositoryCommit>> loader, List<RepositoryCommit> commits) {
        hideLoading();
        fillData(commits);
    }

    @Override
    public void onLoaderReset(Loader<List<RepositoryCommit>> arg0) {
        // TODO Auto-generated method stub
    }
    
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        RepositoryCommit commit = (RepositoryCommit) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(getSherlockActivity(), CommitActivity.class);
        
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Object.OBJECT_SHA, commit.getSha());
        intent.putExtra(Constants.Object.TREE_SHA, commit.getCommit().getTree().getSha());

        startActivity(intent);
    }
}