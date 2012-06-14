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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.widget.AbsListView;

import com.gh4a.holder.BreadCrumbHolder;

/**
 * The ForkList activity.
 */
public class ForkListActivity extends RepositoryListActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#setRequestData()
     */
    @Override
    protected void setRequestData() {
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#getRepositories()
     */
    @Override
    protected List<Repository> getRepositories() throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        RepositoryService repoService = new RepositoryService(client);
        
        return repoService.getForks(new RepositoryId(mUserLogin, mRepoName));
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#setTitleBar()
     */
    @Override
    protected void setTitleBar() {
        mTitleBar = mUserLogin + " / " + mRepoName;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#setSubtitle()
     */
    @Override
    protected void setSubtitle() {
        mSubtitle = "Network Members";
    }

    /*
     * (non-Javadoc)
     * @seecom.gh4a.RepositoryListActivity#onScrollStateChanged(android.widget.
     * AbsListView, int)
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mReload = false;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#setRowLayout()
     */
    @Override
    protected void setRowLayout() {
        mRowLayout = R.layout.row_simple;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.RepositoryListActivity#setBreadCrumbs()
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

        createBreadcrumb("Network Members", breadCrumbHolders);
    }
}
