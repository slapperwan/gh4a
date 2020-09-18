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
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.activity.StarringService;

import io.reactivex.Single;
import retrofit2.Response;

public class StargazerListFragment extends PagedDataBaseFragment<User> {
    public static StargazerListFragment newInstance(String repoOwner, String repoName) {
        StargazerListFragment f = new StargazerListFragment();

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        f.setArguments(args);

        return f;
    }

    private String mRepoOwner;
    private String mRepoName;
    private Boolean mIsStarring;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
        loadStarringState(false);
    }

    @Override
    protected RootAdapter<User, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new UserAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_stargazers_found;
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
        mIsStarring = null;
        loadStarringState(true);
        super.onRefresh();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (Gh4Application.get().isAuthorized()) {
            MenuItem starItem = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "")
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
            if (mIsStarring == null) {
                starItem.setActionView(R.layout.ab_loading);
                starItem.expandActionView();
            } else if (mIsStarring) {
                starItem.setTitle(R.string.repo_unstar_action);
            } else {
                starItem.setTitle(R.string.repo_star_action);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            toggleStarringState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Single<Response<Page<User>>> loadPage(int page, boolean bypassCache) {
        final StarringService service = ServiceFactory.get(StarringService.class, bypassCache);
        return service.getStargazers(mRepoOwner, mRepoName, page);
    }

    private void loadStarringState(boolean force) {
        if (!Gh4Application.get().isAuthorized()) {
            return;
        }
        StarringService service = ServiceFactory.get(StarringService.class, force);
        service.checkIfRepositoryIsStarred(mRepoOwner, mRepoName)
                .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(makeLoaderSingle(1, force))
                .subscribe(result -> {
                    mIsStarring = result;
                    getActivity().invalidateOptionsMenu();
                }, this::handleLoadFailure);
    }

    private void toggleStarringState() {
        StarringService service = ServiceFactory.get(StarringService.class, false);
        Single<Response<Void>> responseSingle = mIsStarring
                ? service.unstarRepository(mRepoOwner, mRepoName)
                : service.starRepository(mRepoOwner, mRepoName);
        responseSingle.map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils::doInBackground)
                .subscribe(result -> {
                    if (mIsStarring != null) {
                        mIsStarring = !mIsStarring;
                        getActivity().invalidateOptionsMenu();
                    }
                }, error -> {
                    handleActionFailure("Updating repo starring state failed", error);
                });
    }
}