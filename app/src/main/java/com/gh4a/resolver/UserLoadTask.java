package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.users.UserService;

public abstract class UserLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mUserLogin;

    public UserLoadTask(FragmentActivity activity, String userLogin) {
        super(activity);
        this.mUserLogin = userLogin;
    }

    @Override
    protected Intent run() throws ApiRequestException {
        UserService userService = Gh4Application.get().getGitHubService(UserService.class);
        User user = ApiHelpers.throwOnFailure(userService.getUser(mUserLogin).blockingGet());
        if (user == null) {
            return null;
        }
        return getIntent(user);
    }

    protected abstract Intent getIntent(User user);
}
