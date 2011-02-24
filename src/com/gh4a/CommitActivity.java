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
package com.gh4a;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.Commit;
import com.github.api.v2.schema.Delta;
import com.github.api.v2.services.CommitService;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;

/**
 * The Commit activity.
 */
public class CommitActivity extends BaseActivity {

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The object sha. */
    protected String mObjectSha;
    
    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.commit);
        setUpActionBar();

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mObjectSha = data.getString(Constants.Object.OBJECT_SHA);

        setBreadCrumb();

        new LoadCommitInfoTask(this).execute();
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[2];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);
        data.put(Constants.Repository.REPO_NAME, mRepoName);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        // Repo
        b = new BreadCrumbHolder();
        b.setLabel(mRepoName);
        b.setTag(Constants.Repository.REPO_NAME);
        b.setData(data);
        breadCrumbHolders[1] = b;

        createBreadcrumb("Commit - " + mObjectSha.substring(0, 7), breadCrumbHolders);
    }

    /**
     * An asynchronous task that runs on a background thread to load commit
     * info.
     */
    private static class LoadCommitInfoTask extends AsyncTask<Void, Integer, Commit> {

        /** The target. */
        private WeakReference<CommitActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load commit info task.
         *
         * @param activity the activity
         */
        public LoadCommitInfoTask(CommitActivity activity) {
            mTarget = new WeakReference<CommitActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Commit doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    CommitActivity activity = mTarget.get();
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    CommitService commitService = factory.createCommitService();
                    return commitService.getCommit(activity.mUserLogin, activity.mRepoName,
                            activity.mObjectSha);
                }
                catch (GitHubException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                CommitActivity activity = mTarget.get();
                activity.mLoadingDialog = LoadingDialog.show(activity, true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Commit result) {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog.dismiss();
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().fillData(result);
                }
            }
        }

    }

    /**
     * Fill data into UI components.
     * 
     * @param commit the commit
     */
    protected void fillData(final Commit commit) {
        LinearLayout llChanged = (LinearLayout) findViewById(R.id.ll_changed);
        LinearLayout llAdded = (LinearLayout) findViewById(R.id.ll_added);
        LinearLayout llDeleted = (LinearLayout) findViewById(R.id.ll_deleted);

        ImageView ivGravatar = (ImageView) findViewById(R.id.iv_gravatar);
        ImageDownloader.getInstance().download(StringUtils.md5Hex(commit.getAuthor().getEmail()),
                ivGravatar);
        if (!StringUtils.isBlank(commit.getAuthor().getLogin())) {
            ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    getApplicationContext().openUserInfoActivity(CommitActivity.this,
                            commit.getAuthor().getLogin(), commit.getAuthor().getName());
                }
            });
        }
        TextView tvMessage = (TextView) findViewById(R.id.tv_message);
        TextView tvExtra = (TextView) findViewById(R.id.tv_extra);
        TextView tvSummary = (TextView) findViewById(R.id.tv_desc);

        Resources res = getResources();
        String extraDataFormat = res.getString(R.string.more_data);

        tvMessage.setText(commit.getMessage());
        tvExtra.setText(String.format(extraDataFormat, !StringUtils.isBlank(commit.getAuthor()
                .getLogin()) ? commit.getAuthor().getLogin() : commit.getAuthor().getName(), pt
                .format(commit.getCommittedDate())));

        int addedCount = 0;
        int removedCount = 0;
        int modifiedCount = 0;

        List<String> addedList = commit.getAdded();
        if (addedList != null) {
            addedCount = addedList.size();
            for (final String filename : addedList) {
                TextView tvFilename = new TextView(getApplicationContext());
                SpannableString content = new SpannableString(filename);
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                tvFilename.setText(content);
                tvFilename.setTextAppearance(getApplicationContext(),
                        R.style.default_text_medium_url);
                tvFilename.setBackgroundResource(R.drawable.default_link);
                tvFilename.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent().setClass(CommitActivity.this,
                                AddedFileViewerActivity.class);
                        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                        intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
                        intent.putExtra(Constants.Object.TREE_SHA, commit.getTree());
                        intent.putExtra(Constants.Object.PATH, filename);
                        startActivity(intent);
                    }
                });
                llAdded.addView(tvFilename);
            }
        }
        
        if (addedCount == 0) {
            TextView tvFilename = new TextView(getApplicationContext());
            tvFilename.setTextAppearance(getApplicationContext(),
                    R.style.default_text_medium);
            tvFilename.setText(R.string.commit_no_files);
            llAdded.addView(tvFilename);
        }

        List<String> removedList = commit.getRemoved();
        if (removedList != null) {
            removedCount = removedList.size();
            for (final String filename : removedList) {
                TextView tvFilename = new TextView(getApplicationContext());
                tvFilename.setText(filename);
                tvFilename.setTextAppearance(getApplicationContext(),
                        R.style.default_text_medium);
                llDeleted.addView(tvFilename);
            }
        }

        if (removedCount == 0) {
            TextView tvFilename = new TextView(getApplicationContext());
            tvFilename.setTextAppearance(getApplicationContext(),
                    R.style.default_text_medium);
            tvFilename.setText(R.string.commit_no_files);
            llDeleted.addView(tvFilename);
        }
        
        List<Delta> modifiedList = commit.getModified();
        if (modifiedList != null) {
            for (final Delta delta : modifiedList) {
                TextView tvFilename = new TextView(getApplicationContext());
                SpannableString content = new SpannableString(delta.getFilename());
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                tvFilename.setText(content);
                tvFilename.setTextAppearance(getApplicationContext(),
                        R.style.default_text_medium_url);
                tvFilename.setBackgroundResource(R.drawable.default_link);
                tvFilename.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent().setClass(CommitActivity.this,
                                DiffViewerActivity.class);
                        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                        intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
                        intent.putExtra(Constants.Commit.DIFF, delta.getDiff());
                        intent.putExtra(Constants.Object.PATH, delta.getFilename());
                        startActivity(intent);
                    }
                });
                llChanged.addView(tvFilename);
            }
            modifiedCount = modifiedList.size();
        }

        if (modifiedCount == 0) {
            TextView tvFilename = new TextView(getApplicationContext());
            tvFilename.setTextAppearance(getApplicationContext(),
                    R.style.default_text_medium);
            tvFilename.setText(R.string.commit_no_files);
            llChanged.addView(tvFilename);
        }
        
        tvSummary.setText(String.format(getResources().getString(R.string.commit_summary),
                modifiedCount, addedCount, removedCount));

        mLoadingDialog.dismiss();
    }
}