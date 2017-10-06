package com.gh4a.loader;

import java.io.IOException;
import java.net.HttpURLConnection;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.service.activity.StarringService;

public class IsStarringLoader extends BaseLoader<Boolean> {

    private final String mRepoOwner;
    private final String mRepoName;

    public IsStarringLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public Boolean doLoadInBackground() throws IOException {
        StarringService service = Gh4Application.get().getGitHubService(StarringService.class);
        try {
            ApiHelpers.throwOnFailure(
                    service.checkIfRepositoryIsStarred(mRepoOwner, mRepoName).blockingGet());
            return true;
        } catch (ApiRequestException e) {
            if (e.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
                // 404 means 'not starred'
                return false;
            }
            throw e;
        }
    }
}
