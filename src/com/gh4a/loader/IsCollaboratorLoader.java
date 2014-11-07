package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CollaboratorService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class IsCollaboratorLoader extends BaseLoader<Boolean> {

    private String mRepoOwner;
    private String mRepoName;

    public IsCollaboratorLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public Boolean doLoadInBackground() throws IOException {
        Gh4Application app = Gh4Application.get(getContext());
        CollaboratorService collabService =
                (CollaboratorService) app.getService(Gh4Application.COLLAB_SERVICE);
        try {
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            return collabService.isCollaborator(repoId, app.getAuthLogin());
        } catch (RequestException e) {
            if (e.getStatus() == 403) {
                // the API returns 403 if the user doesn't have push access,
                // which in turn means he isn't a collaborator
                return false;
            }
            throw e;
        }
    }
}
