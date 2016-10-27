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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.IssueMilestoneListFragment;
import com.gh4a.fragment.LoadingListFragmentBase;

public class IssueMilestoneListActivity extends FragmentContainerActivity implements
        View.OnClickListener, LoadingListFragmentBase.OnRecyclerViewCreatedListener {
    public static Intent makeIntent(Context context, String repoOwner, String repoName) {
        return new Intent(context, IssueMilestoneListActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName);
    }

    private String mRepoOwner;
    private String mRepoName;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Gh4Application.get().isAuthorized()) {
            CoordinatorLayout rootLayout = getRootLayout();
            FloatingActionButton fab = (FloatingActionButton)
                    getLayoutInflater().inflate(R.layout.add_fab, rootLayout, false);
            fab.setOnClickListener(this);
            rootLayout.addView(fab);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_milestones);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
    }

    @Override
    protected Fragment onCreateFragment() {
        return IssueMilestoneListFragment.newInstance(mRepoOwner, mRepoName);
    }

    @Override
    public void onRecyclerViewCreated(Fragment fragment, RecyclerView recyclerView) {
        recyclerView.setTag(R.id.FloatingActionButtonScrollEnabled, new Object());
    }

    @Override
    protected Intent navigateUp() {
        return IssueListActivity.makeIntent(this, mRepoOwner, mRepoName);
    }

    @Override
    public void onClick(View view) {
        startActivity(IssueMilestoneEditActivity.makeCreateIntent(this, mRepoOwner, mRepoName));
    }
}
