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

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryIssue;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.GravatarUtils;

public class RepositoryIssueAdapter extends RootAdapter<RepositoryIssue> implements OnClickListener {
    private AQuery aq;
    
    public RepositoryIssueAdapter(Context context) {
        super(context);
        aq = new AQuery(context);
    }
    
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(mContext);

        if (v == null) {
            v = inflater.inflate(R.layout.row_issue, null);
            
            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.ivGravatar.setOnClickListener(this);

            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(boldCondensed);
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.tvNumber = (TextView) v.findViewById(R.id.tv_number);
            viewHolder.llLabels = (LinearLayout) v.findViewById(R.id.ll_labels);
            viewHolder.tvState = (TextView) v.findViewById(R.id.tv_state);
            viewHolder.ivAssignee = (ImageView) v.findViewById(R.id.iv_assignee);
            viewHolder.tvComments = (TextView) v.findViewById(R.id.tv_comments);
            viewHolder.tvRepo = (TextView) v.findViewById(R.id.tv_repo);
            viewHolder.tvRepo.setVisibility(View.VISIBLE);
            viewHolder.tvMilestone = (TextView) v.findViewById(R.id.tv_milestone);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final RepositoryIssue issue = mObjects.get(position);
        
        aq.recycle(convertView);
        aq.id(viewHolder.ivGravatar).image(GravatarUtils.getGravatarUrl(issue.getUser().getGravatarId()), 
                true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);

        viewHolder.ivGravatar.setTag(issue);
        viewHolder.tvNumber.setText(String.valueOf(issue.getNumber()));

        viewHolder.tvState.setText(issue.getState());
        if ("closed".equals(issue.getState())) {
            viewHolder.tvState.setBackgroundResource(R.drawable.default_red_box);
        }
        else {
            viewHolder.tvState.setBackgroundResource(R.drawable.default_green_box);
        }

        viewHolder.llLabels.removeAllViews();

        //show labels
        List<Label> labels = issue.getLabels();
        if (labels != null) {
            for (Label label : labels) {
                TextView tvLabel = (TextView) inflater.inflate(R.layout.issue_list_label,
                        viewHolder.llLabels, false);
                int color = Color.parseColor("#" + label.getColor());
                boolean dark = Color.red(color) + Color.green(color) + Color.blue(color) < 383;

                tvLabel.setText(label.getName());
                tvLabel.setBackgroundColor(color);
                tvLabel.setTextColor(v.getResources().getColor(
                        dark ? android.R.color.primary_text_dark : android.R.color.primary_text_light));
                viewHolder.llLabels.addView(tvLabel);
            }
        }

        viewHolder.tvDesc.setText(issue.getTitle());
        viewHolder.tvExtra.setText(issue.getUser().getLogin() + "\n" + pt.format(issue.getCreatedAt()));
        viewHolder.tvComments.setText(String.valueOf(issue.getComments()));
        viewHolder.tvRepo.setText(mContext.getString(R.string.repo_issue_on,
                issue.getRepository().getOwner().getLogin() + "/" + issue.getRepository().getName()));

        if (issue.getAssignee() != null) {
            viewHolder.ivAssignee.setVisibility(View.VISIBLE);
            aq.id(viewHolder.ivAssignee).image(GravatarUtils.getGravatarUrl(issue.getAssignee().getGravatarId()), 
                    true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);
        }
        else {
            viewHolder.ivAssignee.setVisibility(View.GONE);
        }

        if (issue.getMilestone() != null) {
            viewHolder.tvMilestone.setVisibility(View.VISIBLE);
            viewHolder.tvMilestone.setText("Milestone : " + issue.getMilestone().getTitle());
        }
        else {
            viewHolder.tvMilestone.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            RepositoryIssue issue = (RepositoryIssue) v.getTag();
            /** Open user activity */
            Gh4Application.get(mContext).openUserInfoActivity(mContext, issue.getUser().getLogin(), null);
        }
    }
    
    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        public TextView tvNumber;
        public LinearLayout llLabels;
        public TextView tvState;
        public ImageView ivAssignee;
        public TextView tvComments;
        public TextView tvRepo;
        public TextView tvMilestone;
    }
}