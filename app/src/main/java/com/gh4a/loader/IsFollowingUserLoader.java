package com.gh4a.loader;

import java.net.HttpURLConnection;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.service.users.UserFollowerService;

public class IsFollowingUserLoader extends BaseLoader<Boolean> {

    private final String mLogin;

    public IsFollowingUserLoader(Context context, String login) {
        super(context);
        this.mLogin = login;
    }

    @Override
    public Boolean doLoadInBackground() throws ApiRequestException {
        UserFollowerService service = Gh4Application.get().getGitHubService(UserFollowerService.class);
        try {
            ApiHelpers.throwOnFailure(service.isFollowing(mLogin).blockingGet());
            return true;
        } catch (ApiRequestException e) {
            if (e.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
                // 404 means 'not following'
                return false;
            }
            throw e;
        }
    }
}
