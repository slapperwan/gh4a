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
import android.support.v7.widget.RecyclerView;
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

import org.eclipse.egit.github.core.Issue;

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
        AvatarHandler.assignAvatar(holder.ivGravatar, issue.getUser());
        holder.ivGravatar.setTag(issue);

        holder.lvLabels.setLabels(issue.getLabels());
        holder.tvNumber.setText("#" + issue.getNumber());
        holder.tvDesc.setText(issue.getTitle());
        holder.tvCreator.setText(ApiHelpers.getUserLogin(mContext, issue.getUser()));
        holder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext,
                issue.getCreatedAt(), true));

        if (issue.getComments() > 0) {
            holder.tvComments.setVisibility(View.VISIBLE);
            holder.tvComments.setText(String.valueOf(issue.getComments()));
        } else {
            holder.tvComments.setVisibility(View.GONE);
        }

        if (issue.getMilestone() != null) {
            holder.tvMilestone.setVisibility(View.VISIBLE);
            holder.tvMilestone.setText(issue.getMilestone().getTitle());
        } else {
            holder.tvMilestone.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Issue issue = (Issue) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext, issue.getUser());
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
            lvLabels = (LabelBadgeView) view.findViewById(R.id.labels);
            tvComments = (TextView) view.findViewById(R.id.tv_comments);
            tvMilestone = (TextView) view.findViewById(R.id.tv_milestone);
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