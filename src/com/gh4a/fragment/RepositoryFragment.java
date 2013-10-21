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
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import com.gh4a.CollaboratorListActivity;
import com.gh4a.Constants;
import com.gh4a.ContributorListActivity;
import com.gh4a.DownloadsActivity;
import com.gh4a.Gh4Application;
import com.gh4a.IssueListActivity;
import com.gh4a.R;
import com.gh4a.WatcherListActivity;
import com.gh4a.WikiListActivity;
import com.gh4a.loader.ReadmeLoader;
import com.gh4a.utils.StringUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class RepositoryFragment extends BaseFragment implements 
    OnClickListener, LoaderManager.LoaderCallbacks<String> {

    private Repository mRepository;
    private String mRepoOwner;
    private String mRepoName;
    private int mStargazerCount;
    private boolean mDataLoaded;
    
    public static RepositoryFragment newInstance(Repository repository) {
        RepositoryFragment f = new RepositoryFragment();

        Bundle args = new Bundle();
        args.putSerializable("REPOSITORY", repository);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = (Repository) getArguments().getSerializable("REPOSITORY");
        mRepoOwner = mRepository.getOwner().getLogin();
        mRepoName = mRepository.getName();
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
        
        fillData();
        
        Gh4Application app = (Gh4Application) getActivity().getApplicationContext();
        Typeface boldCondensed = app.boldCondensed;
        
        TextView tvReadmeTitle = (TextView) getView().findViewById(R.id.readme_title);
        tvReadmeTitle.setTypeface(boldCondensed);
        tvReadmeTitle.setTextColor(getResources().getColor(R.color.highlight));

        showLoading(R.id.pb_readme, R.id.readme);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mDataLoaded) {
            if (getLoaderManager().getLoader(0) == null) {
                getLoaderManager().initLoader(0, null, this);
            }
            else {
                getLoaderManager().restartLoader(0, null, this);
            }
            getLoaderManager().getLoader(0).forceLoad();
        }
    }
    
    public void fillData() {
        View v = getView();
        final Gh4Application app = (Gh4Application) getActivity().getApplicationContext();
        Typeface boldCondensed = app.boldCondensed;
        Typeface condensed = app.condensed;
        Typeface regular = app.regular;
        Typeface italic = app.italic;
        
        TextView tvOwner = (TextView) v.findViewById(R.id.tv_login);
        tvOwner.setText(mRepoOwner);
        tvOwner.setTextColor(getResources().getColor(R.color.highlight));
        tvOwner.setTypeface(boldCondensed);
        tvOwner.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                app.openUserInfoActivity(getSherlockActivity(),
                        mRepoOwner, null);
            }
        });
        TextView tvRepoName = (TextView) v.findViewById(R.id.tv_name);
        tvRepoName.setText(mRepoName);
        tvRepoName.setTypeface(boldCondensed);
        
        TextView tvParentRepo = (TextView) v.findViewById(R.id.tv_parent);
        
        TextView tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        tvDesc.setTypeface(regular);
        
        TextView tvUrl = (TextView) v.findViewById(R.id.tv_url);
        tvUrl.setTypeface(regular);
        
        TextView tvLanguage = (TextView) v.findViewById(R.id.tv_language);
        tvLanguage.setTypeface(regular);
        
        if (mRepository.isFork()) {
            tvParentRepo.setVisibility(View.VISIBLE);
            tvParentRepo.setTypeface(italic);

            if (mRepository.getParent() != null) {
                tvParentRepo.setText(app.getString(R.string.forked_from,
                        mRepository.getParent().getOwner().getLogin() + "/" +
                        mRepository.getParent().getName()));
                tvParentRepo.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
                tvParentRepo.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        app.openRepositoryInfoActivity(getSherlockActivity(),
                                mRepository.getParent().getOwner().getLogin(),
                                mRepository.getParent().getName(), 0);
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
            tvLanguage.setText(getString(R.string.repo_language, mRepository.getLanguage()));
            tvLanguage.setVisibility(View.VISIBLE);
        }
        else {
            tvLanguage.setVisibility(View.GONE);
        }
        
        tvUrl.setText(mRepository.getHtmlUrl());

        TableLayout tlStargazers = (TableLayout) v.findViewById(R.id.cell_stargazers);
        tlStargazers.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        tlStargazers.setOnClickListener(this);
        
        TextView tvStargazersCount = (TextView) v.findViewById(R.id.tv_stargazers_count);
        mStargazerCount = mRepository.getWatchers();
        tvStargazersCount.setText(String.valueOf(mStargazerCount));
        tvStargazersCount.setTypeface(boldCondensed);
        
        TableLayout tlForks = (TableLayout) v.findViewById(R.id.cell_forks);
        tlForks.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        tlForks.setOnClickListener(this);
        
        TextView tvForksCount = (TextView) v.findViewById(R.id.tv_forks_count);
        tvForksCount.setTypeface(boldCondensed);
        tvForksCount.setText(String.valueOf(mRepository.getForks()));
        
        TableLayout tlIssues = (TableLayout) v.findViewById(R.id.cell_issues);
        tlIssues.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        TextView tvIssues = (TextView) v.findViewById(R.id.tv_issues_label);
        TextView tvIssuesCount = (TextView) v.findViewById(R.id.tv_issues_count);
        
        if (mRepository.isHasIssues()) {
            tlIssues.setVisibility(View.VISIBLE);
            tlIssues.setOnClickListener(this);
            
            tvIssues.setVisibility(View.VISIBLE);
            
            tvIssuesCount.setTypeface(boldCondensed);
            tvIssuesCount.setText(String.valueOf(mRepository.getOpenIssues()));
            tvIssuesCount.setVisibility(View.VISIBLE);
        }
        else {
            tlIssues.setVisibility(View.GONE);
            tvIssues.setVisibility(View.GONE);
            tvIssuesCount.setVisibility(View.GONE);
        }
        
        TableLayout tlPullRequests = (TableLayout) v.findViewById(R.id.cell_pull_requests);
        tlPullRequests.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        tlPullRequests.setOnClickListener(this);
        
        TextView tvPullRequestsCount = (TextView) v.findViewById(R.id.tv_pull_requests_count);
        tvPullRequestsCount.setTypeface(boldCondensed);
        
        TextView tvPullRequests = (TextView) v.findViewById(R.id.tv_pull_requests_label);
        tvPullRequests.setTypeface(condensed);
        
        if (mRepository.isHasIssues()) {
            tlIssues.setVisibility(View.VISIBLE);
            tlIssues.setOnClickListener(this);
            
            tvIssues.setVisibility(View.VISIBLE);
            
            tvIssuesCount.setTypeface(boldCondensed);
            tvIssuesCount.setText(String.valueOf(mRepository.getOpenIssues()));
            tvIssuesCount.setVisibility(View.VISIBLE);
        }
        else {
            tlIssues.setVisibility(View.GONE);
            tvIssues.setVisibility(View.GONE);
            tvIssuesCount.setVisibility(View.GONE);
        }
        
        TextView tvWiki = (TextView) v.findViewById(R.id.tv_wiki_label);
        tvWiki.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        tvWiki.setPadding(0, 16, 0, 16);
        if (mRepository.isHasWiki()) {
            tvWiki.setTypeface(boldCondensed);
            tvWiki.setOnClickListener(this);
            tvWiki.setVisibility(View.VISIBLE);
        }
        else {
            tvWiki.setVisibility(View.GONE);
        }
        
        TextView tvContributor = (TextView) v.findViewById(R.id.tv_contributors_label);
        tvContributor.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        tvContributor.setPadding(0, 16, 0, 16);
        tvContributor.setOnClickListener(this);
        tvContributor.setTypeface(boldCondensed);
        
        TextView tvCollaborators = (TextView) v.findViewById(R.id.tv_collaborators_label);
        tvCollaborators.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        tvCollaborators.setPadding(0, 16, 0, 16);
        tvCollaborators.setOnClickListener(this);
        tvCollaborators.setTypeface(boldCondensed);
        
        TextView tvOthers = (TextView) v.findViewById(R.id.other_info);
        tvOthers.setTypeface(boldCondensed);
        tvOthers.setTextColor(getResources().getColor(R.color.highlight));
        
        TextView tvDownloads = (TextView) v.findViewById(R.id.tv_downloads_label);
        tvDownloads.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        tvDownloads.setPadding(0, 16, 0, 16);
        tvDownloads.setOnClickListener(this);
        tvDownloads.setTypeface(boldCondensed);
    }

    public void updateStargazerCount(boolean starring) {
        TextView tvStargazersCount = (TextView) getView().findViewById(R.id.tv_stargazers_count);
        if (starring) {
            tvStargazersCount.setText(String.valueOf(++mStargazerCount));
        }
        else {
            tvStargazersCount.setText(String.valueOf(--mStargazerCount));
        }
    }
    
    public void fillReadme(String readme) {
        if (readme != null) {
            if (getActivity() != null) {
                TextView tvReadme = (TextView) getView().findViewById(R.id.readme);
                tvReadme.setMovementMethod(LinkMovementMethod.getInstance());
                
                readme = HtmlUtils.format(readme).toString();
                HttpImageGetter imageGetter = new HttpImageGetter(getSherlockActivity());
                imageGetter.bind(tvReadme, readme, mRepository.getId());
            }
        }
        else {
            if (getView() != null) {
                TextView tvReadme = (TextView) getView().findViewById(R.id.readme);
                tvReadme.setText(R.string.repo_no_readme);
                tvReadme.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            }
        }
    }
    
    @Override
    public void onClick(View view) {
        int id = view.getId();
        Gh4Application app = (Gh4Application) getActivity().getApplicationContext();
        
        if (id == R.id.cell_pull_requests) {
            app.openPullRequestListActivity(getActivity(),
                    mRepoOwner, mRepoName,
                    Constants.Issue.ISSUE_STATE_OPEN);
        } else if (id == R.id.tv_contributors_label) {
            getContributors(view);
        } else if (id == R.id.tv_collaborators_label) {
            getCollaborators(view);
        } else if (id == R.id.cell_issues) {
            app.openIssueListActivity(getActivity(),
                    mRepoOwner, mRepoName,
                    Constants.Issue.ISSUE_STATE_OPEN);
        } else if (id == R.id.cell_stargazers) {
            getStargazers(view);
        } else if (id == R.id.cell_forks) {
            getNetworks(view);
        } else if (id == R.id.tv_wiki_label) {
            getWiki(view);
        } else if (id == R.id.tv_downloads_label) {
            getDownloads(view);
        }
    }

    public void getWiki(View view) {
        Intent intent = new Intent().setClass(getActivity(), WikiListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        startActivity(intent);
    }
    
    public void getStargazers(View view) {
        Intent intent = new Intent().setClass(getActivity(), WatcherListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra("pos", 0);
        startActivity(intent);
    }

    public void getNetworks(View view) {
        Intent intent = new Intent().setClass(getActivity(), WatcherListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra("pos", 2);
        startActivity(intent);
    }

    public void getOpenIssues(View view) {
        Intent intent = new Intent().setClass(getActivity(), IssueListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Issue.ISSUE_STATE, Constants.Issue.ISSUE_STATE_OPEN);
        startActivity(intent);
    }

    public void getContributors(View view) {
        Intent intent = new Intent().setClass(getActivity(), ContributorListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        startActivity(intent);
    }
    
    public void getCollaborators(View view) {
        Intent intent = new Intent().setClass(getActivity(), CollaboratorListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        startActivity(intent);
    }
    
    public void getDownloads(View view) {
        Intent intent = new Intent().setClass(getActivity(), DownloadsActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        startActivity(intent);
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle bundle) {
        return new ReadmeLoader(getSherlockActivity(), mRepoOwner, mRepoName);            
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String readme) {
        mDataLoaded = true;
        hideLoading(R.id.pb_readme, R.id.readme);
        if (readme != null) {
            fillReadme(readme);
        }
    }

    @Override
    public void onLoaderReset(Loader<String> arg0) {
        // TODO Auto-generated method stub
        
    }
}