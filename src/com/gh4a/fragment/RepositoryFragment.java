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

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.Loader;
import android.support.v4.os.AsyncTaskCompat;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.activities.CollaboratorListActivity;
import com.gh4a.activities.ContributorListActivity;
import com.gh4a.activities.DownloadsActivity;
import com.gh4a.activities.ForkListActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.WatcherListActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestCountLoader;
import com.gh4a.loader.ReadmeLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

import org.eclipse.egit.github.core.Permissions;
import org.eclipse.egit.github.core.Repository;

public class RepositoryFragment extends LoadingFragmentBase implements OnClickListener {
    private Repository mRepository;
    private View mContentView;
    private String mRef;
    private HttpImageGetter mImageGetter;

    private LoaderCallbacks<String> mReadmeCallback = new LoaderCallbacks<String>() {
        @Override
        public Loader<LoaderResult<String>> onCreateLoader(int id, Bundle args) {
            return new ReadmeLoader(getActivity(), mRepository.getOwner().getLogin(),
                    mRepository.getName(), StringUtils.isBlank(mRef) ? mRepository.getMasterBranch() : mRef);
        }
        @Override
        public void onResultReady(LoaderResult<String> result) {
            TextView readmeView = (TextView) mContentView.findViewById(R.id.readme);
            View progress = mContentView.findViewById(R.id.pb_readme);
            AsyncTaskCompat.executeParallel(new FillReadmeTask(
                    mRepository.getId(), readmeView, progress, mImageGetter), result.getData());
        }
    };

    private LoaderCallbacks<Integer> mPullRequestsCallback = new LoaderCallbacks<Integer>() {
        @Override
        public Loader<LoaderResult<Integer>> onCreateLoader(int id, Bundle args) {
            return new PullRequestCountLoader(getActivity(), mRepository, Constants.Issue.STATE_OPEN);
        }

        @Override
        public void onResultReady(LoaderResult<Integer> result) {
            View v = getView();
            v.findViewById(R.id.pull_requests_progress).setVisibility(View.GONE);

            TextView tvPullRequestsCountView = (TextView) v.findViewById(R.id.tv_pull_requests_count);
            tvPullRequestsCountView.setText(String.valueOf(result.getData()));
        }
    };

    private ClickableSpan mLoginClickSpan = new ClickableSpan() {
        @Override
        public void onClick(View view) {
            Context context = getActivity();
            if (context != null) {
                startActivity(IntentUtils.getUserActivityIntent(context, mRepository.getOwner()));
            }
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            Context context = getActivity();
            if (context != null) {
                ds.setColor(UiUtils.resolveColor(context, android.R.attr.textColorLink));
            }
        }
    };

    public static RepositoryFragment newInstance(Repository repository, String ref) {
        RepositoryFragment f = new RepositoryFragment();

        Bundle args = new Bundle();
        args.putSerializable("REPOSITORY", repository);
        args.putString(Constants.Object.REF, ref);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = (Repository) getArguments().getSerializable("REPOSITORY");
        mRef = getArguments().getString(Constants.Object.REF);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.repository, null);
        mImageGetter = new HttpImageGetter(inflater.getContext());
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mImageGetter.destroy();
        mImageGetter = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentView(mContentView);
        fillData();
        setContentShownNoAnimation(true);

