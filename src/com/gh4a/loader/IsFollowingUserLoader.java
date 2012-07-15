package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.UserService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class IsFollowingUserLoader extends AsyncTaskLoader<Boolean> {

    private String mLogin;
    
    public IsFollowingUserLoader(Context context, String login) {
        super(context);
        this.mLogin = login;
    }
    
    @Override
    public Boolean loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        UserService userService = new UserService(client);
        try {
            return userService.isFollowing(mLogin);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
