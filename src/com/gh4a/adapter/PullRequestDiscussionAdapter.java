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

import android.content.Context;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.Discussion;
import com.github.api.v2.schema.PullRequest;
import com.github.api.v2.schema.Repository;

/**
 * The Comment adapter.
 */
public class PullRequestDiscussionAdapter extends RootAdapter<Discussion> {

    /** The repo name. */
    protected String mRepoName;
    
    /** The pull request. */
    protected PullRequest mPullRequest;
    
    /**
     * Instantiates a new comment adapter.
     *
     * @param context the context
     * @param objects the objects
     * @param repoName the repo name
     * @param issueUserLogin the issue user login
     */
    public PullRequestDiscussionAdapter(Context context, List<Discussion> objects, String repoName, PullRequest pullRequest) {
        super(context, objects);
        mRepoName = repoName;
        mPullRequest = pullRequest;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.adapter.RootAdapter#doGetView(int, android.view.View,
     * android.view.ViewGroup)
     */
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.pull_request_discussion_row, null);
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvUser = (TextView) v.findViewById(R.id.tv_login);
            viewHolder.tvDate = (TextView) v.findViewById(R.id.tv_created_at);
            viewHolder.tvSha = (TextView) v.findViewById(R.id.tv_sha);
            viewHolder.tvBody = (TextView) v.findViewById(R.id.tv_desc);

            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final Gh4Application context = (Gh4Application) v.getContext().getApplicationContext();
        final Discussion discussion = mObjects.get(position);
        if (discussion != null) {
            ImageDownloader imageDownloader = ImageDownloader.getInstance();
            if (Discussion.Type.COMMIT.equals(discussion.getType())) {
                imageDownloader.download(StringUtils.md5Hex(discussion.getAuthor().getEmail()),
                        viewHolder.ivGravatar);
            }
            else if (Discussion.Type.ISSUE_COMMENT.equals(discussion.getType())) {
                imageDownloader.download(discussion.getGravatarId(), viewHolder.ivGravatar);
            }
            else if (Discussion.Type.PULL_REQUEST_REVIEW_COMMENT.equals(discussion.getType())) {
                imageDownloader.download(StringUtils.md5Hex(discussion.getUser().getEmail()),
                        viewHolder.ivGravatar);
            }
            else if (Discussion.Type.COMMIT_COMMENT.equals(discussion.getType())) {
                imageDownloader.download(discussion.getUser().getGravatarId(),
                        viewHolder.ivGravatar);
            }

            viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    if (Discussion.Type.COMMIT.equals(discussion.getType())) {
                        // if user is on github
                        if (!StringUtils.isBlank(discussion.getAuthor().getLogin())) {
                            context.openUserInfoActivity(v.getContext(), discussion.getAuthor()
                                    .getLogin(), null);
                        }
                        // else get from PullRequest issue user
                        else {
                            context.openUserInfoActivity(v.getContext(), mPullRequest.getIssueUser().getLogin(), null);
                        }
                    }
                    else if (Discussion.Type.ISSUE_COMMENT.equals(discussion.getType())) {
                        context.openUserInfoActivity(v.getContext(), discussion.getUser()
                                .getLogin(), null);
                    }
                    else if (Discussion.Type.PULL_REQUEST_REVIEW_COMMENT.equals(discussion
                            .getType())) {
                        context.openUserInfoActivity(v.getContext(), discussion.getUser()
                                .getLogin(), null);
                    }
                    else if (Discussion.Type.COMMIT_COMMENT.equals(discussion
                            .getType())) {
                        context.openUserInfoActivity(v.getContext(), discussion.getUser()
                                .getLogin(), null);
                    }
                }
            });

            viewHolder.tvSha.setText(null);
            if (Discussion.Type.COMMIT.equals(discussion.getType())) {
                viewHolder.tvUser.setText(StringUtils.ifNullDefaultTo(discussion.getAuthor()
                        .getLogin(), discussion.getAuthor().getName()));
                viewHolder.tvDate.setText(pt.format(discussion.getCommittedDate()));
                viewHolder.tvSha.setVisibility(View.VISIBLE);
                
                SpannableString content = new SpannableString("Commit "
                        + discussion.getId().substring(0, 7));
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                viewHolder.tvSha.append(content);
                viewHolder.tvSha.setBackgroundResource(R.drawable.default_link);
                viewHolder.tvSha.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (mPullRequest.getHead().getRepository() != null) {
                            Repository repository = mPullRequest.getHead().getRepository();
                            context.openCommitInfoActivity(v.getContext(), 
                                    repository.getOwner(), 
                                    repository.getName(),
                                    discussion.getId());
                        }
                        else if (mPullRequest.getBase().getRepository() != null) {
                            Repository repository = mPullRequest.getBase().getRepository();
                            context.openCommitInfoActivity(v.getContext(), 
                                    repository.getOwner(), 
                                    repository.getName(),
                                    discussion.getId());
                        }
                    }
                });
                
                viewHolder.tvBody.setText(discussion.getMessage());
            }
            else if (Discussion.Type.ISSUE_COMMENT.equals(discussion.getType())) {
                viewHolder.tvUser.setText(discussion.getUser().getLogin());
                viewHolder.tvDate.setText(pt.format(discussion.getCreatedAt()));
                viewHolder.tvBody.setText(discussion.getBody());
                viewHolder.tvSha.setVisibility(View.GONE);
            }
            else if (Discussion.Type.PULL_REQUEST_REVIEW_COMMENT.equals(discussion.getType())) {
                viewHolder.tvUser.setText(discussion.getUser().getLogin());
                viewHolder.tvDate.setText(pt.format(discussion.getCreatedAt()));
                viewHolder.tvBody.setText(discussion.getBody());
                viewHolder.tvSha.setVisibility(View.GONE);
            }
            else if (Discussion.Type.COMMIT_COMMENT.equals(discussion.getType())) {
                viewHolder.tvUser.setText(discussion.getUser().getLogin());
                viewHolder.tvDate.setText(pt.format(discussion.getCreatedAt()));
                viewHolder.tvSha.setVisibility(View.VISIBLE);
                viewHolder.tvSha.setText(v.getResources().getString(R.string.commit_comment,
                        discussion.getCommitId().substring(0, 7)));
                viewHolder.tvBody.setText(discussion.getBody());
            }
        }
        return v;
    }

    /**
     * The Class ViewHolder.
     */
    protected class ViewHolder {
        
        /** The iv gravatar. */
        public ImageView ivGravatar;
        
        /** The tv user. */
        public TextView tvUser;
        
        /** The tv date. */
        public TextView tvDate;
        
        /** The tv sha. */
        public TextView tvSha;
        
        /** The tv body. */
        public TextView tvBody;
    }

}