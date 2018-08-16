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
import android.support.v4.app.LoaderManager;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.CollaboratorListActivity;
import com.gh4a.activities.ContributorListActivity;
import com.gh4a.activities.ForkListActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.StargazerListActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.activities.WatcherListActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.HtmlUtils;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.IntentSpan;
import com.meisolsson.githubsdk.model.Permissions;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.SearchPage;
import com.meisolsson.githubsdk.model.Subscription;
import com.meisolsson.githubsdk.model.request.activity.SubscriptionRequest;
import com.meisolsson.githubsdk.service.activity.StarringService;
import com.meisolsson.githubsdk.service.activity.WatchingService;
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService;
import com.meisolsson.githubsdk.service.search.SearchService;
import com.gh4a.widget.OverviewRow;
import com.vdurmont.emoji.EmojiParser;

import java.net.HttpURLConnection;
import java.util.Locale;

import io.reactivex.Single;
import retrofit2.Response;

public class RepositoryFragment extends LoadingFragmentBase implements
        OverviewRow.OnIconClickListener, View.OnClickListener {
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
    private static final int ID_LOADER_WATCHING = 2;
    private static final int ID_LOADER_STARRING = 3;

    private static final String STATE_KEY_IS_README_EXPANDED = "is_readme_expanded";
    private static final String STATE_KEY_IS_README_LOADED = "is_readme_loaded";

    private Repository mRepository;
    private View mContentView;
    private OverviewRow mWatcherRow;
    private OverviewRow mStarsRow;
    private String mRef;
    private HttpImageGetter mImageGetter;
    private TextView mReadmeView;
    private View mLoadingView;
    private TextView mReadmeTitleView;
    private Boolean mIsWatching = null;
    private Boolean mIsStarring = null;
    private boolean mIsReadmeLoaded = false;
    private boolean mIsReadmeExpanded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = getArguments().getParcelable("repo");
        mRef = getArguments().getString("ref");
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
            OverviewRow issuesRow = mContentView.findViewById(R.id.issues_row);
            issuesRow.setText(null);
            OverviewRow pullsRow = mContentView.findViewById(R.id.pulls_row);
            pullsRow.setText(null);
        }
        if (mIsWatching != null && mWatcherRow != null) {
            mWatcherRow.setText(null);
        }
        mIsWatching = null;
        if (mIsStarring != null && mStarsRow != null) {
            mStarsRow.setText(null);
        }
        mIsStarring = null;
        if (mImageGetter != null) {
            mImageGetter.clearHtmlCache();
        }
        if (mIsReadmeLoaded) {
            loadReadme(true);
        }
        loadPullRequestCount(true);
        loadStarringState(true);
        loadWatchingState(true);
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

        LoaderManager lm = getLoaderManager();
        if (mIsReadmeExpanded || mIsReadmeLoaded) {
            loadReadme(false);
        }
        loadPullRequestCount(false);
        loadWatchingState(false);
        loadStarringState(false);

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
        if (mIsReadmeLoaded) {
            loadReadme(true);
        }
        if (mReadmeView != null) {
            mReadmeView.setVisibility(View.GONE);
        }
        if (mLoadingView != null && mIsReadmeExpanded) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
    }

    private void fillData() {
        TextView tvRepoName = mContentView.findViewById(R.id.tv_repo_name);
        IntentSpan repoSpan = new IntentSpan(tvRepoName.getContext(),
                context -> UserActivity.makeIntent(context, mRepository.owner()));
        SpannableStringBuilder repoName = new SpannableStringBuilder();
        repoName.append(mRepository.owner().login());
        repoName.append("/");
        repoName.append(mRepository.name());
        repoName.setSpan(repoSpan, 0, mRepository.owner().login().length(), 0);
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
        fillTextView(R.id.tv_url, 0, !StringUtils.isBlank(mRepository.homepage())
                ? mRepository.homepage() : mRepository.htmlUrl());

        final String owner = mRepository.owner().login();
        final String name = mRepository.name();

        OverviewRow languageRow = mContentView.findViewById(R.id.language_row);
        languageRow.setVisibility(StringUtils.isBlank(mRepository.language())
                ? View.GONE : View.VISIBLE);
        languageRow.setText(getString(R.string.repo_language, mRepository.language()));

        OverviewRow issuesRow = mContentView.findViewById(R.id.issues_row);
        issuesRow.setVisibility(mRepository.hasIssues() ? View.VISIBLE : View.GONE);
        issuesRow.setClickIntent(IssueListActivity.makeIntent(getActivity(), owner, name));

        OverviewRow pullsRow = mContentView.findViewById(R.id.pulls_row);
        pullsRow.setClickIntent(IssueListActivity.makeIntent(getActivity(), owner, name, true));

        OverviewRow forksRow = mContentView.findViewById(R.id.forks_row);
        forksRow.setText(getResources().getQuantityString(R.plurals.fork,
                mRepository.forksCount(), mRepository.forksCount()));
        forksRow.setClickIntent(ForkListActivity.makeIntent(getActivity(), owner, name));

        mStarsRow = mContentView.findViewById(R.id.stars_row);
        mStarsRow.setIconClickListener(this);
        mStarsRow.setClickIntent(StargazerListActivity.makeIntent(getActivity(), owner, name));

        mWatcherRow = mContentView.findViewById(R.id.watchers_row);
        mWatcherRow.setIconClickListener(this);
        mWatcherRow.setClickIntent(WatcherListActivity.makeIntent(getActivity(), owner, name));

        if (!Gh4Application.get().isAuthorized()) {
            updateWatcherUi();
            updateStargazerUi();
        }

        mContentView.findViewById(R.id.tv_contributors_label).setOnClickListener(this);
        mContentView.findViewById(R.id.other_info).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_releases_label).setOnClickListener(this);
        mReadmeTitleView.setOnClickListener(this);

        Permissions permissions = mRepository.permissions();
        updateClickableLabel(R.id.tv_collaborators_label,
                permissions != null && permissions.push());
        updateClickableLabel(R.id.tv_wiki_label, mRepository.hasWiki());

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

    private void updateStargazerUi() {
        mStarsRow.setText(getResources().getQuantityString(R.plurals.star,
                mRepository.stargazersCount(), mRepository.stargazersCount()));
        mStarsRow.setToggleState(mIsStarring != null && mIsStarring);
    }

    private void updateWatcherUi() {
        mWatcherRow.setText(getResources().getQuantityString(R.plurals.watcher,
                mRepository.subscribersCount(), mRepository.subscribersCount()));
        mWatcherRow.setToggleState(mIsWatching != null && mIsWatching);
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

        if (id == R.id.tv_contributors_label) {
            intent = ContributorListActivity.makeIntent(getActivity(), owner, name);
        } else if (id == R.id.tv_collaborators_label) {
            intent = CollaboratorListActivity.makeIntent(getActivity(), owner, name);
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

    @Override
    public void onIconClick(OverviewRow row) {
        if (row == mWatcherRow && mIsWatching != null) {
            mWatcherRow.setText(null);
            toggleWatchingState();
        } else if (row == mStarsRow && mIsStarring != null) {
            mStarsRow.setText(null);
            toggleStarringState();
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
        RepositoryContentService service = ServiceFactory.get(RepositoryContentService.class, force);

        service.getReadmeHtml(repoOwner, repoName, mRef)
                .map(ApiHelpers::throwOnFailure)
                .map(Optional::of)
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, Optional.<String>absent()))
                .map(htmlOpt -> {
                    if (htmlOpt.isPresent()) {
                        String html = HtmlUtils.rewriteRelativeUrls(htmlOpt.get(),
                                repoOwner, repoName, mRef != null ? mRef : mRepository.defaultBranch());
                        mImageGetter.encode(context, id, html);
                        return Optional.of(html);
                    }
                    return Optional.<String>absent();
                })
                .compose(makeLoaderSingle(ID_LOADER_README, force))
                .doOnSubscribe(disposable -> {
                    mIsReadmeLoaded = false;
                    updateReadmeVisibility();
                })
                .subscribe(readmeOpt -> {
                    if (readmeOpt.isPresent()) {
                        mReadmeView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
                        mImageGetter.bind(mReadmeView, readmeOpt.get(), id);
                    } else {
                        mReadmeView.setText(R.string.repo_no_readme);
                        mReadmeView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                    }
                    mIsReadmeLoaded = true;
                    updateReadmeVisibility();
                }, this::handleLoadFailure);
    }

    private void loadPullRequestCount(boolean force) {
        SearchService service = ServiceFactory.get(SearchService.class, force, null, null, 1);
        String query = String.format(Locale.US, "type:pr repo:%s/%s state:open",
                mRepository.owner().login(), mRepository.name());

        service.searchIssues(query, null, null, 0)
                .map(ApiHelpers::throwOnFailure)
                .map(SearchPage::totalCount)
                .compose(makeLoaderSingle(ID_LOADER_PULL_REQUEST_COUNT, force))
                .subscribe(count -> {
                    int issueCount = mRepository.openIssuesCount() - count;

                    OverviewRow issuesRow = mContentView.findViewById(R.id.issues_row);
                    issuesRow.setText(getResources().getQuantityString(R.plurals.issue, issueCount, issueCount));

                    OverviewRow pullsRow = mContentView.findViewById(R.id.pulls_row);
                    pullsRow.setText(getResources().getQuantityString(R.plurals.pull_request, count, count));
                }, this::handleLoadFailure);
    }

    private void toggleStarringState() {
        StarringService service = ServiceFactory.get(StarringService.class, false);
        Single<Response<Void>> responseSingle = mIsStarring
                ? service.unstarRepository(mRepository.owner().login(), mRepository.name())
                : service.starRepository(mRepository.owner().login(), mRepository.name());
        responseSingle.map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils::doInBackground)
                .subscribe(result -> {
                    if (mIsStarring != null) {
                        mIsStarring = !mIsStarring;
                        mRepository = mRepository.toBuilder()
                                .stargazersCount(mRepository.stargazersCount() + (mIsStarring ? 1 : -1))
                                .build();
                        updateStargazerUi();
                    }
                }, error -> {
                    handleActionFailure("Updating repo starring state failed", error);
                    updateStargazerUi();
                });

    }

    private void toggleWatchingState() {
        WatchingService service = ServiceFactory.get(WatchingService.class, false);
        final String repoOwner = mRepository.owner().login(), repoName = mRepository.name();
        final Single<?> responseSingle;

        if (mIsWatching) {
            responseSingle = service.deleteRepositorySubscription(repoOwner, repoName)
                    .map(ApiHelpers::throwOnFailure);
        } else {
            SubscriptionRequest request = SubscriptionRequest.builder()
                    .subscribed(true)
                    .build();
            responseSingle = service.setRepositorySubscription(repoOwner, repoName, request)
                    .map(ApiHelpers::throwOnFailure);
        }

        responseSingle.compose(RxUtils::doInBackground)
                .subscribe(result -> {
                    if (mIsWatching != null) {
                        mIsWatching = !mIsWatching;
                        mRepository = mRepository.toBuilder()
                                .subscribersCount(mRepository.subscribersCount() + (mIsWatching ? 1 : -1))
                                .build();
                        updateWatcherUi();
                    }
                }, error -> {
                    handleActionFailure("Updating repo watching state failed", error);
                    updateWatcherUi();
                });
    }


    private void loadStarringState(boolean force) {
        if (!Gh4Application.get().isAuthorized()) {
            return;
        }
        StarringService service = ServiceFactory.get(StarringService.class, force);
        service.checkIfRepositoryIsStarred(mRepository.owner().login(), mRepository.name())
                .map(ApiHelpers::throwOnFailure)
                // success response means 'starred'
                .map(result -> true)
                // 404 means 'not starred'
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, false))
                .compose(makeLoaderSingle(ID_LOADER_STARRING, force))
                .subscribe(result -> {
                    mIsStarring = result;
                    updateStargazerUi();
                }, this::handleLoadFailure);
    }

    private void loadWatchingState(boolean force) {
        if (!Gh4Application.get().isAuthorized()) {
            return;
        }
        WatchingService service = ServiceFactory.get(WatchingService.class, force);
        service.getRepositorySubscription(mRepository.owner().login(), mRepository.name())
                .map(ApiHelpers::throwOnFailure)
                .map(Subscription::subscribed)
                // 404 means 'not subscribed'
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, false))
                .compose(makeLoaderSingle(ID_LOADER_WATCHING, force))
                .subscribe(result -> {
                    mIsWatching = result;
                    updateWatcherUi();
                }, this::handleLoadFailure);
    }


}