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
package com.gh4a.activities;

import java.util.List;

import org.eclipse.egit.github.core.User;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.R;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.LoaderResult;

public class CollaboratorListActivity extends UserListActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName) {
        return new Intent(context, CollaboratorListActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName);
    }

    private String mUserLogin;
    private String mRepoName;

    @Override
    protected void onInitExtras(Bundle extras) {
        mUserLogin = extras.getString("owner");
        mRepoName = extras.getString("repo");
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.repo_collaborators);
    }

    @Override
    protected String getSubTitle() {
        return mUserLogin + "/" + mRepoName;
    }

    @Override
    protected Loader<LoaderResult<List<User>>> getUserListLoader() {
        return new CollaboratorListLoader(this, mUserLogin, mRepoName);
    }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mUserLogin, mRepoName);
    }
}