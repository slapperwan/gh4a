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
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.UserAdapter;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.users.UserFollowerService;

import io.reactivex.Single;
import retrofit2.Response;

public class FollowersFollowingListFragment extends PagedDataBaseFragment<User> {
    private static final String EXTRA_USER = "user";
    private static final String EXTRA_SHOW_FOLLOWERS = "show_followers";

    public static FollowersFollowingListFragment newInstance(String login, boolean showFollowers) {
        FollowersFollowingListFragment f = new FollowersFollowingListFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_USER, login);
        args.putBoolean(EXTRA_SHOW_FOLLOWERS, showFollowers);
        f.setArguments(args);

        return f;
    }

    private String mLogin;
    private boolean mShowFollowers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(EXTRA_USER);
        mShowFollowers = getArguments().getBoolean(EXTRA_SHOW_FOLLOWERS);
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
    protected Single<Response<Page<User>>> loadPage(int page, boolean bypassCache) {
        final UserFollowerService service = ServiceFactory.get(UserFollowerService.class, bypassCache);
        return mShowFollowers
                ? service.getFollowers(mLogin, page)
                : service.getFollowing(mLogin, page);
    }
}