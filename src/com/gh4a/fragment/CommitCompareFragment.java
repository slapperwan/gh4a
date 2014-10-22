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

import java.util.List;

import org.eclipse.egit.github.core.RepositoryCommit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.CommitCompareLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;

public class CommitCompareFragment extends ListDataBaseFragment<RepositoryCommit> {
    private static final int REQUEST_COMMIT = 2000;

    private String mRepoOwner;
    private String mRepoName;
    private String mBase;
    private String mHead;

    public static CommitCompareFragment newInstance(String repoOwner, String repoName,
            String base, String head) {
        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putString(Constants.Repository.BASE, base);
        args.putString(Constants.Repository.HEAD, head);

        CommitCompareFragment f = new CommitCompareFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mRepoOwner = args.getString(Constants.Repository.OWNER);
        mRepoName = args.getString(Constants.Repository.NAME);
        mBase = args.getString(Constants.Repository.BASE);
        mHead = args.getString(Constants.Repository.HEAD);
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_commits_found;
    }

    @Override
    protected RootAdapter<RepositoryCommit> onCreateAdapter() {
        return new CommitAdapter(getActivity());
    }

    @Override
    protected void onItemClick(RepositoryCommit commit) {
        Intent intent = IntentUtils.getCommitInfoActivityIntent(getActivity(),
                mRepoOwner, mRepoName, commit.getSha());
        startActivityForResult(intent, REQUEST_COMMIT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COMMIT) {
            if (resultCode == Activity.RESULT_OK) {
                // comments were updated
                refresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public Loader<LoaderResult<List<RepositoryCommit>>> onCreateLoader(int id, Bundle args) {
        return new CommitCompareLoader(getActivity(), mRepoOwner, mRepoName, mBase, mHead);
    }
}