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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CommitActivity;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.CommitCompare;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommitService;

import io.reactivex.Single;

public class CommitCompareFragment extends ListDataBaseFragment<Commit> implements
        RootAdapter.OnItemClickListener<Commit> {
    public static CommitCompareFragment newInstance(String repoOwner, String repoName,
            String baseRef, String headRef) {
        return newInstance(repoOwner, repoName, -1, null, baseRef, null, headRef);
    }

    public static CommitCompareFragment newInstance(String repoOwner, String repoName,
            int pullRequestNumber, String baseRefLabel, String baseRef,
            String headRefLabel, String headRef) {
        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putString("base", baseRef);
        args.putString("base_label", baseRefLabel);
        args.putString("head", headRef);
        args.putString("head_label", headRefLabel);
        args.putInt("pr", pullRequestNumber);

        CommitCompareFragment f = new CommitCompareFragment();
        f.setArguments(args);
        return f;
    }

    private static final int REQUEST_COMMIT = 2000;

    private String mRepoOwner;
    private String mRepoName;
    private String mBase;
    private String mBaseLabel;
    private String mHead;
    private String mHeadLabel;
    private int mPullRequestNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mRepoOwner = args.getString("owner");
        mRepoName = args.getString("repo");
        mBase = args.getString("base");
        mBaseLabel = args.getString("base_label");
        mHead = args.getString("head");
        mHeadLabel = args.getString("head_label");
        mPullRequestNumber = args.getInt("pr", -1);
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_commits_found;
    }

    @Override
    protected RootAdapter<Commit, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        CommitAdapter adapter = new CommitAdapter(getActivity());
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    public void onItemClick(Commit commit) {
        Intent intent = CommitActivity.makeIntent(getActivity(),
                mRepoOwner, mRepoName, mPullRequestNumber, commit.sha());
        startActivityForResult(intent, REQUEST_COMMIT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COMMIT) {
            if (resultCode == Activity.RESULT_OK) {
                // comments were updated
                onRefresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected Single<List<Commit>> onCreateDataSingle() {
        RepositoryCommitService service =
                Gh4Application.get().getGitHubService(RepositoryCommitService.class);

        Single<CommitCompare> compareSingle = service.compareCommits(mRepoOwner, mRepoName, mBase, mHead)
                .map(ApiHelpers::throwOnFailure)
                .onErrorResumeNext(error -> {
                    if (error instanceof ApiRequestException) {
                        if (((ApiRequestException) error).getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
                            // We got a 404; likely the history of the base branch was rewritten. Try the labels.
                            return service.compareCommits(mRepoOwner, mRepoName, mBaseLabel, mHeadLabel)
                                    .map(ApiHelpers::throwOnFailure);
                        }
                    }
                    return Single.error(error);
                });

        return compareSingle
                .map(CommitCompare::commits)
                // Bummer, at least one branch was deleted.
                // Can't do anything here, so return an empty list.
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND,new ArrayList<>()));
    }
}