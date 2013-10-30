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

import android.os.Bundle;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.UserAdapter;

public class FollowersFollowingListFragment extends PagedDataBaseFragment<User> {
    private String mLogin;
    private boolean mFindFollowers;
    
    public static FollowersFollowingListFragment newInstance(String login, boolean mFindFollowers) {
        FollowersFollowingListFragment f = new FollowersFollowingListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putBoolean("FIND_FOLLOWER", mFindFollowers);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mFindFollowers = getArguments().getBoolean("FIND_FOLLOWER");
    }
    
    @Override
    protected RootAdapter<User> onCreateAdapter() {
        return new UserAdapter(getActivity(), false);
    }
    
    @Override
    protected int getEmptyTextResId() {
        return R.string.no_followers_found;
    }

    @Override
    protected void onItemClick(User user) {
        Gh4Application app = Gh4Application.get(getActivity());
        app.openUserInfoActivity(getActivity(), user.getLogin(), user.getName());
    }

    @Override
    protected PageIterator<User> onCreateIterator() {
        UserService userService = (UserService)
                Gh4Application.get(getActivity()).getService(Gh4Application.USER_SERVICE);
        return mFindFollowers ? userService.pageFollowers(mLogin) : userService.pageFollowing(mLogin);
    }
}