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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.WikiListFragment;
import com.meisolsson.githubsdk.model.GitHubWikiPage;

public class WikiListActivity extends FragmentContainerActivity {

    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_INITIAL_PAGE = "initial_page";

    public static Intent makeIntent(Context context, String repoOwner,
            String repoName, GitHubWikiPage initialPage) {
        String initialPageId = initialPage != null ? initialPage.sha() : null;
        return new Intent(context, WikiListActivity.class)
                .putExtra(EXTRA_OWNER, repoOwner)
                .putExtra(EXTRA_REPO, repoName)
                .putExtra(EXTRA_INITIAL_PAGE, initialPageId);
    }

    private String mUserLogin;
    private String mRepoName;

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.recent_wiki);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mUserLogin + "/" + mRepoName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mUserLogin = extras.getString(EXTRA_OWNER);
        mRepoName = extras.getString(EXTRA_REPO);
    }

    @Override
    protected Fragment onCreateFragment() {
        String initialPage = getIntent().getStringExtra(EXTRA_INITIAL_PAGE);
        return WikiListFragment.newInstance(mUserLogin, mRepoName, initialPage);
    }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mUserLogin, mRepoName);
    }
}
