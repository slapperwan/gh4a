package com.gh4a.loader;

import java.io.IOException;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.service.repositories.RepositoryCollaboratorService;

public class IsCollaboratorLoader extends BaseLoader<Boolean> {

    private final String mRepoOwner;
    private final String mRepoName;

    public IsCollaboratorLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public Boolean doLoadInBackground() throws IOException {
        Gh4Application app = Gh4Application.get();
        String login = app.getAuthLogin();
        if (login == null) {
            return false;
        }

        RepositoryCollaboratorService service =
                app.getGitHubService(RepositoryCollaboratorService.class);
        try {
            return ApiHelpers.throwOnFailure(
                    service.isUserCollaborator(mRepoOwner, mRepoName, login).blockingGet());
        } catch (ApiRequestException e) {
            if (e.getStatus() == 403) {
                // the API returns 403 if the user doesn't have push access,
                // which in turn means he isn't a collaborator
                return false;
            }
            throw e;
        }
    }
}
