package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class BranchListLoader extends BaseLoader<List<RepositoryBranch>> {

    private String mRepoOwner;
    private String mRepoName;
    
    public BranchListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }
    
    @Override
    public List<RepositoryBranch> doLoadInBackground() throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get(getContext()).getService(Gh4Application.REPO_SERVICE);
        return repoService.getBranches(new RepositoryId(mRepoOwner, mRepoName));
    }
}
