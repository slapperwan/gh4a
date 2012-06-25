package com.gh4a.loader;

import java.util.List;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.UserService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.gh4a.Gh4Application;

public class FollowersFollowingListLoader extends AsyncTaskLoader<List<User>> {

    private String mLogin;
    private int mSize;
    private boolean mFindFollowers;
    
    public FollowersFollowingListLoader(Context context, String login, int size,
            boolean findFollowers) {
        super(context);
        this.mLogin = login;
        this.mSize = size;
        this.mFindFollowers = findFollowers;
    }
    
    @Override
    public List<User> loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        UserService userService = new UserService(client);
        List<User> users = null;
        if (mFindFollowers) {
            if (mSize > 0) {
                users = (List<User>) userService.pageFollowers(mLogin, mSize).next();
            }
            else {
                users = (List<User>) userService.pageFollowers(mLogin).next();
            }
        } 
        else {
            if (mSize > 0) {
                users = (List<User>) userService.pageFollowing(mLogin, mSize).next();
            }
            else {
                users = (List<User>) userService.pageFollowing(mLogin).next();
            }
        }
        return users;
    }
}
