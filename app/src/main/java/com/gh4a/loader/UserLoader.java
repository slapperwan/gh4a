package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.UserService;

import java.io.IOException;

public class UserLoader extends BaseLoader<User> {

    private final String mLogin;

    public UserLoader(Context context, String login) {
        super(context);
        this.mLogin = login;
    }

    @Override
    public User doLoadInBackground() throws IOException {
        return loadUser(mLogin);
    }

    public static User loadUser(String login) throws IOException {
        UserService userService = (UserService)
                Gh4Application.get().getService(Gh4Application.USER_SERVICE);
        return userService.getUser(login);
    }
}
