package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.users.UserService;

public class UserLoader extends BaseLoader<User> {
    private final String mLogin;

    public UserLoader(Context context, String login) {
        super(context);
        mLogin = login;
    }

    @Override
    public User doLoadInBackground() throws ApiRequestException {
        return loadUser(mLogin);
    }

    public static User loadUser(String login) throws ApiRequestException {
        UserService userService = Gh4Application.get().getGitHubService(UserService.class);
        return ApiHelpers.throwOnFailure(userService.getUser(login).blockingGet());
    }
}
