package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.UserService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class UserLoader extends BaseLoader<User> {

    private String mLogin;

    public UserLoader(Context context, String login) {
        super(context);
        this.mLogin = login;
    }

    @Override
    public User doLoadInBackground() throws IOException {
        UserService userService = (UserService)
                Gh4Application.get().getService(Gh4Application.USER_SERVICE);
        return userService.getUser(mLogin);
    }
}
