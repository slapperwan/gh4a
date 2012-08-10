package com.gh4a.loader;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.UserService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.Gh4Application;

public class FollowUserLoader extends BaseLoader {

    private String mLogin;
    private Boolean mIsFollowing;
    
    public FollowUserLoader(Context context, String login, boolean isFollowing) {
        super(context);
        mLogin = login;
        mIsFollowing = isFollowing;
    }
    
    @Override
    public void doLoadInBackground(HashMap<Integer, Object> result) throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        UserService userService = new UserService(client);
        if (mIsFollowing) {
            userService.unfollow(mLogin);
            result.put(LoaderResult.DATA, false);
        }
        else {
            userService.follow(mLogin);
            result.put(LoaderResult.DATA, true);
        }
    }
}
