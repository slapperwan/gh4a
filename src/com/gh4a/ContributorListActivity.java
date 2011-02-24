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

import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.User;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.RepositoryService;

/**
 * The ContributorList activity.
 */
public class ContributorListActivity extends UserListActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#setRequestData()
     */
    @Override
    protected void setRequestData() {
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mShowMoreData = false;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#setTitleBar()
     */
    protected void setTitleBar() {
        mTitleBar = mUserLogin + " / " + mRepoName;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#setSubtitle()
     */
    protected void setSubtitle() {
        mSubtitle = getResources().getString(R.string.repo_contributors);
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#setRowLayout()
     */
    @Override
    protected void setRowLayout() {
        mRowLayout = R.layout.row_gravatar_1;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#setBreadCrumbs()
     */
    protected void setBreadCrumbs() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[2];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);
        data.put(Constants.Repository.REPO_NAME, mRepoName);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        // Repo
        b = new BreadCrumbHolder();
        b.setLabel(mRepoName);
        b.setTag(Constants.Repository.REPO_NAME);
        b.setData(data);
        breadCrumbHolders[1] = b;

        createBreadcrumb(mSubtitle, breadCrumbHolders);
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#getUsers()
     */
    protected List<User> getUsers() throws GitHubException {
        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
        RepositoryService repositoryService = factory.createRepositoryService();
        return repositoryService.getContributors(mUserLogin, mRepoName);
    }
}
