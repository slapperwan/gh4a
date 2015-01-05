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
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.gh4a.BasePagerActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.PullRequestListFragment;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;

public class PullRequestListActivity extends BasePagerActivity {
    private String mRepoOwner;
    private String mRepoName;

    private static final int[] TITLES = new int[] {
        R.string.open, R.string.closed
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.NAME);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.pull_requests);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
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
    protected Fragment getFragment(int position) {
        return PullRequestListFragment.newInstance(mRepoOwner, mRepoName,
                position == 1 ? Constants.Issue.STATE_CLOSED : Constants.Issue.STATE_OPEN);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mRepoOwner, mRepoName, null);
    }
}
