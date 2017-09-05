package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.FollowerFollowingListActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;

import org.eclipse.egit.github.core.User;

public class UserFollowersLoadTask extends UserLoadTask {
    @VisibleForTesting
    protected boolean mShowFollowers;

    public UserFollowersLoadTask(FragmentActivity activity, String userLogin,
            boolean showFollowers) {
        super(activity, userLogin);
        mShowFollowers = showFollowers;
    }

    @Override
    protected Intent getIntent(User user) {
        if (ApiHelpers.UserType.ORG.equals(user.getType())) {
            return UserActivity.makeIntent(mActivity, user);
        }
        return FollowerFollowingListActivity.makeIntent(mActivity, user.getLogin(),
                mShowFollowers);
    }
}
