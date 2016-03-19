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

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.User;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

public class PullRequestAdapter extends RootAdapter<PullRequest, PullRequestAdapter.ViewHolder> {
    public PullRequestAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_issue, parent, false);
        v.findViewById(R.id.labels).setVisibility(View.GONE);

        ViewHolder holder = new ViewHolder(v);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, PullRequest pullRequest) {
        final User user = pullRequest.getUser();

        AvatarHandler.assignAvatar(holder.ivGravatar, user);
        holder.ivGravatar.setTag(pullRequest.getUser());

        holder.tvNumber.setText("#" + pullRequest.getNumber());
        holder.tvDesc.setText(pullRequest.getTitle());
        holder.tvCreator.setText(ApiHelpers.getUserLogin(mContext, user));
        holder.tvTimestamp.setText(
                StringUtils.formatRelativeTime(mContext, pullRequest.getCreatedAt(), true));

        int comments = pullRequest.getComments() + pullRequest.getReviewComments();
        if (comments > 0) {
            holder.tvComments.setVisibility(View.VISIBLE);
            holder.tvComments.setText(String.valueOf(comments));
        } else {
            holder.tvComments.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            User user = (User) v.getTag();
            Intent intent = IntentUtils.getUserActivityIntent(mContext, user);
            if (intent != null) {
                mContext.startActivity(intent);
            }
        } else {
            super.onClick(v);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            ivGravatar = (ImageView) view.findViewById(R.id.iv_gravatar);
            tvDesc = (TextView) view.findViewById(R.id.tv_desc);
            tvCreator = (TextView) view.findViewById(R.id.tv_creator);
            tvTimestamp = (TextView) view.findViewById(R.id.tv_timestamp);
            tvNumber = (TextView) view.findViewById(R.id.tv_number);
            tvComments = (TextView) view.findViewById(R.id.tv_comments);
        }

        private ImageView ivGravatar;
        private TextView tvCreator;
        private TextView tvTimestamp;
        private TextView tvDesc;
        private TextView tvNumber;
        private TextView tvComments;
    }
}