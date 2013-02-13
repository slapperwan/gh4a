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

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;

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
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.GravatarUtils;

public class IssueAdapter extends RootAdapter<Issue> {

    private AQuery aq;
    
    public IssueAdapter(Context context, List<Issue> objects) {
        super(context, objects);
        aq = new AQuery((BaseSherlockFragmentActivity) context);
    }
    
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_issue, null);
            
            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            Typeface regular = app.regular;
            
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(boldCondensed);
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.tvExtra.setTypeface(regular);
            
            viewHolder.llLabels = (LinearLayout) v.findViewById(R.id.ll_labels);
            
            viewHolder.tvState = (TextView) v.findViewById(R.id.tv_state);
            
            viewHolder.ivAssignee = (ImageView) v.findViewById(R.id.iv_assignee);
            
            viewHolder.tvComments = (TextView) v.findViewById(R.id.tv_comments);
            
            viewHolder.tvMilestone = (TextView) v.findViewById(R.id.tv_milestone);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final Issue issue = mObjects.get(position);
        if (issue != null) {
            
            aq.recycle(convertView);
            aq.id(viewHolder.ivGravatar).image(GravatarUtils.getGravatarUrl(issue.getUser().getGravatarId()), 
                    true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);
            
            viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    Gh4Application context = (Gh4Application) v.getContext()
                            .getApplicationContext();
                    context.openUserInfoActivity(v.getContext(), issue.getUser().getLogin(), null);
                }
            });
            
            if (viewHolder.tvState != null) {
                viewHolder.tvState.setText(issue.getState());
                if ("closed".equals(issue.getState())) {
                    viewHolder.tvState.setBackgroundResource(R.drawable.default_red_box);
                }
                else {
                    viewHolder.tvState.setBackgroundResource(R.drawable.default_green_box);
                }
            }
            
            viewHolder.llLabels.removeAllViews();
            
            TextView tvNumber = new TextView(v.getContext());
            tvNumber.setSingleLine(true);
            tvNumber.setText(String.valueOf(issue.getNumber()));
            tvNumber.setTypeface(null, Typeface.BOLD);
            tvNumber.setTextColor(Color.BLACK);
            tvNumber.setBackgroundColor(Color.LTGRAY);
            tvNumber.setPadding(2, 2, 2, 2);
            viewHolder.llLabels.addView(tvNumber);
            
            //show labels
            List<Label> labels = issue.getLabels();
            if (labels != null && !labels.isEmpty()) {
                for (Label label : labels) {
                    TextView tvLabel = new TextView(v.getContext());
                    tvLabel.setSingleLine(true);
                    tvLabel.setText(label.getName());
                    tvLabel.setTextSize(10);
                    tvLabel.setBackgroundColor(Color.parseColor("#" + label.getColor()));
                    tvLabel.setPadding(2, 2, 2, 2);
                    int r = Color.red(Color.parseColor("#" + label.getColor()));
                    int g = Color.green(Color.parseColor("#" + label.getColor()));
                    int b = Color.blue(Color.parseColor("#" + label.getColor()));
                    if (r + g + b < 383) {
                        tvLabel.setTextColor(v.getResources().getColor(android.R.color.primary_text_dark));
                    }
                    else {
                        tvLabel.setTextColor(v.getResources().getColor(android.R.color.primary_text_light));
                    }
                    viewHolder.llLabels.addView(tvLabel);
                }
            }
            
            viewHolder.tvDesc.setText(issue.getTitle());

            viewHolder.tvExtra.setText(issue.getUser().getLogin() + "\n" + pt.format(issue.getCreatedAt()));
            if (issue.getAssignee() != null) {
                viewHolder.ivAssignee.setVisibility(View.VISIBLE);
                aq.id(viewHolder.ivAssignee).image(GravatarUtils.getGravatarUrl(issue.getAssignee().getGravatarId()),
                        true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), AQuery.FADE_IN);
            }
            else {
                viewHolder.ivAssignee.setVisibility(View.GONE);
            }
            
            viewHolder.tvComments.setText(String.valueOf(issue.getComments()));
            
            if (issue.getMilestone() != null) {
                viewHolder.tvMilestone.setVisibility(View.VISIBLE);
                viewHolder.tvMilestone.setText("Milestone : " + issue.getMilestone().getTitle());
            }
            else {
                viewHolder.tvMilestone.setVisibility(View.GONE);
            }
        }
        return v;
    }

    public void add(Issue issue) {
        mObjects.add(issue);
    }

    private static class ViewHolder {

        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        public LinearLayout llLabels;
        public TextView tvState;
        public ImageView ivAssignee;
        public TextView tvComments;
        public TextView tvMilestone;
    }
}