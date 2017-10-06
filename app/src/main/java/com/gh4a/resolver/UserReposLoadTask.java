package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.RepositoryListActivity;
import com.gh4a.fragment.RepositoryListContainerFragment;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.UserType;

public class UserReposLoadTask extends UserLoadTask {
    @VisibleForTesting
    protected final boolean mShowStars;

    public UserReposLoadTask(FragmentActivity activity, String userLogin, boolean showStars) {
        super(activity, userLogin);
        mShowStars = showStars;
    }

    @Override
    protected Intent getIntent(User user) {
        boolean isOrg = user.type() == UserType.Organization;
        String filter = mShowStars && !isOrg
                ? RepositoryListContainerFragment.FILTER_TYPE_STARRED
                : null;
        return RepositoryListActivity.makeIntent(mActivity, user.login(), isOrg, filter);
    }
}
