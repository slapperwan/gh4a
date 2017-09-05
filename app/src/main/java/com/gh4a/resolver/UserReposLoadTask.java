package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.RepositoryListActivity;
import com.gh4a.fragment.RepositoryListContainerFragment;
import com.gh4a.utils.ApiHelpers;

import org.eclipse.egit.github.core.User;

public class UserReposLoadTask extends UserLoadTask {
    @VisibleForTesting
    protected boolean mShowStars;

    public UserReposLoadTask(FragmentActivity activity, String userLogin, boolean showStars) {
        super(activity, userLogin);
        mShowStars = showStars;
    }

    @Override
    protected Intent getIntent(User user) {
        boolean isOrg = ApiHelpers.UserType.ORG.equals(user.getType());
        String filter = mShowStars && !isOrg
                ? RepositoryListContainerFragment.FILTER_TYPE_STARRED
                : null;
        return RepositoryListActivity.makeIntent(mActivity, user.getLogin(), isOrg,
                filter);
    }
}
