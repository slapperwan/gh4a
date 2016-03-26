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

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.IssueMilestoneListFragment;
import com.gh4a.fragment.LoadingListFragmentBase;
import com.gh4a.utils.IntentUtils;

public class IssueMilestoneListActivity extends FragmentContainerActivity implements
        View.OnClickListener, LoadingListFragmentBase.OnRecyclerViewCreatedListener {
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
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
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
        return IntentUtils.getIssueListActivityIntent(this,
                mRepoOwner, mRepoName, Constants.Issue.STATE_OPEN);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, IssueMilestoneEditActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        startActivity(intent);
    }
}
