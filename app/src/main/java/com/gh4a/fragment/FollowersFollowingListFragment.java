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
import com.meisolsson.githubsdk.service.users.UserFollowerService;

import io.reactivex.Single;
import retrofit2.Response;

public class FollowersFollowingListFragment extends PagedDataBaseFragment<User> {
    public static FollowersFollowingListFragment newInstance(String login, boolean showFollowers) {
        FollowersFollowingListFragment f = new FollowersFollowingListFragment();

        Bundle args = new Bundle();
        args.putString("user", login);
        args.putBoolean("show_followers", showFollowers);
        f.setArguments(args);

        return f;
    }

    private String mLogin;
    private boolean mShowFollowers;
    private Boolean mIsFollowing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString("user");
        mShowFollowers = getArguments().getBoolean("show_followers");
        if (mShowFollowers) {
            loadFollowingState(false);
        }
    }

    @Override
    protected RootAdapter<User, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new UserAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return mShowFollowers ? R.string.no_followers_found : R.string.no_following_found;
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
        mIsFollowing = null;
        if (mShowFollowers) {
            loadFollowingState(true);
        }
        super.onRefresh();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mShowFollowers && Gh4Application.get().isAuthorized()) {
            MenuItem followItem = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "")
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
            if (mIsFollowing == null) {
                followItem.setActionView(R.layout.ab_loading);
                followItem.expandActionView();
            } else if (mIsFollowing) {
                followItem.setTitle(R.string.user_follow_action);
            } else {
                followItem.setTitle(R.string.user_unfollow_action);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            toggleFollowingState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Single<Response<Page<User>>> loadPage(int page, boolean bypassCache) {
        final UserFollowerService service = ServiceFactory.get(UserFollowerService.class, bypassCache);
        return mShowFollowers
                ? service.getFollowers(mLogin, page)
                : service.getFollowing(mLogin, page);
    }

    private void loadFollowingState(boolean force) {
        if (!Gh4Application.get().isAuthorized()) {
            return;
        }
        UserFollowerService service = ServiceFactory.get(UserFollowerService.class, force);
        service.isFollowing(mLogin)
                .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(makeLoaderSingle(1, force))
                .subscribe(result -> {
                    mIsFollowing = result;
                    getActivity().invalidateOptionsMenu();
                }, this::handleLoadFailure);
    }

    private void toggleFollowingState() {
        UserFollowerService service = ServiceFactory.get(UserFollowerService.class, false);
        Single<Response<Void>> responseSingle = mIsFollowing
                ? service.unfollowUser(mLogin)
                : service.followUser(mLogin);
        responseSingle.map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils::doInBackground)
                .subscribe(result -> {
                    if (mIsFollowing != null) {
                        mIsFollowing = !mIsFollowing;
                        getActivity().invalidateOptionsMenu();
                    }
                }, error -> {
                    handleActionFailure("Updating user following state failed", error);
                });
    }
}