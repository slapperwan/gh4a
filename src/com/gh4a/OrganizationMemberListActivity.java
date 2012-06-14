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

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.OrganizationService;

import com.gh4a.holder.BreadCrumbHolder;

public class OrganizationMemberListActivity extends UserListActivity {

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
        mShowMoreData = true;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#setTitleBar()
     */
    protected void setTitleBar() {
        mTitleBar = mUserLogin;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#setSubtitle()
     */
    protected void setSubtitle() {
        mSubtitle = "Members";
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#setRowLayout()
     */
    @Override
    protected void setRowLayout() {
        mRowLayout = R.layout.row_gravatar_2;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#setBreadCrumbs()
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

    /*
     * (non-Javadoc)
     * @see com.gh4a.UserListActivity#getUsers()
     */
    protected List<User> getUsers() throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        OrganizationService orgService = new OrganizationService(client);
        return orgService.getPublicMembers(mUserLogin);
    }
}
