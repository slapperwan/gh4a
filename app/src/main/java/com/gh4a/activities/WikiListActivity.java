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

import org.eclipse.egit.github.core.GollumPage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.gh4a.R;
import com.gh4a.fragment.WikiListFragment;

public class WikiListActivity extends FragmentContainerActivity {
    public static Intent makeIntent(Context context, String repoOwner,
            String repoName, GollumPage initialPage) {
        String initialPageId = initialPage != null ? initialPage.getSha() : null;
        return new Intent(context, WikiListActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("initial_page", initialPageId);
    }

    private String mUserLogin;
    private String mRepoName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.recent_wiki);
        actionBar.setSubtitle(mUserLogin + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mUserLogin = extras.getString("owner");
        mRepoName = extras.getString("repo");
    }

    @Override
    protected Fragment onCreateFragment() {
        String initialPage = getIntent().getStringExtra("initial_page");
        return WikiListFragment.newInstance(mUserLogin, mRepoName, initialPage);
    }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mUserLogin, mRepoName);
    }
}
