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

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.CollaboratorListActivity;
import com.gh4a.activities.ContributorListActivity;
import com.gh4a.activities.DownloadsActivity;
import com.gh4a.activities.ForkListActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.activities.WatcherListActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.service.RepositoryService;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.IntentSpan;
import com.vdurmont.emoji.EmojiParser;
import org.eclipse.egit.github.core.Permissions;
import org.eclipse.egit.github.core.Repository;
import io.reactivex.functions.Consumer;

public class RepositoryFragment extends LoadingFragmentBase implements OnClickListener {
    public static RepositoryFragment newInstance(Repository repository, String ref) {
        RepositoryFragment f = new RepositoryFragment();

        Bundle args = new Bundle();
        args.putSerializable("repo", repository);
        args.putString("ref", ref);
        f.setArguments(args);

        return f;
    }

    private Repository mRepository;
    private View mContentView;
    private String mRef;
    private HttpImageGetter mImageGetter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = (Repository) getArguments().getSerializable("repo");
        mRef = getArguments().getString("ref");
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, ViewGroup parent) {
        mContentView = inflater.inflate(R.layout.repository, parent, false);
        return mContentView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mImageGetter.destroy();
        mImageGetter = null;
    }

    @Override
    public void onRefresh() {
        if (mContentView != null) {
            mContentView.findViewById(R.id.readme).setVisibility(View.GONE);
            mContentView.findViewById(R.id.pb_readme).setVisibility(View.VISIBLE);
            mContentView.findViewById(R.id.pull_requests_progress).setVisibility(View.VISIBLE);
        }
        if (mImageGetter != null) {
            mImageGetter.clearHtmlCache();
        }

        reloadData();
    }

    private void reloadData() {
        setContentShown(false);

        loadReadme(true);
        loadPullRequestsCount(true);
    }

    public void loadReadme(boolean refresh) {
        Consumer consumer = result -> { // Fill-in readme
            setContentShown(true);
            TextView readmeView = (TextView) mContentView.findViewById(R.id.readme);
            View progress = mContentView.findViewById(R.id.pb_readme);
            if (result != null)
                mImageGetter.encode(getContext(), mRepository.getId(),
                        String.valueOf(result));

            if (result != null) {
                readmeView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
                mImageGetter.bind(readmeView, String.valueOf(result), mRepository.getId());
            } else {
                readmeView.setText(R.string.repo_no_readme);
                readmeView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            }
            readmeView.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
        };

        RepositoryService.loadReadme(
            getActivity(),
            mRepository.getOwner().getLogin(),
            mRepository.getName(),
            StringUtils.isBlank(mRef) ? mRepository.getDefaultBranch() : mRef,
            refresh
        ).subscribe(consumer, error -> {});
    }

    private void loadPullRequestsCount(boolean refresh) {
        Consumer consumer = result -> {
            View v = getView();
            v.findViewById(R.id.issues_progress).setVisibility(View.GONE);
            v.findViewById(R.id.pull_requests_progress).setVisibility(View.GONE);

            TextView tvIssuesCount = (TextView) mContentView.findViewById(R.id.tv_issues_count);
            tvIssuesCount.setText(String.valueOf(mRepository.getOpenIssues() - (Integer) result));

            TextView tvPullRequestsCountView = (TextView) v.findViewById(R.id.tv_pull_requests_count);
            tvPullRequestsCountView.setText(String.valueOf(result));
        };

        RepositoryService.loadPullRequestCount(
            getActivity(),
            mRepository,
            ApiHelpers.IssueState.OPEN,
            refresh
        ).subscribe(consumer, error -> {});
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImageGetter = new HttpImageGetter(getActivity());
        fillData();
        setContentShown(true);

        loadReadme(false);
        loadPullRequestsCount(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageGetter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageGetter.pause();
    }

    public void setRef(String ref) {
        mRef = ref;
        getArguments().putString("ref", ref);

        // reload readme
        loadReadme(false);

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
        repoName.setSpan(new IntentSpan(tvRepoName.getContext()) {
            @Override
            protected Intent getIntent() {
                return UserActivity.makeIntent(getActivity(), mRepository.getOwner());
            }
        }, 0, mRepository.getOwner().getLogin().length(), 0);
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

        LinearLayout llIssues = (LinearLayout) mContentView.findViewById(R.id.cell_issues);

        if (mRepository.isHasIssues()) {
            llIssues.setVisibility(View.VISIBLE);
            llIssues.setOnClickListener(this);
            // value will be filled when PR count arrives
        } else {
            llIssues.setVisibility(View.GONE);
        }

        mContentView.findViewById(R.id.tv_private).setVisibility(
                mRepository.isPrivate() ? View.VISIBLE : View.GONE);

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
            if (stringId != 0) {
                view.setText(getString(stringId, text));
            } else {
                view.setText(EmojiParser.parseToUnicode(text));
            }
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
            intent = IssueListActivity.makeIntent(getActivity(), owner, name, true);
        } else if (id == R.id.tv_contributors_label) {
            intent = ContributorListActivity.makeIntent(getActivity(), owner, name);
        } else if (id == R.id.tv_collaborators_label) {
            intent = CollaboratorListActivity.makeIntent(getActivity(), owner, name);
        } else if (id == R.id.cell_issues) {
            intent = IssueListActivity.makeIntent(getActivity(), owner, name);
        } else if (id == R.id.cell_stargazers) {
            intent = WatcherListActivity.makeIntent(getActivity(), owner, name);
        } else if (id == R.id.cell_forks) {
            intent = ForkListActivity.makeIntent(getActivity(), owner, name);
        } else if (id == R.id.tv_wiki_label) {
            intent = WikiListActivity.makeIntent(getActivity(), owner, name, null);
        } else if (id == R.id.tv_downloads_label) {
            intent = DownloadsActivity.makeIntent(getActivity(), owner, name);
        } else if (id == R.id.tv_releases_label) {
            intent = ReleaseListActivity.makeIntent(getActivity(), owner, name);
        } else if (view.getTag() instanceof Repository) {
            intent = RepositoryActivity.makeIntent(getActivity(), (Repository) view.getTag());
        }

        if (intent != null) {
            startActivity(intent);
        }
    }
}