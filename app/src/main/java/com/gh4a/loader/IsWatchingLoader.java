package com.gh4a.loader;

import java.io.IOException;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Subscription;
import com.meisolsson.githubsdk.service.activity.WatchingService;

public class IsWatchingLoader extends BaseLoader<Boolean> {

    private final String mRepoOwner;
    private final String mRepoName;

    public IsWatchingLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public Boolean doLoadInBackground() throws IOException {
        WatchingService service = Gh4Application.get().getGitHubService(WatchingService.class);
        Subscription subscription = ApiHelpers.throwOnFailure(
                service.getRepositorySubscription(mRepoOwner, mRepoName).blockingGet());
        return subscription.subscribed();
    }
}
