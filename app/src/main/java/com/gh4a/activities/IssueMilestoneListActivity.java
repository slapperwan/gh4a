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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.gh4a.utils.ActivityResultHelpers;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.gh4a.BaseFragmentPagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.IssueMilestoneListFragment;
import com.gh4a.fragment.LoadingListFragmentBase;

public class IssueMilestoneListActivity extends BaseFragmentPagerActivity implements
        View.OnClickListener, LoadingListFragmentBase.OnRecyclerViewCreatedListener {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            boolean fromPullRequest) {
        return new Intent(context, IssueMilestoneListActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("from_pr", fromPullRequest);
    }

    private static final int[] TITLES = new int[] {
        R.string.open, R.string.closed
    };

    private final ActivityResultLauncher<Intent> mCreateMilestoneLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> {
                onRefresh();
                setResult(RESULT_OK);
            })
    );

    private String mRepoOwner;
    private String mRepoName;
    private boolean mParentIsPullRequest;

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
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.issue_milestones);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        return IssueMilestoneListFragment.newInstance(mRepoOwner, mRepoName,
                position == 1, mParentIsPullRequest);
    }

    @Override
    protected void onFragmentInstantiated(Fragment f, int position) {
        if (position == 0) {
            mOpenFragment = (IssueMilestoneListFragment) f;
        }
    }

    @Override
    protected void onFragmentDestroyed(Fragment f) {
        if (f == mOpenFragment) {
            mOpenFragment = null;
        }
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mParentIsPullRequest = extras.getBoolean("from_pr", false);
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
            mCreateFab.setScaleX(openFraction);
            mCreateFab.setScaleY(openFraction);
            mCreateFab.setVisibility(openFraction == 0 ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @Override
    protected Intent navigateUp() {
        return IssueListActivity.makeIntent(this, mRepoOwner, mRepoName, mParentIsPullRequest);
    }

    @Override
    public void onClick(View view) {
        Intent intent = IssueMilestoneEditActivity.makeCreateIntent(this,
                mRepoOwner, mRepoName, mParentIsPullRequest);
        mCreateMilestoneLauncher.launch(intent);
    }

    @Override
    protected void invalidateFragments() {
        mOpenFragment = null;
        super.invalidateFragments();
    }
}
