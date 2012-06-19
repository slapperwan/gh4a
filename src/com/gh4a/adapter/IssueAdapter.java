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
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;

/**
 * The Issue adapter.
 */
public class IssueAdapter extends RootAdapter<Issue> {

    private int mRowLayout;
    /**
     * Instantiates a new issue adapter.
     * 
     * @param context the context
     * @param objects the objects
     */
    public IssueAdapter(Context context, List<Issue> objects) {
        super(context, objects);
    }
    
    public IssueAdapter(Context context, List<Issue> objects, int rowLayout) {
        super(context, objects);
        mRowLayout = rowLayout;
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.adapter.RootAdapter#doGetView(int, android.view.View,
     * android.view.ViewGroup)
     */
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(mRowLayout, null);
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.llLabels = (LinearLayout) v.findViewById(R.id.ll_labels);
            viewHolder.tvState = (TextView) v.findViewById(R.id.tv_state);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final Issue issue = mObjects.get(position);
        if (issue != null) {
            ImageDownloader.getInstance().download(issue.getUser().getGravatarId(), viewHolder.ivGravatar);
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
            
            //issue number
//            TextView tvNumber = new TextView(v.getContext());
//            tvNumber.setSingleLine(true);
//            tvNumber.setText("#" + issue.getNumber());
//            tvNumber.setTextAppearance(v.getContext(), R.style.default_text_small);
//            tvNumber.setBackgroundResource(R.drawable.default_grey_box);
//            
//            viewHolder.llLabels.addView(tvNumber);

            //show assignees
            if (issue.getAssignee() != null) {
                TextView tvLabel = new TextView(v.getContext());
                tvLabel.setSingleLine(true);
                tvLabel.setText("Assignee " + issue.getAssignee().getLogin() + " ");
                tvLabel.setTextAppearance(v.getContext(), R.style.default_text_small_url);
                tvLabel.setOnClickListener(new OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        Gh4Application context = (Gh4Application) v.getContext()
                                .getApplicationContext();
                        context.openUserInfoActivity(v.getContext(), 
                                issue.getAssignee().getLogin(), null);
                    }
                });
                
                viewHolder.llLabels.addView(tvLabel);
                viewHolder.llLabels.setVisibility(View.VISIBLE);
            }
            
            //show labels
            List<Label> labels = issue.getLabels();
            if (labels != null && !labels.isEmpty()) {
                for (Label label : labels) {
                    TextView tvLabel = new TextView(v.getContext());
                    tvLabel.setSingleLine(true);
                    tvLabel.setText(label.getName());
                    tvLabel.setTextAppearance(v.getContext(), R.style.default_text_small);
                    tvLabel.setBackgroundResource(R.drawable.default_grey_box);
                    
                    viewHolder.llLabels.addView(tvLabel);
                }
                viewHolder.llLabels.setVisibility(View.VISIBLE);
            }
            else {
                viewHolder.llLabels.setVisibility(View.GONE);
            }
            //viewHolder.llLabels.setVisibility(View.VISIBLE);
            
            viewHolder.tvDesc.setText("#" + issue.getNumber() + " - " + StringUtils.doTeaser(issue.getTitle()));

            Resources res = v.getResources();
            String extraData = res.getString(R.string.more_data_3, 
                    "by " + issue.getUser().getLogin(),
                    pt.format(issue.getCreatedAt()), issue.getComments() + " "
                            + res.getQuantityString(R.plurals.issue_comment, issue.getComments()));

            viewHolder.tvExtra.setText(extraData);
        }
        return v;
    }

    /**
     * Adds the object.
     * 
     * @param issue the issue
     */
    public void add(Issue issue) {
        mObjects.add(issue);
    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {

        /** The iv gravatar. */
        public ImageView ivGravatar;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvExtra;
        
        /** The ll labels. */
        public LinearLayout llLabels;
        
        /** The tv state. */
        public TextView tvState;

    }
}