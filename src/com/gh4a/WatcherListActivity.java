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

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.WatcherService;

/**
 * The WatcherList activity.
 */
public class WatcherListActivity extends UserListActivity {

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
        mSubtitle = getResources().getString(R.string.repo_watchers);
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
     * @see com.gh4a.UserListActivity#getUsers()
     */
    protected List<User> getUsers() throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        WatcherService watcherService = new WatcherService(client);
        return watcherService.getWatchers(new RepositoryId(mUserLogin, mRepoName));
    }
}
