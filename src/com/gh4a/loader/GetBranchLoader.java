package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class GetBranchLoader extends AsyncTaskLoader<RepositoryBranch> {

    private String mRepoOwner;
    private String mRepoName;
    private String mBranchName;
    
    public GetBranchLoader(Context context, String repoOwner, String repoName, String branchName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mBranchName = branchName;
    }
    
    @Override
    public RepositoryBranch loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        
        RepositoryService repoService = new RepositoryService(client);
        try {
            RepositoryBranch branch = null;
            if (mBranchName == null) {//get default branch
                Repository repo = repoService.getRepository(mRepoOwner, mRepoName);
                String masterBranch = repo.getMasterBranch();
                List<RepositoryBranch> branches = repoService.getBranches(new RepositoryId(mRepoOwner, mRepoName));
                for (RepositoryBranch repositoryBranch : branches) {
                    if (repositoryBranch.getName().equals(masterBranch)) {
                        branch = repositoryBranch;
                        break;
                    }
                }
            }
            else {
                List<RepositoryBranch> branches = repoService.getBranches(new RepositoryId(mRepoOwner, mRepoName));
                for (RepositoryBranch repositoryBranch : branches) {
                    if (repositoryBranch.getName().equals(mBranchName)) {
                        branch = repositoryBranch;
                        break;
                    }
                }
            }
            
            return branch;
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }
}
