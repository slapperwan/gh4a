package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.client.GitHubClient;
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
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        UserService userService = new UserService(client);
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
