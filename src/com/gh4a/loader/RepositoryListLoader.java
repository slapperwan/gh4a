package com.gh4a.loader;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class RepositoryListLoader extends BaseLoader<Collection<Repository>> {
    private String mLogin;
    private Map<String, String> mFilterData;
    private int mSize;
    private String mUserType;

    public RepositoryListLoader(Context context, String login, String userType,
            Map<String, String> filterData, int size) {
        super(context);
        this.mLogin = login;
        this.mFilterData = filterData;
        this.mSize = size;
        this.mUserType = userType;
    }

    @Override
    public Collection<Repository> doLoadInBackground() throws IOException {
        Gh4Application app = Gh4Application.get(getContext());
        RepositoryService repoService = (RepositoryService) app.getService(Gh4Application.REPO_SERVICE);
        if (mLogin.equals(app.getAuthLogin())) {
            if (mSize > 0) {
                return repoService.pageRepositories(mFilterData, mSize).next();
            } else {
                return repoService.getRepositories(mFilterData);
            }
        } else if (Constants.User.TYPE_ORG.equals(mUserType)) {
            if (mSize > 0) {
                return (List<Repository>) repoService.pageOrgRepositories(mLogin, mFilterData, mSize).next();
            } else {
                return repoService.getOrgRepositories(mLogin, mFilterData);
            }
        } else {
            if (mSize > 0) {
                return repoService.pageRepositories(mLogin, mFilterData, mSize).next();
            } else {
                return repoService.getRepositories(mLogin, mFilterData);
            }
        }
    }
}
