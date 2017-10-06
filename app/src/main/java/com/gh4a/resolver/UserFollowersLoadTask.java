package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.FollowerFollowingListActivity;
import com.gh4a.activities.UserActivity;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.UserType;

public class UserFollowersLoadTask extends UserLoadTask {
    @VisibleForTesting
    protected final boolean mShowFollowers;

    public UserFollowersLoadTask(FragmentActivity activity, String userLogin,
            boolean showFollowers) {
        super(activity, userLogin);
        mShowFollowers = showFollowers;
    }

    @Override
    protected Intent getIntent(User user) {
        if (user.type() == UserType.Organization) {
            return UserActivity.makeIntent(mActivity, user);
        }
        return FollowerFollowingListActivity.makeIntent(mActivity, user.login(), mShowFollowers);
    }
}
