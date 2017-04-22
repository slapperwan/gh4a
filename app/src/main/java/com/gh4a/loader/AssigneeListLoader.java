package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class AssigneeListLoader extends BaseLoader<List<User>> {
    private final String mRepoOwner;
    private final String mRepoName;

    public AssigneeListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<User> doLoadInBackground() throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        return repoService.getAssignees(new RepositoryId(mRepoOwner, mRepoName));
    }
}
