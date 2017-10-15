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
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.CollaboratorListActivity;
import com.gh4a.activities.ContributorListActivity;
import com.gh4a.activities.ForkListActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.activities.WatcherListActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.HtmlUtils;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.IntentSpan;
import com.meisolsson.githubsdk.model.Permissions;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService;
import com.meisolsson.githubsdk.service.search.SearchService;
import com.philosophicalhacker.lib.RxLoader;
import com.vdurmont.emoji.EmojiParser;

import java.util.Locale;

import retrofit2.Response;

public class RepositoryFragment extends LoadingFragmentBase implements OnClickListener {
    public static RepositoryFragment newInstance(Repository repository, String ref) {
        RepositoryFragment f = new RepositoryFragment();

        Bundle args = new Bundle();
        args.putParcelable("repo", repository);
        args.putString("ref", ref);
        f.setArguments(args);

        return f;
    }

    private static final int ID_LOADER_README = 0;
    private static final int ID_LOADER_PULL_REQUEST_COUNT = 1;

    private static final String STATE_KEY_IS_README_EXPANDED = "is_readme_expanded";
    private static final String STATE_KEY_IS_README_LOADED = "is_readme_loaded";

    private RxLoader mRxLoader;
    private Repository mRepository;
    private View mContentView;
    private String mRef;
    private HttpImageGetter mImageGetter;
    private TextView mReadmeView;
    private View mLoadingView;
    private TextView mReadmeTitleView;
    private boolean mIsReadmeLoaded = false;
    private boolean mIsReadmeExpanded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = getArguments().getParcelable("repo");
        mRef = getArguments().getString("ref");
        mRxLoader = new RxLoader(getActivity(), getLoaderManager());
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, ViewGroup parent) {
        mContentView = inflater.inflate(R.layout.repository, parent, false);
        mReadmeView = mContentView.findViewById(R.id.readme);
        mLoadingView = mContentView.findViewById(R.id.pb_readme);
        mReadmeTitleView = mContentView.findViewById(R.id.readme_title);
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
        if (mReadmeView != null) {
            mReadmeView.setVisibility(View.GONE);
        }
        if (mLoadingView != null && mIsReadmeExpanded) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
        if (mContentView != null) {
            mContentView.findViewById(R.id.pull_requests_progress).setVisibility(View.VISIBLE);
        }
        if (mImageGetter != null) {
            mImageGetter.clearHtmlCache();
        }
        setContentShown(false);
        loadReadme(true);
        loadPullRequestCount(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImageGetter = new HttpImageGetter(getActivity());
        fillData();
        setContentShown(true);

        if (savedInstanceState != null) {
            mIsReadmeExpanded = savedInstanceState.getBoolean(STATE_KEY_IS_README_EXPANDED, false);
            mIsReadmeLoaded = savedInstanceState.getBoolean(STATE_KEY_IS_README_LOADED, false);
        }
        if (mIsReadmeExpanded || mIsReadmeLoaded) {
            loadReadme(false);
        }
        loadPullRequestCount(false);

        updateReadmeVisibility();
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_KEY_IS_README_EXPANDED, mIsReadmeExpanded);
        outState.putBoolean(STATE_KEY_IS_README_LOADED, mIsReadmeLoaded);
    }

    public void setRef(String ref) {
        mRef = ref;
        getArguments().putString("ref", ref);

        // Reload readme
        loadReadme(true);
        if (mReadmeView != null) {
            mReadmeView.setVisibility(View.GONE);
        }
        if (mLoadingView != null && mIsReadmeExpanded) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
    }

    private void fillData() {
        TextView tvRepoName = mContentView.findViewById(R.id.tv_repo_name);
        SpannableStringBuilder repoName = new SpannableStringBuilder();
        repoName.append(mRepository.owner().login());
        repoName.append("/");
        repoName.append(mRepository.name());
        repoName.setSpan(new IntentSpan(tvRepoName.getContext()) {
            @Override
            protected Intent getIntent() {
                return UserActivity.makeIntent(getActivity(), mRepository.owner());
            }
        }, 0, mRepository.owner().login().length(), 0);
        tvRepoName.setText(repoName);
        tvRepoName.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);

        TextView tvParentRepo = mContentView.findViewById(R.id.tv_parent);
        if (mRepository.isFork() && mRepository.parent() != null) {
            Repository parent = mRepository.parent();
            tvParentRepo.setVisibility(View.VISIBLE);
            tvParentRepo.setText(getString(R.string.forked_from,
                    parent.owner().login() + "/" + parent.name()));
            tvParentRepo.setOnClickListener(this);
            tvParentRepo.setTag(parent);
        } else {
            tvParentRepo.setVisibility(View.GONE);
        }

        fillTextView(R.id.tv_desc, 0, mRepository.description());
        fillTextView(R.id.tv_language,R.string.repo_language, mRepository.language());
        fillTextView(R.id.tv_url, 0, !StringUtils.isBlank(mRepository.homepage())
                ? mRepository.homepage() : mRepository.htmlUrl());

        mContentView.findViewById(R.id.cell_stargazers).setOnClickListener(this);
        mContentView.findViewById(R.id.cell_forks).setOnClickListener(this);
        mContentView.findViewById(R.id.cell_pull_requests).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_contributors_label).setOnClickListener(this);
        mContentView.findViewById(R.id.other_info).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_releases_label).setOnClickListener(this);
        mReadmeTitleView.setOnClickListener(this);

        Permissions permissions = mRepository.permissions();
        updateClickableLabel(R.id.tv_collaborators_label,
                permissions != null && permissions.push());
        updateClickableLabel(R.id.tv_wiki_label, mRepository.hasWiki());

        TextView tvStargazersCount = mContentView.findViewById(R.id.tv_stargazers_count);
        tvStargazersCount.setText(String.valueOf(mRepository.watchersCount()));

        TextView tvForksCount = mContentView.findViewById(R.id.tv_forks_count);
        tvForksCount.setText(String.valueOf(mRepository.forksCount()));

        LinearLayout llIssues = mContentView.findViewById(R.id.cell_issues);

        if (mRepository.hasIssues()) {
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
        TextView view = mContentView.findViewById(id);

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
        mRepository = mRepository.toBuilder()
                .watchersCount(mRepository.watchersCount() + (starring ? 1 : -1))
                .build();

        TextView tvStargazersCount = mContentView.findViewById(R.id.tv_stargazers_count);
        tvStargazersCount.setText(String.valueOf(mRepository.watchersCount()));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.readme_title) {
            toggleReadmeExpanded();
            return;
        }

        String owner = mRepository.owner().login();
        String name = mRepository.name();
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
        } else if (id == R.id.tv_releases_label) {
            intent = ReleaseListActivity.makeIntent(getActivity(), owner, name);
        } else if (view.getTag() instanceof Repository) {
            intent = RepositoryActivity.makeIntent(getActivity(), (Repository) view.getTag());
        }

        if (intent != null) {
            startActivity(intent);
        }
    }
    private void toggleReadmeExpanded() {
        mIsReadmeExpanded = !mIsReadmeExpanded;

        if (mIsReadmeExpanded && !mIsReadmeLoaded) {
            loadReadme(false);
        }

        updateReadmeVisibility();
    }

    private void updateReadmeVisibility() {
        mReadmeView.setVisibility(mIsReadmeExpanded && mIsReadmeLoaded ? View.VISIBLE : View.GONE);
        mLoadingView.setVisibility(
                mIsReadmeExpanded && !mIsReadmeLoaded ? View.VISIBLE : View.GONE);

        int drawableAttr = mIsReadmeExpanded ? R.attr.dropUpArrowIcon : R.attr.dropDownArrowIcon;
        int drawableRes = UiUtils.resolveDrawable(getContext(), drawableAttr);
        mReadmeTitleView.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawableRes, 0);
    }


    private void loadReadme(boolean force) {
        Context context = getActivity();
        Long id = mRepository.id();
        String repoOwner = mRepository.owner().login();
        String repoName = mRepository.name();
        RepositoryContentService service = ServiceFactory.createService(
                RepositoryContentService.class, "application/vnd.github.v3.html", null, null);

        service.getReadmeHtml(repoOwner, repoName, mRef)
                .map(response -> response.code() == 404 ? Response.success((String) null) : response)
                .map(ApiHelpers::throwOnFailure)
                .map(html -> {
                    if (html != null) {
                        html = HtmlUtils.rewriteRelativeUrls(html, repoOwner, repoName, mRef);
                        mImageGetter.encode(context, id, html);
                    }
                    return html;
                })
                .doOnSubscribe(disposable -> mIsReadmeLoaded = false)
                .compose(RxUtils::doInBackground)
                .toObservable()
                .compose(mRxLoader.makeObservableTransformer(ID_LOADER_README, force))
                .subscribe(readme -> {
                    if (readme != null) {
                        mReadmeView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
                        mImageGetter.bind(mReadmeView, readme, id);
                    } else {
                        mReadmeView.setText(R.string.repo_no_readme);
                        mReadmeView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                    }
                    mReadmeView.setVisibility(mIsReadmeExpanded ? View.VISIBLE : View.GONE);
                    mLoadingView.setVisibility(View.GONE);
                    mIsReadmeLoaded = true;

                }, error -> {});
    }

    private void loadPullRequestCount(boolean force) {
        SearchService service = ServiceFactory.createService(SearchService.class, null, null, 1);
        String query = String.format(Locale.US, "type:pr repo:%s/%s state:open",
                mRepository.owner().login(), mRepository.name());

        service.searchIssues(query, null, null, 0)
                .map(ApiHelpers::throwOnFailure)
                .map(page -> page.totalCount())
                .compose(RxUtils::doInBackground)
                .toObservable()
                .compose(mRxLoader.makeObservableTransformer(ID_LOADER_PULL_REQUEST_COUNT, force))
                .subscribe(count -> {
                    View v = getView();
                    v.findViewById(R.id.issues_progress).setVisibility(View.GONE);
                    v.findViewById(R.id.pull_requests_progress).setVisibility(View.GONE);

                    TextView tvIssuesCount = mContentView.findViewById(R.id.tv_issues_count);
                    tvIssuesCount.setText(String.valueOf(mRepository.openIssuesCount() - count));

                    TextView tvPullRequestsCountView = v.findViewById(R.id.tv_pull_requests_count);
                    tvPullRequestsCountView.setText(String.valueOf(count));
                }, error -> {});
    }
}