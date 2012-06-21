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
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.WatcherService;

import android.os.Bundle;
import android.widget.AbsListView;

import com.gh4a.utils.StringUtils;

/**
 * The WatchedRepoList activity.
 */
public class WatchedRepoListActivity extends RepositoryListActivity {

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
    protected List<Repository> getRepositories() throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        WatcherService watcherService = new WatcherService(client);
        return watcherService.getWatched(mUserLogin);
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
        mSubtitle = "Watched Repositories";
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

}
