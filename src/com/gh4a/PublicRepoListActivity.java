/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.widget.AbsListView;

import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.Repository;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.RepositoryService;
import com.github.api.v2.services.auth.Authentication;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;

/**
 * The PublicRepoList activity.
 */
public class PublicRepoListActivity extends RepositoryListActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The user name. */
    protected String mUserName;

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#setRequestData()
     */
    @Override
    protected void setRequestData() {
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.Repository.REPO_OWNER);
        mUserName = data.getString(Constants.User.USER_NAME);
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#getRepositories()
     */
    @Override
    protected List<Repository> getRepositories() throws GitHubException {
        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
        RepositoryService repositoryService = factory.createRepositoryService();
        Authentication auth = new LoginPasswordAuthentication(getAuthUsername(), getAuthPassword());
        repositoryService.setAuthentication(auth);
        Log.v(Constants.LOG_TAG, "++++++++++ " + mPage);
        return repositoryService.getRepositories(mUserLogin, mPage);
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#setTitleBar()
     */
    @Override
    protected void setTitleBar() {
        mTitleBar = mUserLogin + (!StringUtils.isBlank(mUserName) ? " - " + mUserName : "");
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#setSubtitle()
     */
    protected void setSubtitle() {
        mSubtitle = getResources().getString(R.string.user_pub_repos);
    }

    /*
     * (non-Javadoc)
     * @seecom.gh4a.RepositoryListActivity#onScrollStateChanged(android.widget.
     * AbsListView, int)
     */
//    @Override
//    public void onScrollStateChanged(AbsListView view, int scrollState) {
//        mReload = false;
//    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#setBreadCrumbs()
     */
    protected void setBreadCrumbs() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[1];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        createBreadcrumb(mSubtitle, breadCrumbHolders);
    }
}
