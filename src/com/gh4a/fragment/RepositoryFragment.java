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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.egit.github.core.Content;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.util.EncodingUtils;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import com.gh4a.CollaboratorListActivity;
import com.gh4a.Constants;
import com.gh4a.ContributorListActivity;
import com.gh4a.Gh4Application;
import com.gh4a.IssueListActivity;
import com.gh4a.R;
import com.gh4a.RepositoryActivity;
import com.gh4a.WatcherListActivity;
import com.gh4a.WikiListActivity;
import com.gh4a.loader.IsWatchingLoader;
import com.gh4a.loader.ReadmeLoader;
import com.gh4a.loader.WatchLoader;
import com.gh4a.utils.StringUtils;
import com.petebevin.markdown.MarkdownProcessor;

public class RepositoryFragment extends BaseFragment implements 
    OnClickListener, LoaderManager.LoaderCallbacks {

    private Repository mRepository;
    private String mRepoOwner;
    private String mRepoName;
    private boolean isWatching;
    
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
        
        RepositoryActivity repoActivity = (RepositoryActivity) getSherlockActivity();
        LinearLayout llBtnActions = (LinearLayout) getView().findViewById(R.id.ll_btn_actions);
        ProgressBar pbActions = (ProgressBar) getView().findViewById(R.id.pb_actions);
        
        Gh4Application app = (Gh4Application) getActivity().getApplicationContext();
        Typeface boldCondensed = app.boldCondensed;
        
        TextView tvReadmeTitle = (TextView) getView().findViewById(R.id.readme_title);
        tvReadmeTitle.setTypeface(boldCondensed);
        tvReadmeTitle.setTextColor(Color.parseColor("#0099cc"));

        llBtnActions.setVisibility(View.GONE);
        if (mRepoOwner.equals(repoActivity.getAuthLogin())) {
            pbActions.setVisibility(View.GONE);
        }
        else {
            pbActions.setVisibility(View.VISIBLE);
            getLoaderManager().initLoader(1, null, this);
            getLoaderManager().getLoader(1).forceLoad();
        }
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
        
        getLoaderManager().initLoader(2, null, this);
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
        tvOwner.setTextColor(Color.parseColor("#0099cc"));
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
                tvParentRepo.setText("forked from "
                        + mRepository.getParent().getOwner().getLogin() 
                        + "/" 
                        + mRepository.getParent().getName());
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
            tvLanguage.setText(getResources().getString(R.string.repo_language) 
                    + " " + mRepository.getLanguage());
            tvLanguage.setVisibility(View.VISIBLE);
        }
        else {
            tvLanguage.setVisibility(View.GONE);
        }
        
        tvUrl.setText(mRepository.getHtmlUrl());

        TableLayout tlWatchers = (TableLayout) v.findViewById(R.id.cell_watchers);
        tlWatchers.setOnClickListener(this);
        
        TextView tvWatchers = (TextView) v.findViewById(R.id.tv_watchers_label);
        
        TextView tvWatchersCount = (TextView) v.findViewById(R.id.tv_watchers_count);
        tvWatchersCount.setText(String.valueOf(mRepository.getWatchers()));
        tvWatchersCount.setTypeface(boldCondensed);
        
        TableLayout tlForks = (TableLayout) v.findViewById(R.id.cell_forks);
        tlForks.setOnClickListener(this);
        
        TextView tvForks = (TextView) v.findViewById(R.id.tv_forks_label);
        
        TextView tvForksCount = (TextView) v.findViewById(R.id.tv_forks_count);
        tvForksCount.setTypeface(boldCondensed);
        tvForksCount.setText(String.valueOf(mRepository.getForks()));
        
        TableLayout tlIssues = (TableLayout) v.findViewById(R.id.cell_issues);
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
        if (mRepository.isHasWiki()) {
            tvWiki.setTypeface(boldCondensed);
            tvWiki.setOnClickListener(this);
            tvWiki.setVisibility(View.VISIBLE);
        }
        else {
            tvWiki.setVisibility(View.GONE);
        }
        
        TextView tvContributor = (TextView) v.findViewById(R.id.tv_contributors_label);
        tvContributor.setOnClickListener(this);
        tvContributor.setTypeface(boldCondensed);
        
        TextView tvCollaborators = (TextView) v.findViewById(R.id.tv_collaborators_label);
        tvCollaborators.setOnClickListener(this);
        tvCollaborators.setTypeface(boldCondensed);
        
        TextView tvOthers = (TextView) v.findViewById(R.id.other_info);
        tvOthers.setTypeface(boldCondensed);
        tvOthers.setTextColor(Color.parseColor("#0099cc"));
    }

    private static class FillReadmeTask extends AsyncTask<Content, Void, Spanned> {

        private WeakReference<RepositoryFragment> mTarget;
        private boolean mException;

        public FillReadmeTask(RepositoryFragment activity) {
            mTarget = new WeakReference<RepositoryFragment>(activity);
        }

        @Override
        protected Spanned doInBackground(Content... params) {
            if (mTarget.get() != null && params[0] != null) {
                String content = new String(EncodingUtils.fromBase64(params[0].getContent()));
                MarkdownProcessor m = new MarkdownProcessor();
                String html = m.markdown(content);
                Spanned readme = Html.fromHtml(html, new ImageGetter() {
                    @Override
                    public Drawable getDrawable(String source) {
                        try {
                            URL url = new URL(source);
                            Object content = url.getContent();
                            InputStream is = (InputStream) content;
                            Drawable drawable = Drawable.createFromStream(is, null);
                            if (drawable != null) {
                                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable
                                        .getIntrinsicHeight());
                                return drawable;
                            }
                            return null;
                            
                        } catch (MalformedURLException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        return null;
                    }
                }, null);
                
                return readme;
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().showLoading(R.id.pb_readme, R.id.readme);
            }
        }

        @Override
        protected void onPostExecute(Spanned result) {
            if (mTarget.get() != null) {
                mTarget.get().hideLoading(R.id.pb_readme, R.id.readme);
                mTarget.get().fillReadme(result);
            }
        }
    }
    
    public void fillReadme(Spanned readme) {
        if (readme != null) {
            if (getActivity() != null) {
                final Gh4Application app = (Gh4Application) getActivity().getApplicationContext();
                Typeface regular = app.regular;
                
                TextView tvReadme = (TextView) getView().findViewById(R.id.readme);
                tvReadme.setTypeface(regular);
                tvReadme.setText(readme);
                
                tvReadme.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        else {
            if (getView() != null) {
                TextView tvReadme = (TextView) getView().findViewById(R.id.readme);
                tvReadme.setText("README not found");
                tvReadme.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            }
        }
    }
    
    @Override
    public void onClick(View view) {
        int id = view.getId();
        Gh4Application app = (Gh4Application) getActivity().getApplicationContext();
        
        switch (id) {
        case R.id.btn_commits:
            app.openBranchListActivity(getActivity(),
                    mRepoOwner, mRepoName, R.id.btn_commits);
            break;
        case R.id.cell_pull_requests:
            app.openPullRequestListActivity(getActivity(),
                    mRepoOwner, mRepoName,
                    Constants.Issue.ISSUE_STATE_OPEN);
            break;
        case R.id.tv_contributors_label:
            getContributors(view);
            break;
        case R.id.tv_collaborators_label:
            getCollaborators(view);
            break;
        case R.id.cell_issues:
            app.openIssueListActivity(getActivity(),
                    mRepoOwner, mRepoName,
                    Constants.Issue.ISSUE_STATE_OPEN);
            break;
        case R.id.cell_watchers:
            getWatchers(view);
            break;
        case R.id.cell_forks:
            getNetworks(view);
            break;
        case R.id.tv_wiki_label:
            getWiki(view);
            break;
        default:
            break;
        }
    }

    public void getWiki(View view) {
        Intent intent = new Intent().setClass(getActivity(), WikiListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        startActivity(intent);
    }
    
    public void getWatchers(View view) {
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
        intent.putExtra("pos", 1);
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

    private void updateWatchBtn() {
        LinearLayout llBtnActions = (LinearLayout) getView().findViewById(R.id.ll_btn_actions);
        ProgressBar pbActions = (ProgressBar) getView().findViewById(R.id.pb_actions);
        pbActions.setVisibility(View.GONE);
        llBtnActions.setVisibility(View.VISIBLE);
        
        Button btnWatch = (Button) getView().findViewById(R.id.btn_watch);
        btnWatch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getLoaderManager().restartLoader(2, null, RepositoryFragment.this);
                getLoaderManager().getLoader(2).forceLoad();
            }
        });
        
        Button btnFollow = (Button) getView().findViewById(R.id.btn_watch);
        if (isWatching) {
            btnFollow.setText(R.string.repo_unwatch_action);
        }
        else {
            btnFollow.setText(R.string.repo_watch_action);
        }
    }
    
    @Override
    public Loader onCreateLoader(int id, Bundle bundle) {
        if (id == 1) {
            return new IsWatchingLoader(getSherlockActivity(), mRepoOwner, mRepoName);
        }
        else if (id == 2) {
            return new WatchLoader(getSherlockActivity(), mRepoOwner, mRepoName, isWatching);
        }
        else {
            return new ReadmeLoader(getSherlockActivity(), mRepoOwner, mRepoName);            
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        if (loader.getId() == 1) {
            isWatching = (Boolean) object;
            updateWatchBtn();
        }
        else if (loader.getId() == 2) {
            isWatching = (Boolean) object;
            updateWatchBtn();
        }
        else {
            new FillReadmeTask(this).execute((Content) object);
        }
    }

    @Override
    public void onLoaderReset(Loader arg0) {
        // TODO Auto-generated method stub
        
    }
}