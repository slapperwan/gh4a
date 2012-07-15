package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.UserService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class FollowUserLoader extends AsyncTaskLoader<Boolean> {

    private String mLogin;
    private Boolean mIsFollowing;
    
    public FollowUserLoader(Context context, String login, boolean isFollowing) {
        super(context);
        mLogin = login;
        mIsFollowing = isFollowing;
    }
    
    @Override
    public Boolean loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        UserService userService = new UserService(client);
        try {
            if (mIsFollowing) {
                userService.unfollow(mLogin);
                return false;
            }
            else {
                userService.follow(mLogin);
                return true;
            }
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
