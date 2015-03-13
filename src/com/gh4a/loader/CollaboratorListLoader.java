package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CollaboratorService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class CollaboratorListLoader extends BaseLoader<List<User>> {

    private String mRepoOwner;
    private String mRepoName;

    public CollaboratorListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<User> doLoadInBackground() throws IOException {
        CollaboratorService collabService = (CollaboratorService)
                Gh4Application.get().getService(Gh4Application.COLLAB_SERVICE);
        return collabService.getCollaborators(new RepositoryId(mRepoOwner, mRepoName));
    }
}
