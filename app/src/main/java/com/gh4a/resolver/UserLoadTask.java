package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.Optional;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.users.UserService;

import io.reactivex.Single;

public abstract class UserLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mUserLogin;

    public UserLoadTask(FragmentActivity activity, String userLogin) {
        super(activity);
        this.mUserLogin = userLogin;
    }

    @Override
    protected Single<Optional<Intent>> getSingle() {
        UserService userService = ServiceFactory.get(UserService.class, false);
        return userService.getUser(mUserLogin)
                .map(ApiHelpers::throwOnFailure)
                .map(user -> Optional.of(getIntent(user)));
    }

    protected abstract Intent getIntent(User user);
}
