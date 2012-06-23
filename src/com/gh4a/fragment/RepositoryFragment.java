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

import org.eclipse.egit.github.core.Repository;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.gh4a.CollaboratorListActivity;
import com.gh4a.Constants;
import com.gh4a.ContributorListActivity;
import com.gh4a.ForkListActivity;
import com.gh4a.Gh4Application;
import com.gh4a.IssueListActivity;
import com.gh4a.R;
import com.gh4a.WatcherListActivity;
import com.gh4a.WikiListActivity;
import com.gh4a.loader.RepositoryLoader;
import com.gh4a.utils.StringUtils;

public class RepositoryFragment extends SherlockFragment implements 
    OnClickListener, OnItemClickListener, LoaderManager.LoaderCallbacks<Repository> {

    private String mRepoOwner;
    private String mRepoName;
    private Repository mRepository;
    
    public static RepositoryFragment newInstance(String repoOwner, String repoName) {
        RepositoryFragment f = new RepositoryFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.repository, container, false);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }

    /**
     * Fill data into UI components.
     */
    public void fillData() {
        View v = getView();
        final Gh4Application app = (Gh4Application) getActivity().getApplicationContext();
        
        ImageButton btnBranches = (ImageButton) v.findViewById(R.id.btn_branches);
        btnBranches.setOnClickListener(this);

        ImageButton btnTags = (ImageButton) v.findViewById(R.id.btn_tags);
        btnTags.setOnClickListener(this);

        ImageButton btnPullRequests = (ImageButton) v.findViewById(R.id.btn_pull_requests);
        btnPullRequests.setOnClickListener(this);

        Button btnWatchers = (Button) v.findViewById(R.id.btn_watchers);
        btnWatchers.setOnClickListener(this);

        Button btnForks = (Button) v.findViewById(R.id.btn_forks);
        btnForks.setOnClickListener(this);

        Button btnOpenIssues = (Button) v.findViewById(R.id.btn_open_issues);
        btnOpenIssues.setOnClickListener(this);
        
        ImageButton btnCollaborators = (ImageButton) v.findViewById(R.id.btn_collaborators);
        btnCollaborators.setOnClickListener(this);
        
        ImageButton btnContributors = (ImageButton) v.findViewById(R.id.btn_contributors);
        btnContributors.setOnClickListener(this);

        TextView tvOwner = (TextView) v.findViewById(R.id.tv_login);
        tvOwner.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                app.openUserInfoActivity(getSherlockActivity(),
                        mRepoOwner, null);
            }
        });
        TextView tvRepoName = (TextView) v.findViewById(R.id.tv_name);
        TextView tvParentRepo = (TextView) v.findViewById(R.id.tv_parent);
        TextView tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        TextView tvUrl = (TextView) v.findViewById(R.id.tv_url);
        TextView tvLanguage = (TextView) v.findViewById(R.id.tv_language);
        
        tvOwner.setText(mRepoOwner);
        tvRepoName.setText(mRepoName);
        
        if (mRepository.isFork()) {
            tvParentRepo.setVisibility(View.VISIBLE);
            if (mRepository.getParent() != null) {
                tvParentRepo.setText("forked from "
                        + mRepository.getParent().getName());
                tvParentRepo.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        String[] repoPart = mRepository.getName().split("/");
                        app.openRepositoryInfoActivity(getSherlockActivity(),
                                repoPart[0], repoPart[1]);
                    }
                });
            }
        }
        else {
            tvParentRepo.setVisibility(View.GONE);
        }
        
        if (!StringUtils.isBlank(mRepository.getDescription())) {
            tvDesc.setText(mRepository.getDescription());
            tvDesc.setVisibility(View.VISIBLE);
        }
        else {
            tvDesc.setVisibility(View.GONE);
        }
        
        if (!StringUtils.isBlank(mRepository.getLanguage())) {
            tvLanguage.setText(getResources().getString(R.string.repo_language) 
                    + " " + mRepository.getLanguage());
            tvLanguage.setVisibility(View.VISIBLE);
        }
        else {
            tvLanguage.setVisibility(View.GONE);
        }
        
        tvUrl.setText(mRepository.getHtmlUrl());

        btnWatchers.setText(String.valueOf(mRepository.getWatchers()));
        btnForks.setText(String.valueOf(mRepository.getForks()));

        if (mRepository.isHasIssues()) {
            btnOpenIssues.setText(String.valueOf(mRepository.getOpenIssues()));
        }
        else {
            RelativeLayout rlOpenIssues = (RelativeLayout) v.findViewById(R.id.rl_open_issues);
            rlOpenIssues.setVisibility(View.GONE);
        }
        
        if (mRepository.isHasWiki()) {
            ImageButton btnWiki = (ImageButton) v.findViewById(R.id.btn_wiki);
            btnWiki.setOnClickListener(this);
        }
        else {
            RelativeLayout rlWiki = (RelativeLayout) v.findViewById(R.id.rl_wiki);
            rlWiki.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        Gh4Application app = (Gh4Application) getActivity().getApplicationContext();
        
        switch (id) {
        case R.id.btn_branches:
            app.openBranchListActivity(getActivity(),
                    mRepoOwner, mRepoName, R.id.btn_branches);
            break;
        case R.id.btn_tags:
            app.openTagListActivity(getActivity(),
                    mRepoOwner, mRepoName, R.id.btn_tags);
            break;
        case R.id.btn_commits:
            app.openBranchListActivity(getActivity(),
                    mRepoOwner, mRepoName, R.id.btn_commits);
            break;
        case R.id.btn_pull_requests:
            app.openPullRequestListActivity(getActivity(),
                    mRepoOwner, mRepoName,
                    Constants.Issue.ISSUE_STATE_OPEN);
            break;
        case R.id.btn_watchers:
            getWatchers(view);
            break;
        case R.id.btn_forks:
            getNetworks(view);
            break;
        case R.id.btn_contributors:
            getContributors(view);
            break;
        case R.id.btn_collaborators:
            getCollaborators(view);
            break;
        case R.id.btn_open_issues:
            app.openIssueListActivity(getActivity(),
                    mRepoOwner, mRepoName,
                    Constants.Issue.ISSUE_STATE_OPEN);
            break;
        case R.id.btn_wiki:
            Intent intent = new Intent().setClass(getActivity(), WikiListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            startActivity(intent);
            break;
        default:
            break;
        }
    }

    /**
     * Gets the watchers when Watchers button clicked.
     * 
     * @param view the view
     * @return the watchers
     */
    public void getWatchers(View view) {
        Intent intent = new Intent().setClass(getActivity(), WatcherListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        startActivity(intent);
    }

    /**
     * Gets the networks when Networkd button clicked.
     * 
     * @param view the view
     * @return the networks
     */
    public void getNetworks(View view) {
        Intent intent = new Intent().setClass(getActivity(), ForkListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        startActivity(intent);
    }

    /**
     * Gets the open issues when Open Issues button clicked.
     * 
     * @param view the view
     * @return the open issues
     */
    public void getOpenIssues(View view) {
        Intent intent = new Intent().setClass(getActivity(), IssueListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Issue.ISSUE_STATE, Constants.Issue.ISSUE_STATE_OPEN);
        startActivity(intent);
    }

    /**
     * Gets the contributors.
     *
     * @param view the view
     * @return the contributors
     */
    public void getContributors(View view) {
        Intent intent = new Intent().setClass(getActivity(), ContributorListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        startActivity(intent);
    }
    
    /**
     * Gets the collaborators.
     *
     * @param view the view
     * @return the collaborators
     */
    public void getCollaborators(View view) {
        Intent intent = new Intent().setClass(getActivity(), CollaboratorListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        startActivity(intent);
    }

    @Override
    public Loader<Repository> onCreateLoader(int arg0, Bundle arg1) {
        return new RepositoryLoader(getSherlockActivity(), mRepoOwner, mRepoName);
    }

    @Override
    public void onLoadFinished(Loader<Repository> loader, Repository repository) {
        if (repository != null) {
            this.mRepository = repository;
            fillData();
        }
    }

    @Override
    public void onLoaderReset(Loader<Repository> arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // TODO Auto-generated method stub
        
    }
}