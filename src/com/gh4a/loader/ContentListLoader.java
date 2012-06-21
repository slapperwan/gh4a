package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.TreeEntry;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class ContentListLoader extends AsyncTaskLoader<List<TreeEntry>> {

    private String mRepoOwner;
    private String mRepoName;
    
    public ContentListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        this.mRepoOwner = repoOwner;
        this.mRepoName = repoName;
    }
    
    @Override
    public List<TreeEntry> loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        
        RepositoryService repoService = new RepositoryService(client);
        DataService dataService = new DataService(client);
        
        try {
            Repository repo = repoService.getRepository(mRepoOwner, mRepoName);
            String masterBranch = repo.getMasterBranch();
            
            String sha = null;
            List<RepositoryBranch> branches = repoService.getBranches(new RepositoryId(mRepoOwner, mRepoName));
            for (RepositoryBranch repositoryBranch : branches) {
                if (repositoryBranch.getName().equals(masterBranch)) {
                    sha = repositoryBranch.getCommit().getSha();
                }
            }
            return dataService.getTree(new RepositoryId(mRepoOwner, mRepoName), sha).getTree();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
