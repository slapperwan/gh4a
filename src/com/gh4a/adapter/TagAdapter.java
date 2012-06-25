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
package com.gh4a.adapter;

import java.util.List;

import org.eclipse.egit.github.core.RepositoryTag;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gh4a.BranchListActivity;
import com.gh4a.Constants;
import com.gh4a.FileManagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;

/**
 * The BranchTag adapter.
 */
public class TagAdapter extends RootAdapter<RepositoryTag> {

    /** The user login. */
    protected String mUserLogin;
    
    /** The repo name. */
    protected String mRepoName;

    /**
     * Instantiates a new branch tag adapter.
     * 
     * @param context the context
     * @param objects the objects
     */
    public TagAdapter(Context context, List<RepositoryTag> objects) {
        super(context, objects);
    }

    /**
     * Instantiates a new branch tag adapter.
     *
     * @param context the context
     * @param objects the objects
     * @param userLogin the user login
     * @param repoName the repo name
     */
    public TagAdapter(Context context, List<RepositoryTag> objects, String userLogin,
            String repoName) {
        super(context, objects);
        mUserLogin = userLogin;
        mRepoName = repoName;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.BaseAdapter#areAllItemsEnabled()
     */
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.BaseAdapter#isEnabled(int)
     */
    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.adapter.RootAdapter#doGetView(int, android.view.View,
     * android.view.ViewGroup)
     */
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_branch_tag, null);
        }

        final RepositoryTag branchTag = mObjects.get(position);
        if (branchTag != null) {
            TextView tvText = (TextView) v.findViewById(R.id.tv_title);
            tvText.setText(branchTag.getName());

            Button btnTree = (Button) v.findViewById(R.id.btn_tree);
            btnTree.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent()
                            .setClass(v.getContext(), FileManagerActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    intent.putExtra(Constants.Object.TREE_SHA, branchTag.getCommit().getSha());
                    intent.putExtra(Constants.Object.OBJECT_SHA, branchTag.getCommit().getSha());
                    intent.putExtra(Constants.Repository.REPO_BRANCH, branchTag.getName());
                    intent.putExtra(Constants.Object.PATH, "Tree");
//                    if (v.getContext() instanceof BranchListActivity) {
//                        intent.putExtra(Constants.VIEW_ID, R.id.btn_branches);
//                    }
//                    else {
//                        intent.putExtra(Constants.VIEW_ID, R.id.btn_tags);
//                    }
                    v.getContext().startActivity(intent);
                }
            });

            Button btnCommits = (Button) v.findViewById(R.id.btn_commits);
            btnCommits.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    int viewId = 0;
//                    if (v.getContext() instanceof BranchListActivity) {
//                        viewId = R.id.btn_branches;
//                    }
//                    else {
//                        viewId = R.id.btn_tags;
//                    }

                    ((Gh4Application) v.getContext().getApplicationContext())
                            .openCommitListActivity(v.getContext(), mUserLogin, mRepoName,
                                    branchTag.getName(), branchTag.getCommit().getSha(), viewId);
                }
            });
        }
        return v;
    }
}
