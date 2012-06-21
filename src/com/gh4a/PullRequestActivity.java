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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.adapter.CommentAdapter;
import com.gh4a.utils.ImageDownloader;

/**
 * The PullRequest activity.
 */
public class PullRequestActivity extends BaseActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The pull request number. */
    protected int mPullRequestNumber;

    /** The header. */
    protected LinearLayout mHeader;

    /** The pull request adapter. */
    protected CommentAdapter mCommentAdapter;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The discussion loaded. */
    protected boolean mDiscussionLoaded;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pull_request);
        setUpActionBar();

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mPullRequestNumber = data.getInt(Constants.PullRequest.NUMBER);

        new LoadPullRequestTask(this).execute();
    }

    /**
     * Fill data into UI components.
     * 
     * @param pullRequest the pull request
     */
    protected void fillData(Map<String, Object> data) {
        ListView lvComments = (ListView) findViewById(R.id.lv_comments);
        final PullRequest pullRequest = (PullRequest) data.get("pullRequest");
        List<Comment> comments = (List<Comment>) data.get("comments");
        List<RepositoryCommit> commits = (List<RepositoryCommit>) data.get("commits");
        
        // set details inside listview header
        LayoutInflater infalter = getLayoutInflater();
        mHeader = (LinearLayout) infalter.inflate(R.layout.pull_request_header, lvComments, false);

        lvComments.addHeaderView(mHeader, null, true);

        mCommentAdapter = new CommentAdapter(PullRequestActivity.this, new ArrayList<Comment>());
        lvComments.setAdapter(mCommentAdapter);
        
        ImageView ivGravatar = (ImageView) mHeader.findViewById(R.id.iv_gravatar);
        ImageDownloader.getInstance().download(pullRequest.getUser().getGravatarId(),
                ivGravatar);
        ivGravatar.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getApplicationContext()
                        .openUserInfoActivity(PullRequestActivity.this,
                                pullRequest.getUser().getLogin(),
                                pullRequest.getUser().getName());
            }
        });
        TextView tvLogin = (TextView) mHeader.findViewById(R.id.tv_login);
        TextView tvCreateAt = (TextView) mHeader.findViewById(R.id.tv_created_at);
        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        TextView tvDesc = (TextView) mHeader.findViewById(R.id.tv_desc);

        tvLogin.setText(pullRequest.getUser().getLogin());
        tvCreateAt.setText(pt.format(pullRequest.getCreatedAt()));
        tvState.setText(pullRequest.getState());
        if ("closed".equals(pullRequest.getState())) {
            tvState.setBackgroundResource(R.drawable.default_red_box);
        }
        else {
            tvState.setBackgroundResource(R.drawable.default_green_box);
        }
        tvTitle.setText(pullRequest.getTitle());
        tvDesc.setText(pullRequest.getBody());
        
        LinearLayout llLabels = (LinearLayout) findViewById(R.id.ll_labels);
        
//        List<String> labels = pullRequest.getIssueUrl();
//        if (labels != null && !labels.isEmpty()) {
//            for (String label : labels) {
//                TextView tvLabel = new TextView(getApplicationContext());
//                tvLabel.setSingleLine(true);
//                tvLabel.setText(label);
//                tvLabel.setTextAppearance(getApplicationContext(), R.style.default_text_small);
//                tvLabel.setBackgroundResource(R.drawable.default_grey_box);
//                
//                llLabels.addView(tvLabel);
//            }
//            llLabels.setVisibility(View.VISIBLE);
//        }
//        else {
//            llLabels.setVisibility(View.GONE);
//        }
        
        LinearLayout llCommits = (LinearLayout) findViewById(R.id.ll_commits);
        llCommits.setBackgroundResource(R.drawable.default_grey_box);
        for (final RepositoryCommit commit : commits) {
            TextView tvName = new TextView(getApplicationContext());
            tvName.setText(commit.getCommit().getCommitter().getName() + " added a commit");
            tvName.setTextAppearance(getApplicationContext(), R.style.default_text_medium);
            llCommits.addView(tvName);
            
            TextView tvLabel = new TextView(getApplicationContext());
            tvLabel.setSingleLine(true);
            tvLabel.setText(commit.getSha().subSequence(0, 7) + " " + commit.getCommit().getMessage());
            tvLabel.setTextAppearance(getApplicationContext(), R.style.default_text_medium_url);
            tvLabel.setBackgroundResource(R.drawable.default_link);
            tvLabel.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    getApplicationContext().openCommitInfoActivity(PullRequestActivity.this, mUserLogin,
                            mRepoName, commit.getSha());
                }
            });
            
            llCommits.addView(tvLabel);
        }
        fillDiscussion(comments);
    }

    /**
     * Fill comment into UI components.
     * 
     * @param discussions the discussions
     */
    protected void fillDiscussion(List<Comment> comments) {
        if (comments != null && comments.size() > 0) {
            mCommentAdapter.notifyDataSetChanged();
            for (Comment comment : comments) {
                mCommentAdapter.add(comment);
            }
        }
        mCommentAdapter.notifyDataSetChanged();
        mDiscussionLoaded = true;
    }

    /**
     * An asynchronous task that runs on a background thread to load pull
     * request.
     */
    private static class LoadPullRequestTask extends AsyncTask<Void, Integer, Map<String, Object>> {

        /** The target. */
        private WeakReference<PullRequestActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load pull request task.
         *
         * @param activity the activity
         */
        public LoadPullRequestTask(PullRequestActivity activity) {
            mTarget = new WeakReference<PullRequestActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Map<String, Object> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    PullRequestActivity activity = mTarget.get();
                    Map<String, Object> m = new HashMap<String, Object>();
                    
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    PullRequestService pullRequestService = new PullRequestService(client);
                    PullRequest pullRequest = pullRequestService.getPullRequest(new RepositoryId(activity.mUserLogin, activity.mRepoName),
                            activity.mPullRequestNumber);
                    
                    //show commits
                    if (pullRequest.getCommits() > 0) {
                        List<RepositoryCommit> commits = pullRequestService.getCommits(new RepositoryId(activity.mUserLogin, activity.mRepoName), 
                                activity.mPullRequestNumber);
                        
                        m.put("commits", commits);
                    }
                    
                    IssueService issueService = new IssueService();
                    List<Comment> comments = issueService.getComments(activity.mUserLogin, 
                            activity.mRepoName, pullRequest.getNumber());
                    
                    m.put("pullRequest", pullRequest);
                    m.put("comments", comments);
                    
                    return m;
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

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Map<String, Object> result) {
            if (mTarget.get() != null) {
                PullRequestActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillData(result);
                }
            }
        }
    }

}
