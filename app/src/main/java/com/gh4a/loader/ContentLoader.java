package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Content;
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService;

public class ContentLoader extends BaseLoader<Content> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mPath;
    private final String mRef;

    public ContentLoader(Context context, String repoOwner, String repoName, String path, String ref) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPath = path;
        mRef = ref;
    }

    @Override
    public Content doLoadInBackground() throws ApiRequestException {
        final RepositoryContentService service =
                Gh4Application.get().getGitHubService(RepositoryContentService.class);
        return ApiHelpers.throwOnFailure(
                service.getContents(mRepoOwner, mRepoName, mPath, mRef).blockingGet());
    }
}
