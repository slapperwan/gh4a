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

import org.eclipse.egit.github.core.Milestone;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

public class MilestoneAdapter extends RootAdapter<Milestone> {
    private int mTextColorPrimary, mTextColorSecondary;

    public MilestoneAdapter(Context context) {
        super(context);
        mTextColorPrimary = UiUtils.resolveColor(context, android.R.attr.textColorPrimary);
        mTextColorSecondary = UiUtils.resolveColor(context, android.R.attr.textColorSecondary);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_milestone, null);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvOpen = (TextView) v.findViewById(R.id.tv_open);
        viewHolder.tvClosed = (TextView) v.findViewById(R.id.tv_closed);
        viewHolder.tvDue = (TextView) v.findViewById(R.id.tv_due);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Milestone milestone) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        viewHolder.tvTitle.setText(milestone.getTitle());
        viewHolder.tvTitle.setTextColor(Constants.Issue.STATE_CLOSED.equals(milestone.getState())
                ? mTextColorSecondary : mTextColorPrimary);

        if (!StringUtils.isBlank(milestone.getDescription())) {
            viewHolder.tvDesc.setVisibility(View.VISIBLE);
            viewHolder.tvDesc.setText(milestone.getDescription());
        } else {
            viewHolder.tvDesc.setVisibility(View.GONE);
        }

        viewHolder.tvOpen.setText(mContext.getString(R.string.issue_milestone_open_issues,
                milestone.getOpenIssues()));
        viewHolder.tvClosed.setText(mContext.getString(R.string.issue_milestone_closed_issues,
                milestone.getClosedIssues()));

        if (milestone.getDueOn() != null) {
            viewHolder.tvDue.setText(
                    DateFormat.getMediumDateFormat(mContext).format(milestone.getDueOn()));
            viewHolder.tvDue.setVisibility(View.VISIBLE);
        } else {
            viewHolder.tvDue.setVisibility(View.GONE);
        }
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvOpen;
        public TextView tvClosed;
        public TextView tvDue;
    }
}