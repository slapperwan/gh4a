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
import com.gh4a.fragment.CommitCompareFragment;

public class CompareActivity extends FragmentContainerActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String baseRef, String headRef) {
        return new Intent(context, CompareActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("base", baseRef)
                .putExtra("head", headRef);
    }

    private String mRepoOwner;
    private String mRepoName;

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.commit_compare);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
    }

    @Override
    protected Fragment onCreateFragment() {
        String base = getIntent().getStringExtra("base");
        String head = getIntent().getStringExtra("head");

        return CommitCompareFragment.newInstance(mRepoOwner, mRepoName, base, head);
    }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mRepoOwner, mRepoName);
    }
}