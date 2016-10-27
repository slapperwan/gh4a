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

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.UserService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.UserAdapter;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString("user");
        mShowFollowers = getArguments().getBoolean("show_followers");
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
    protected PageIterator<User> onCreateIterator() {
        UserService userService = (UserService)
                Gh4Application.get().getService(Gh4Application.USER_SERVICE);
        return mShowFollowers ? userService.pageFollowers(mLogin) : userService.pageFollowing(mLogin);
    }
}