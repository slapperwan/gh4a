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
package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.IssueMilestoneEditActivity;
import com.gh4a.adapter.MilestoneAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ActivityResultHelpers;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Milestone;
import com.meisolsson.githubsdk.service.issues.IssueMilestoneService;

import java.util.List;

import io.reactivex.Single;

public class IssueMilestoneListFragment extends ListDataBaseFragment<Milestone> implements
        RootAdapter.OnItemClickListener<Milestone> {
    private String mRepoOwner;
    private String mRepoName;
    private boolean mShowClosed;
    private boolean mFromPullRequest;

    private final ActivityResultLauncher<Intent> mEditMilestoneLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> {
                onRefresh();
                getActivity().setResult(Activity.RESULT_OK);
            })
    );

    public static IssueMilestoneListFragment newInstance(String repoOwner, String repoName,
            boolean showClosed, boolean fromPullRequest) {
        IssueMilestoneListFragment f = new IssueMilestoneListFragment();

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putBoolean("closed", showClosed);
        args.putBoolean("from_pr", fromPullRequest);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRepoOwner = args.getString("owner");
        mRepoName = args.getString("repo");
        mShowClosed = args.getBoolean("closed");
        mFromPullRequest = args.getBoolean("from_pr", false);
    }

    @Override
    protected RootAdapter<Milestone, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        MilestoneAdapter adapter = new MilestoneAdapter(getActivity());
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return mShowClosed
                ? R.string.no_closed_milestones_found
                : R.string.no_open_milestones_found;
    }

    @Override
    public void onItemClick(Milestone milestone) {
        Intent intent = IssueMilestoneEditActivity.makeEditIntent(
                getActivity(), mRepoOwner, mRepoName, milestone, mFromPullRequest);
        mEditMilestoneLauncher.launch(intent);
    }

    @Override
    protected Single<List<Milestone>> onCreateDataSingle(boolean bypassCache) {
        final IssueMilestoneService service =
                ServiceFactory.get(IssueMilestoneService.class, bypassCache);
        String targetState = mShowClosed ? "closed" : "open";

        return ApiHelpers.PageIterator
                .toSingle(page -> service.getRepositoryMilestones(mRepoOwner, mRepoName, targetState, page));
    }
}