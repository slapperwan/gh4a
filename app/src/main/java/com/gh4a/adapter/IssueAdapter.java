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

import android.content.Context;
import android.content.Intent;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.widget.LabelBadgeView;
import com.meisolsson.githubsdk.model.Issue;

public class IssueAdapter extends RootAdapter<Issue, IssueAdapter.ViewHolder> {
    public IssueAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_issue, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, Issue issue) {
        AvatarHandler.assignAvatar(holder.ivGravatar, issue.user());
        holder.ivGravatar.setTag(issue);

        holder.lvLabels.setLabels(issue.labels());
        holder.tvNumber.setText("#" + issue.number());
        holder.tvDesc.setText(issue.title());
        holder.tvCreator.setText(ApiHelpers.getUserLoginWithType(mContext, issue.user()));
        holder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext,
                issue.createdAt(), true));

        if (issue.comments() > 0) {
            holder.tvComments.setVisibility(View.VISIBLE);
            holder.tvComments.setText(String.valueOf(issue.comments()));
        } else {
            holder.tvComments.setVisibility(View.GONE);
        }

        if (issue.milestone() != null) {
            holder.tvMilestone.setVisibility(View.VISIBLE);
            holder.tvMilestone.setText(issue.milestone().title());
        } else {
            holder.tvMilestone.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Issue issue = (Issue) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext, issue.user());
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
            ivGravatar = view.findViewById(R.id.iv_gravatar);
            tvDesc = view.findViewById(R.id.tv_desc);
            tvCreator = view.findViewById(R.id.tv_creator);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvNumber = view.findViewById(R.id.tv_number);
            lvLabels = view.findViewById(R.id.labels);
            tvComments = view.findViewById(R.id.tv_comments);
            tvMilestone = view.findViewById(R.id.tv_milestone);
        }

        final ImageView ivGravatar;
        final TextView tvNumber;
        final TextView tvDesc;
        final TextView tvCreator;
        final TextView tvTimestamp;
        final LabelBadgeView lvLabels;
        final TextView tvComments;
        final TextView tvMilestone;
    }
}