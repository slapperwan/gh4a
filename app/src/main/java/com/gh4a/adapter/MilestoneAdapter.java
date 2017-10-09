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
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.Milestone;

public class MilestoneAdapter extends RootAdapter<Milestone, MilestoneAdapter.ViewHolder> {
    private final int mTextColorPrimary;
    private final int mTextColorSecondary;

    public MilestoneAdapter(Context context) {
        super(context);
        mTextColorPrimary = UiUtils.resolveColor(context, android.R.attr.textColorPrimary);
        mTextColorSecondary = UiUtils.resolveColor(context, android.R.attr.textColorSecondary);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_milestone, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Milestone milestone) {
        holder.tvTitle.setText(milestone.title());
        holder.tvTitle.setTextColor(milestone.state() == IssueState.Closed
                ? mTextColorSecondary : mTextColorPrimary);

        if (!StringUtils.isBlank(milestone.description())) {
            holder.tvDesc.setVisibility(View.VISIBLE);
            holder.tvDesc.setText(milestone.description());
        } else {
            holder.tvDesc.setVisibility(View.GONE);
        }

        holder.tvOpen.setText(mContext.getString(R.string.issue_milestone_open_issues,
                milestone.openIssues()));
        holder.tvClosed.setText(mContext.getString(R.string.issue_milestone_closed_issues,
                milestone.closedIssues()));

        if (milestone.dueOn() != null) {
            holder.tvDue.setText(
                    DateFormat.getMediumDateFormat(mContext).format(milestone.dueOn()));
            holder.tvDue.setVisibility(View.VISIBLE);
        } else {
            holder.tvDue.setVisibility(View.GONE);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_title);
            tvDesc = view.findViewById(R.id.tv_desc);
            tvOpen = view.findViewById(R.id.tv_open);
            tvClosed = view.findViewById(R.id.tv_closed);
            tvDue = view.findViewById(R.id.tv_due);
        }

        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvOpen;
        private final TextView tvClosed;
        private final TextView tvDue;
    }
}