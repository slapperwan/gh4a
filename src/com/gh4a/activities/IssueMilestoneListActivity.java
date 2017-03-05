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
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gh4a.BasePagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.IssueMilestoneListFragment;
import com.gh4a.fragment.LoadingListFragmentBase;
import com.gh4a.utils.UiUtils;

public class IssueMilestoneListActivity extends BasePagerActivity implements
        View.OnClickListener, LoadingListFragmentBase.OnRecyclerViewCreatedListener {
    public static Intent makeIntent(Context context, String repoOwner, String repoName) {
        return new Intent(context, IssueMilestoneListActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName);
    }

    private static final int[] TITLES = new int[] {
        R.string.open, R.string.closed
    };

    private String mRepoOwner;
    private String mRepoName;

    private FloatingActionButton mCreateFab;
    private IssueMilestoneListFragment mOpenFragment;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Gh4Application.get().isAuthorized()) {
            CoordinatorLayout rootLayout = getRootLayout();
            mCreateFab = (FloatingActionButton)
                    getLayoutInflater().inflate(R.layout.add_fab, rootLayout, false);
            mCreateFab.setOnClickListener(this);
            rootLayout.addView(mCreateFab);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_milestones);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        final IssueMilestoneListFragment f = IssueMilestoneListFragment.newInstance(mRepoOwner,
                mRepoName, position == 1);

        if (position == 0) {
            mOpenFragment = f;
        }
        return f;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
    }

    @Override
    public void onRecyclerViewCreated(Fragment fragment, RecyclerView recyclerView) {
        if (fragment == mOpenFragment) {
            recyclerView.setTag(R.id.FloatingActionButtonScrollEnabled, new Object());
        }
    }

    @Override
    protected void onPageMoved(int position, float fraction) {
        super.onPageMoved(position, fraction);
        if (mCreateFab != null) {
            float openFraction = 1 - position - fraction;
            ViewCompat.setScaleX(mCreateFab, openFraction);
            ViewCompat.setScaleY(mCreateFab, openFraction);
            mCreateFab.setVisibility(openFraction == 0 ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @Override
    protected int[][] getTabHeaderColors() {
        return new int[][] {
            {
                UiUtils.resolveColor(this, R.attr.colorIssueOpen),
                UiUtils.resolveColor(this, R.attr.colorIssueOpenDark)
            },
            {
                UiUtils.resolveColor(this, R.attr.colorIssueClosed),
                UiUtils.resolveColor(this, R.attr.colorIssueClosedDark)
            }
        };
    }

    @Override
    protected Intent navigateUp() {
        return IssueListActivity.makeIntent(this, mRepoOwner, mRepoName);
    }

    @Override
    public void onClick(View view) {
        startActivity(IssueMilestoneEditActivity.makeCreateIntent(this, mRepoOwner, mRepoName));
    }

    @Override
    protected void invalidateFragments() {
        mOpenFragment = null;
        super.invalidateFragments();
    }
}
