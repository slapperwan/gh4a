package com.gh4a.resolver;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.users.UserService;

import java.util.Optional;

import io.reactivex.Single;

public abstract class UserLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mUserLogin;

    public UserLoadTask(FragmentActivity activity, Uri urlToResolve, String userLogin) {
        super(activity, urlToResolve);
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
