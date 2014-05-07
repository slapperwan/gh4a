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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.GravatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;

import java.util.List;

public class IssueAdapter extends RootAdapter<Issue> implements OnClickListener {
    public IssueAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_issue, null);
        ViewHolder viewHolder = new ViewHolder();

        Gh4Application app = (Gh4Application) mContext.getApplicationContext();
        Typeface boldCondensed = app.boldCondensed;
        Typeface regular = app.regular;

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvDesc.setTypeface(boldCondensed);

        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.tvExtra.setTypeface(regular);

        viewHolder.llLabels = (LinearLayout) v.findViewById(R.id.ll_labels);
        viewHolder.tvNumber = (TextView) v.findViewById(R.id.tv_number);
        viewHolder.ivAssignee = (ImageView) v.findViewById(R.id.iv_assignee);
        viewHolder.tvComments = (TextView) v.findViewById(R.id.tv_comments);
        viewHolder.tvMilestone = (TextView) v.findViewById(R.id.tv_milestone);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Issue issue) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        GravatarHandler.assignGravatar(viewHolder.ivGravatar, issue.getUser());
        viewHolder.ivGravatar.setTag(issue);

        viewHolder.tvNumber.setText(String.valueOf(issue.getNumber()));

        makeLabelBadges(viewHolder.llLabels, issue.getLabels());

        viewHolder.tvDesc.setText(issue.getTitle());

        viewHolder.tvExtra.setText(issue.getUser().getLogin() + "\n"
                + StringUtils.formatRelativeTime(mContext, issue.getCreatedAt(), false));
        if (issue.getAssignee() != null) {
            viewHolder.ivAssignee.setVisibility(View.VISIBLE);
            GravatarHandler.assignGravatar(viewHolder.ivAssignee, issue.getAssignee());
        } else {
            viewHolder.ivAssignee.setVisibility(View.GONE);
        }

        if (issue.getComments() > 0) {
            viewHolder.tvComments.setText(String.valueOf(issue.getComments()));
            int drawableId = Gh4Application.THEME == R.style.DefaultTheme ? R.drawable.comments_dark : R.drawable.comments;
            Drawable commentDrawable = v.getContext().getResources().getDrawable(drawableId);
            viewHolder.tvComments.setCompoundDrawablesWithIntrinsicBounds(commentDrawable, null, null, null);
        } else{
            viewHolder.tvComments.setCompoundDrawables(null,null,null,null);
            viewHolder.tvComments.setText("");
        }

        if (issue.getMilestone() != null) {
            viewHolder.tvMilestone.setVisibility(View.VISIBLE);
            viewHolder.tvMilestone.setText(mContext.getString(R.string.issue_milestone,
                    issue.getMilestone().getTitle()));
        } else {
            viewHolder.tvMilestone.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Issue issue = (Issue) v.getTag();
            IntentUtils.openUserInfoActivity(mContext, issue.getUser());
        }
    }

    /* package */ static void makeLabelBadges(LinearLayout badgeLayout, List<Label> labels) {
        if (labels != null) {
            int viewCount = badgeLayout.getChildCount();
            int labelCount = labels.size();

            for (int i = 0; i < labelCount; i++) {
                View badge;
                if (i < viewCount) {
                    badge = badgeLayout.getChildAt(i);
                } else {
                    Context context = badgeLayout.getContext();
                    int height = context.getResources().getDimensionPixelSize(R.dimen.label_badge_size);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, height);
                    params.topMargin = context.getResources().getDimensionPixelSize(R.dimen.label_badge_spacing);

                    badge = new View(context);
                    badgeLayout.addView(badge, params);
                }

                badge.setBackgroundColor(Color.parseColor("#" + labels.get(i).getColor()));
                badge.setVisibility(View.VISIBLE);
            }
            for (int i = labelCount; i < viewCount; i++) {
                badgeLayout.getChildAt(i).setVisibility(View.GONE);
            }
        } else {
            badgeLayout.setVisibility(View.GONE);
        }
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        public LinearLayout llLabels;
        public TextView tvNumber;
        public ImageView ivAssignee;
        public TextView tvComments;
        public TextView tvMilestone;
    }
}