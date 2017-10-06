package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.loader.UserLoader;
import com.meisolsson.githubsdk.model.User;

public abstract class UserLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mUserLogin;

    public UserLoadTask(FragmentActivity activity, String userLogin) {
        super(activity);
        this.mUserLogin = userLogin;
    }

    @Override
    protected Intent run() throws Exception {
        User user = UserLoader.loadUser(mUserLogin);
        if (user == null) {
            return null;
        }
        return getIntent(user);
    }

    protected abstract Intent getIntent(User user);
}
