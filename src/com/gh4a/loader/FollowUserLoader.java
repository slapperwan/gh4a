package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.service.UserService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class FollowUserLoader extends BaseLoader<Boolean> {

    private String mLogin;
    private Boolean mIsFollowing;
    
    public FollowUserLoader(Context context, String login, Boolean isFollowing) {
        super(context);
        mLogin = login;
        mIsFollowing = isFollowing;
    }
    
    @Override
    public Boolean doLoadInBackground() throws IOException {
        UserService userService = (UserService)
                getContext().getApplicationContext().getSystemService(Gh4Application.GHUSER_SERVICE);
        if (mIsFollowing != null && mIsFollowing) {
            userService.unfollow(mLogin);
            return false;
        }
        else {
            userService.follow(mLogin);
            return true;
        }
    }
}
