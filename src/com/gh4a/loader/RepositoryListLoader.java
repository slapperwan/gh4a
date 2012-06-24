package com.gh4a.loader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class RepositoryListLoader extends AsyncTaskLoader<List<Repository>> {

    private String mLogin;
    private String mType;
    private Map<String, String> mFilterData;
    private int mSize;
    
    public RepositoryListLoader(Context context, String login, String userType, 
            Map<String, String> filterData, int size) {
        super(context);
        this.mLogin = login;
        this.mType = userType;
        this.mFilterData = filterData; 
        this.mSize = size;
    }
    
    @Override
    public List<Repository> loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        RepositoryService repoService = new RepositoryService(client);
        try {
            if (mLogin.equals(app.getAuthLogin())) {
                if (mSize > 0) {
                    return (List<Repository>) repoService.pageRepositories(mFilterData, mSize).next();    
                }
                else {
                    return repoService.getRepositories(mFilterData);
                }
            }
            else if (Constants.User.USER_TYPE_ORG.equals(mType)) {
                if (mSize > 0) {
                    return (List<Repository>) repoService.pageOrgRepositories(mLogin, mFilterData, mSize).next();
                }
                else {
                    return repoService.getOrgRepositories(mLogin, mFilterData);
                }
            }
            else {
                if (mSize > 0) {
                    return (List<Repository>) repoService.pageRepositories(mLogin, mFilterData, mSize).next();
                }
                else {
                    return repoService.getRepositories(mLogin, mFilterData);
                }
            }
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
