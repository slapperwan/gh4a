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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryCommit;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.adapter.CommentAdapter;
import com.gh4a.loader.IssueCommentsLoader;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.loader.RepositoryCommitsLoader;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.ImageDownloader;

public class PullRequestActivity extends BaseSherlockFragmentActivity
    implements LoaderManager.LoaderCallbacks {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private LinearLayout mHeader;
    private CommentAdapter mCommentAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pull_request);
        setUpActionBar();

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mPullRequestNumber = data.getInt(Constants.PullRequest.NUMBER);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.pull_request_title) + " #" + mPullRequestNumber);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().getLoader(0).forceLoad();
        
        getSupportLoaderManager().initLoader(1, null, this);
        getSupportLoaderManager().initLoader(2, null, this);
    }

    private void fillData(final PullRequest pullRequest) {
        getSupportLoaderManager().getLoader(1).forceLoad();
        getSupportLoaderManager().getLoader(2).forceLoad();
        
        ListView lvComments = (ListView) findViewById(R.id.lv_comments);
        
        // set details inside listview header
        LayoutInflater infalter = getLayoutInflater();
        mHeader = (LinearLayout) infalter.inflate(R.layout.pull_request_header, lvComments, false);
        mHeader.setClickable(false);
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
        
        TextView tvExtra = (TextView) mHeader.findViewById(R.id.tv_extra);
        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        
        TextView tvDesc = (TextView) mHeader.findViewById(R.id.tv_desc);
        TextView tvCommentTitle = (TextView) mHeader.findViewById(R.id.comment_title);
        tvCommentTitle.setTypeface(getApplicationContext().boldCondensed);
        tvCommentTitle.setTextColor(Color.parseColor("#0099cc"));
        tvCommentTitle.setText(getResources().getString(R.string.pull_request_comments) + " (" + pullRequest.getComments() + ")");
        
        tvState.setText(pullRequest.getState());
        if ("closed".equals(pullRequest.getState())) {
            tvState.setBackgroundResource(R.drawable.default_red_box);
        }
        else {
            tvState.setBackgroundResource(R.drawable.default_green_box);
        }
        tvTitle.setText(pullRequest.getTitle());
        tvTitle.setTypeface(getApplicationContext().boldCondensed);
        
        String body = pullRequest.getBody();
        body = body.replaceAll("\n", "<br/>");
        tvDesc.setText(Html.fromHtml(body));
        tvDesc.setTypeface(getApplicationContext().regular);
        
        tvExtra.setText(getResources().getString(R.string.issue_open_by_user,
                pullRequest.getUser().getLogin(),
                pt.format(pullRequest.getCreatedAt())));
        
    }
    
    private void fillCommits(List<RepositoryCommit> commits) {
        LinearLayout llCommits = (LinearLayout) findViewById(R.id.ll_commits);
        llCommits.setBackgroundResource(R.drawable.default_info_box);
        for (final RepositoryCommit commit : commits) {
            TextView tvName = new TextView(getApplicationContext());
            tvName.setText(CommitUtils.getAuthorLogin(commit) + " added a commit");
            tvName.setTextAppearance(getApplicationContext(), R.style.default_text_medium);
            llCommits.addView(tvName);
            
            TextView tvLabel = new TextView(getApplicationContext());
            tvLabel.setSingleLine(true);
            tvLabel.setText(commit.getSha().subSequence(0, 7) + " " + commit.getCommit().getMessage());
            tvLabel.setTextAppearance(getApplicationContext(), R.style.default_text_small_url);
            tvLabel.setBackgroundResource(R.drawable.default_link);
            tvLabel.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    getApplicationContext().openCommitInfoActivity(PullRequestActivity.this, mRepoOwner,
                            mRepoName, commit.getSha());
                }
            });
            
            llCommits.addView(tvLabel);
        }
    }
    private void fillDiscussion(List<Comment> comments) {
        if (comments != null && comments.size() > 0) {
            mCommentAdapter.notifyDataSetChanged();
            for (Comment comment : comments) {
                mCommentAdapter.add(comment);
            }
        }
        mCommentAdapter.notifyDataSetChanged();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle arg1) {
        if (id == 0) {
            return new PullRequestLoader(this, mRepoOwner, mRepoName, mPullRequestNumber);
        }
        else if (id == 1) {
            return new IssueCommentsLoader(this, mRepoOwner, mRepoName, mPullRequestNumber);
        }
        else {
            return new RepositoryCommitsLoader(this, mRepoOwner, mRepoName, mPullRequestNumber);
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        if (loader.getId() == 0) {
            fillData((PullRequest) object);
        }
        else if (loader.getId() == 1) {
            fillDiscussion((List<Comment>) object);
        }
        else {
            fillCommits((List<RepositoryCommit>) object);
        }
    }

    @Override
    public void onLoaderReset(Loader arg0) {
        // TODO Auto-generated method stub
        
    }
}
