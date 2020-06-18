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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Subscription;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.request.activity.SubscriptionRequest;
import com.meisolsson.githubsdk.service.activity.WatchingService;

import java.net.HttpURLConnection;

import io.reactivex.Single;
import retrofit2.Response;

public class WatcherListFragment extends PagedDataBaseFragment<User> {
    public static WatcherListFragment newInstance(String repoOwner, String repoName) {
        WatcherListFragment f = new WatcherListFragment();

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        f.setArguments(args);

        return f;
    }

    private String mRepoOwner;
    private String mRepoName;
    private Boolean mIsWatching;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
        loadWatchingState(false);
    }

    @Override
    protected RootAdapter<User, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new UserAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_watchers_found;
    }

    @Override
    public void onItemClick(User user) {
        Intent intent = UserActivity.makeIntent(getActivity(), user);
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onRefresh() {
        mIsWatching = null;
        loadWatchingState(true);
        super.onRefresh();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (Gh4Application.get().isAuthorized()) {
            MenuItem starItem = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "")
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
            if (mIsWatching == null) {
                starItem.setActionView(R.layout.ab_loading);
                starItem.expandActionView();
            } else if (mIsWatching) {
                starItem.setTitle(R.string.repo_unwatch_action);
            } else {
                starItem.setTitle(R.string.repo_watch_action);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            toggleWatchingState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadWatchingState(boolean force) {
        if (!Gh4Application.get().isAuthorized()) {
            return;
        }
        WatchingService service = ServiceFactory.get(WatchingService.class, force);
        service.getRepositorySubscription(mRepoOwner, mRepoName)
                .map(ApiHelpers::throwOnFailure)
                .map(Subscription::subscribed)
                // 404 means 'not subscribed'
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, false))
                .compose(makeLoaderSingle(1, force))
                .subscribe(result -> {
                    mIsWatching = result;
                    getActivity().invalidateOptionsMenu();
                }, this::handleLoadFailure);
    }

    private void toggleWatchingState() {
        WatchingService service = ServiceFactory.get(WatchingService.class, false);
        final Single<Boolean> responseSingle;

        if (mIsWatching) {
            responseSingle = service.deleteRepositorySubscription(mRepoOwner, mRepoName)
                    .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                    .map(result -> false);
        } else {
            SubscriptionRequest request = SubscriptionRequest.builder()
                    .subscribed(true)
                    .build();
            responseSingle = service.setRepositorySubscription(mRepoOwner, mRepoName, request)
                    .map(ApiHelpers::throwOnFailure)
                    .map(sub -> sub.subscribed());
        }

        responseSingle.compose(RxUtils::doInBackground)
                .subscribe(result -> {
                    if (mIsWatching != null) {
                        mIsWatching = result;
                    }
                    getActivity().invalidateOptionsMenu();
                }, error -> {
                    handleActionFailure("Updating repo watching state failed", error);
                });
    }

    @Override
    protected Single<Response<Page<User>>> loadPage(int page, boolean bypassCache) {
        final WatchingService service = ServiceFactory.get(WatchingService.class, bypassCache);
        return service.getRepositoryWatchers(mRepoOwner, mRepoName, page);
    }
}