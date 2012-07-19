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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.ImageDownloader;

public class CommitActivity extends BaseActivity {

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.commit);
        setUpActionBar();

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mObjectSha = data.getString(Constants.Object.OBJECT_SHA);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getQuantityString(R.plurals.commit, 1) + " " + mObjectSha.substring(0, 7));
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        showLoading();
        new LoadCommitInfoTask(this).execute();
    }

    private static class LoadCommitInfoTask extends AsyncTask<Void, Integer, RepositoryCommit> {

        private WeakReference<CommitActivity> mTarget;
        private boolean mException;

        public LoadCommitInfoTask(CommitActivity activity) {
            mTarget = new WeakReference<CommitActivity>(activity);
        }

        @Override
        protected RepositoryCommit doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    CommitActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    CommitService commitService = new CommitService(client);
                    return commitService.getCommit(new RepositoryId(activity.mRepoOwner, activity.mRepoName),
                            activity.mObjectSha);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(RepositoryCommit result) {
            if (mTarget.get() != null) {
                mTarget.get().hideLoading();
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().fillData(result);
                }
            }
        }

    }

    private void fillData(final RepositoryCommit commit) {
        LinearLayout llChanged = (LinearLayout) findViewById(R.id.ll_changed);
        LinearLayout llAdded = (LinearLayout) findViewById(R.id.ll_added);
        LinearLayout llDeleted = (LinearLayout) findViewById(R.id.ll_deleted);

        ImageView ivGravatar = (ImageView) findViewById(R.id.iv_gravatar);
        
        ImageDownloader.getInstance().download(CommitUtils.getAuthorGravatarId(commit),
                ivGravatar);
        
        if (CommitUtils.getAuthorLogin(commit) != null) {
            ivGravatar.setOnClickListener(new OnClickListener() {
    
                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    getApplicationContext().openUserInfoActivity(CommitActivity.this,
                            CommitUtils.getAuthorLogin(commit), null);
                }
            });
        }
        
        TextView tvMessage = (TextView) findViewById(R.id.tv_message);
        tvMessage.setTypeface(getApplicationContext().regular);
        
        TextView tvExtra = (TextView) findViewById(R.id.tv_extra);
        tvExtra.setTypeface(getApplicationContext().regular);
        
        TextView tvSummary = (TextView) findViewById(R.id.tv_desc);
        tvSummary.setTypeface(getApplicationContext().regular);
        
        TextView tvChangeTitle = (TextView) findViewById(R.id.commit_changed);
        tvChangeTitle.setTypeface(getApplicationContext().boldCondensed);
        tvChangeTitle.setTextColor(Color.parseColor("#0099cc"));
        
        TextView tvAddedTitle = (TextView) findViewById(R.id.commit_added);
        tvAddedTitle.setTypeface(getApplicationContext().boldCondensed);
        tvAddedTitle.setTextColor(Color.parseColor("#0099cc"));
        
        TextView tvDeletedTitle = (TextView) findViewById(R.id.commit_deleted);
        tvDeletedTitle.setTypeface(getApplicationContext().boldCondensed);
        tvDeletedTitle.setTextColor(Color.parseColor("#0099cc"));
        
        tvMessage.setText(commit.getCommit().getMessage());
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(commit.getCommit().getCommitter().getDate());
        int timezoneOffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 3600000;
        cal.add(Calendar.HOUR, timezoneOffset);
        
        tvExtra.setText(CommitUtils.getAuthorName(commit) + " " + pt.format(cal.getTime()));

        List<CommitFile> addedFiles = new ArrayList<CommitFile>();
        List<CommitFile> removedFiles = new ArrayList<CommitFile>();
        List<CommitFile> modifiedFiles = new ArrayList<CommitFile>();
        
        //List<String> addedList = commit.getAdded();
        List<CommitFile> commitFiles = commit.getFiles();
        for (CommitFile commitFile : commitFiles) {
            String status = commitFile.getStatus();
            if ("added".equals(status)) {
                addedFiles.add(commitFile);
            }
            else if ("modified".equals(status)) {
                modifiedFiles.add(commitFile);
            }
            else if ("removed".equals(status)) {
                removedFiles.add(commitFile);
            }
        }
        
        for (final CommitFile file: addedFiles) {
            TextView tvFilename = new TextView(getApplicationContext());
            tvFilename.setText(file.getFilename());
            tvFilename.setPadding(0, 5, 0, 5);
            tvFilename.setTypeface(Typeface.MONOSPACE);
            tvFilename.setTextColor(Color.parseColor("#0099cc"));
            tvFilename.setBackgroundResource(R.drawable.default_link);
            tvFilename.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent().setClass(CommitActivity.this,
                            DiffViewerActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
                    intent.putExtra(Constants.Commit.DIFF, file.getPatch());
                    intent.putExtra(Constants.Object.PATH, file.getFilename());
                    intent.putExtra(Constants.Object.TREE_SHA, commit.getCommit().getTree().getSha());
                    startActivity(intent);
                }
            });
            
            llAdded.addView(tvFilename);
        }
        
        for (final CommitFile file: removedFiles) {
            TextView tvFilename = new TextView(getApplicationContext());
            tvFilename.setText(file.getFilename());
            tvFilename.setPadding(0, 5, 0, 5);
            tvFilename.setTypeface(Typeface.MONOSPACE);
            tvFilename.setTextColor(Color.BLACK);
            
            llDeleted.addView(tvFilename);
        }

        for (final CommitFile file: modifiedFiles) {
            TextView tvFilename = new TextView(getApplicationContext());
            tvFilename.setText(file.getFilename());
            tvFilename.setPadding(0, 5, 0, 5);
            tvFilename.setTypeface(Typeface.MONOSPACE);
            tvFilename.setTextColor(Color.parseColor("#0099cc"));
            tvFilename.setBackgroundResource(R.drawable.default_link);
            tvFilename.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent().setClass(CommitActivity.this,
                            DiffViewerActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
                    intent.putExtra(Constants.Commit.DIFF, file.getPatch());
                    intent.putExtra(Constants.Object.PATH, file.getFilename());
                    intent.putExtra(Constants.Object.TREE_SHA, commit.getCommit().getTree().getSha());
                    startActivity(intent);
                }
            });
            
            llChanged.addView(tvFilename);
        }

        if (addedFiles.size() == 0) {
            TextView tvFilename = new TextView(getApplicationContext());
            tvFilename.setTypeface(getApplicationContext().regular);
            tvFilename.setTextColor(Color.BLACK);
            tvFilename.setText(R.string.commit_no_files);
            llAdded.addView(tvFilename);
        }
        
        if (removedFiles.size() == 0) {
            TextView tvFilename = new TextView(getApplicationContext());
            tvFilename.setTypeface(getApplicationContext().regular);
            tvFilename.setTextColor(Color.BLACK);
            tvFilename.setText(R.string.commit_no_files);
            llDeleted.addView(tvFilename);
        }
        
        if (modifiedFiles.size() == 0) {
            TextView tvFilename = new TextView(getApplicationContext());
            tvFilename.setTypeface(getApplicationContext().regular);
            tvFilename.setTextColor(Color.BLACK);
            tvFilename.setText(R.string.commit_no_files);
            llChanged.addView(tvFilename);
        }
        
        tvSummary.setText(String.format(getResources().getString(R.string.commit_summary),
                commit.getFiles().size(), commit.getStats().getAdditions(), commit.getStats().getDeletions()));
    }
}