        getLoaderManager().initLoader(0, null, mReadmeCallback);
        getLoaderManager().initLoader(1, null, mPullRequestsCallback);
    }

    public void setRef(String ref) {
        mRef = ref;
        getArguments().putString(Constants.Object.REF, ref);
        // reload readme
        getLoaderManager().restartLoader(0, null, mReadmeCallback);
        if (mContentView != null) {
            mContentView.findViewById(R.id.readme).setVisibility(View.GONE);
            mContentView.findViewById(R.id.pb_readme).setVisibility(View.VISIBLE);
        }
    }

    private void fillData() {
        TextView tvRepoName = (TextView) mContentView.findViewById(R.id.tv_repo_name);
        SpannableStringBuilder repoName = new SpannableStringBuilder();
        repoName.append(mRepository.getOwner().getLogin());
        repoName.append("/");
        repoName.append(mRepository.getName());
        repoName.setSpan(mLoginClickSpan, 0, mRepository.getOwner().getLogin().length(), 0);
        tvRepoName.setText(repoName);
        tvRepoName.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);

        TextView tvParentRepo = (TextView) mContentView.findViewById(R.id.tv_parent);
        if (mRepository.isFork() && mRepository.getParent() != null) {
            Repository parent = mRepository.getParent();
            tvParentRepo.setVisibility(View.VISIBLE);
            tvParentRepo.setText(getString(R.string.forked_from,
                    parent.getOwner().getLogin() + "/" + parent.getName()));
            tvParentRepo.setOnClickListener(this);
            tvParentRepo.setTag(parent);
        } else {
            tvParentRepo.setVisibility(View.GONE);
        }

        fillTextView(R.id.tv_desc, 0, mRepository.getDescription());
        fillTextView(R.id.tv_language,R.string.repo_language, mRepository.getLanguage());
        fillTextView(R.id.tv_url, 0, !StringUtils.isBlank(mRepository.getHomepage())
                ? mRepository.getHomepage() : mRepository.getHtmlUrl());

        mContentView.findViewById(R.id.cell_stargazers).setOnClickListener(this);
        mContentView.findViewById(R.id.cell_forks).setOnClickListener(this);
        mContentView.findViewById(R.id.cell_pull_requests).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_contributors_label).setOnClickListener(this);
        mContentView.findViewById(R.id.other_info).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_releases_label).setOnClickListener(this);

        Permissions permissions = mRepository.getPermissions();
        updateClickableLabel(R.id.tv_collaborators_label,
                permissions != null && permissions.hasPushAccess());
        updateClickableLabel(R.id.tv_downloads_label, mRepository.isHasDownloads());
        updateClickableLabel(R.id.tv_wiki_label, mRepository.isHasWiki());

        TextView tvStargazersCount = (TextView) mContentView.findViewById(R.id.tv_stargazers_count);
        tvStargazersCount.setText(String.valueOf(mRepository.getWatchers()));

        TextView tvForksCount = (TextView) mContentView.findViewById(R.id.tv_forks_count);
        tvForksCount.setText(String.valueOf(mRepository.getForks()));

        TextView tvIssues = (TextView) mContentView.findViewById(R.id.tv_issues_label);
        TextView tvIssuesCount = (TextView) mContentView.findViewById(R.id.tv_issues_count);
        LinearLayout llIssues = (LinearLayout) mContentView.findViewById(R.id.cell_issues);

        if (mRepository.isHasIssues()) {
            llIssues.setVisibility(View.VISIBLE);
            llIssues.setOnClickListener(this);

            tvIssues.setVisibility(View.VISIBLE);

            tvIssuesCount.setText(String.valueOf(mRepository.getOpenIssues()));
            tvIssuesCount.setVisibility(View.VISIBLE);
        } else {
            llIssues.setVisibility(View.GONE);
            tvIssues.setVisibility(View.GONE);
            tvIssuesCount.setVisibility(View.GONE);
        }
    }

    private void updateClickableLabel(int id, boolean enable) {
        View view = mContentView.findViewById(id);
        if (enable) {
            view.setOnClickListener(this);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void fillTextView(int id, int stringId, String text) {
        TextView view = (TextView) mContentView.findViewById(id);

        if (!StringUtils.isBlank(text)) {
            view.setText(stringId != 0 ? getString(stringId, text) : text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    public void updateStargazerCount(boolean starring) {
        if (starring) {
            mRepository.setWatchers(mRepository.getWatchers() + 1);
        } else {
            mRepository.setWatchers(mRepository.getWatchers() - 1);
        }

        TextView tvStargazersCount = (TextView) mContentView.findViewById(R.id.tv_stargazers_count);
        tvStargazersCount.setText(String.valueOf(mRepository.getWatchers()));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        String owner = mRepository.getOwner().getLogin();
        String name = mRepository.getName();
        Intent intent = null;

        if (id == R.id.cell_pull_requests) {
            intent = IntentUtils.getPullRequestListActivityIntent(getActivity(), owner, name,
                    Constants.Issue.STATE_OPEN);
        } else if (id == R.id.tv_contributors_label) {
            intent = new Intent(getActivity(), ContributorListActivity.class);
        } else if (id == R.id.tv_collaborators_label) {
            intent = new Intent(getActivity(), CollaboratorListActivity.class);
        } else if (id == R.id.cell_issues) {
            intent = IntentUtils.getIssueListActivityIntent(getActivity(), owner, name,
                    Constants.Issue.STATE_OPEN);
        } else if (id == R.id.cell_stargazers) {
            intent = new Intent(getActivity(), WatcherListActivity.class);
        } else if (id == R.id.cell_forks) {
            intent = new Intent(getActivity(), ForkListActivity.class);
        } else if (id == R.id.tv_wiki_label) {
            intent = new Intent(getActivity(), WikiListActivity.class);
        } else if (id == R.id.tv_downloads_label) {
            intent = new Intent(getActivity(), DownloadsActivity.class);
        } else if (id == R.id.tv_releases_label) {
            intent = new Intent(getActivity(), ReleaseListActivity.class);
        } else if (view.getTag() instanceof Repository) {
            Repository repo = (Repository) view.getTag();
            IntentUtils.openRepositoryInfoActivity(getActivity(), repo);
        }

        if (intent != null) {
            intent.putExtra(Constants.Repository.OWNER, owner);
            intent.putExtra(Constants.Repository.NAME, name);
            startActivity(intent);
        }
    }

    private static class FillReadmeTask extends AsyncTask<String, Void, String> {
        private Long mId;
        private TextView mReadmeView;
        private View mProgressView;
        private HttpImageGetter mImageGetter;

        public FillReadmeTask(long id, TextView readmeView, View progressView,
                HttpImageGetter imageGetter) {
            mId = id;
            mReadmeView = readmeView;
            mProgressView = progressView;
            mImageGetter = imageGetter;
        }

        @Override
        protected String doInBackground(String... params) {
            String readme = params[0];
            if (readme != null) {
                readme = HtmlUtils.format(readme).toString();
                mImageGetter.encode(mId, readme);
            }
            return readme;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mReadmeView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
                mImageGetter.bind(mReadmeView, result, mId);
            } else {
                mReadmeView.setText(R.string.repo_no_readme);
                mReadmeView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            }
            mReadmeView.setVisibility(View.VISIBLE);
            mProgressView.setVisibility(View.GONE);
        }
    }
}