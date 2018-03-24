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
import android.support.v7.widget.RecyclerView;

import com.gh4a.ApiRequestException;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.CommitActivity;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.CommitCompare;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommitService;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

public class CommitCompareFragment extends ListDataBaseFragment<Commit> implements
        RootAdapter.OnItemClickListener<Commit> {
    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_BASE = "base";
    private static final String EXTRA_BASE_LABEL = "base_label";
    private static final String EXTRA_HEAD = "head";
    private static final String EXTRA_HEAD_LABEL = "head_label";
    private static final String EXTRA_PR = "pr";
    private static final int REQUEST_COMMIT = 2000;

    public static CommitCompareFragment newInstance(String repoOwner, String repoName,
            String baseRef, String headRef) {
        return newInstance(repoOwner, repoName, -1, null, baseRef, null, headRef);
    }

    public static CommitCompareFragment newInstance(String repoOwner, String repoName,
            int pullRequestNumber, String baseRefLabel, String baseRef,
            String headRefLabel, String headRef) {
        Bundle args = new Bundle();
        args.putString(EXTRA_OWNER, repoOwner);
        args.putString(EXTRA_REPO, repoName);
        args.putString(EXTRA_BASE, baseRef);
        args.putString(EXTRA_BASE_LABEL, baseRefLabel);
        args.putString(EXTRA_HEAD, headRef);
        args.putString(EXTRA_HEAD_LABEL, headRefLabel);
        args.putInt(EXTRA_PR, pullRequestNumber);

        CommitCompareFragment f = new CommitCompareFragment();
        f.setArguments(args);
        return f;
    }

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
        mRepoOwner = args.getString(EXTRA_OWNER);
        mRepoName = args.getString(EXTRA_REPO);
        mBase = args.getString(EXTRA_BASE);
        mBaseLabel = args.getString(EXTRA_BASE_LABEL);
        mHead = args.getString(EXTRA_HEAD);
        mHeadLabel = args.getString(EXTRA_HEAD_LABEL);
        mPullRequestNumber = args.getInt(EXTRA_PR, -1);
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
    protected Single<List<Commit>> onCreateDataSingle(boolean bypassCache) {
        RepositoryCommitService service = ServiceFactory.get(RepositoryCommitService.class, bypassCache);

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