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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.widget.LabelBadgeView;

import org.eclipse.egit.github.core.Issue;

public class RepositoryIssueAdapter extends RootAdapter<Issue> implements
        View.OnClickListener {
    public RepositoryIssueAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_issue, parent, false);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvCreator = (TextView) v.findViewById(R.id.tv_creator);
        viewHolder.tvTimestamp = (TextView) v.findViewById(R.id.tv_timestamp);
        viewHolder.tvNumber = (TextView) v.findViewById(R.id.tv_number);
        viewHolder.lvLabels = (LabelBadgeView) v.findViewById(R.id.labels);
        viewHolder.tvComments = (TextView) v.findViewById(R.id.tv_comments);
        viewHolder.tvMilestone = (TextView) v.findViewById(R.id.tv_milestone);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Issue issue) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        AvatarHandler.assignAvatar(viewHolder.ivGravatar, issue.getUser());
        viewHolder.ivGravatar.setTag(issue);

        viewHolder.lvLabels.setLabels(issue.getLabels());

        String userName = issue.getUser() != null
                ? issue.getUser().getLogin() : mContext.getString(R.string.deleted);

        viewHolder.tvDesc.setText(issue.getTitle());
        viewHolder.tvCreator.setText(userName);
        viewHolder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext,
                issue.getCreatedAt(), true));

        if (issue.getComments() > 0) {
            viewHolder.tvComments.setVisibility(View.VISIBLE);
            viewHolder.tvComments.setText(String.valueOf(issue.getComments()));
        } else {
            viewHolder.tvComments.setVisibility(View.GONE);
        }

        // https://api.github.com/repos/batterseapower/pinyin-toolkit/issues/132
        String[] urlPart = issue.getUrl().split("/");
        String repoName = urlPart[4] + "/" + urlPart[5];
        viewHolder.tvNumber.setText(mContext.getString(R.string.repo_issue_on,
                issue.getNumber(), repoName));

        if (issue.getMilestone() != null) {
            viewHolder.tvMilestone.setVisibility(View.VISIBLE);
            viewHolder.tvMilestone.setText(issue.getMilestone().getTitle());
        } else {
            viewHolder.tvMilestone.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Issue issue = (Issue) v.getTag();
            Intent intent = IntentUtils.getUserActivityIntent(mContext, issue.getUser());
            if (intent != null) {
                mContext.startActivity(intent);
            }
        }
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvNumber;
        public TextView tvDesc;
        public TextView tvCreator;
        public TextView tvTimestamp;
        public LabelBadgeView lvLabels;
        public TextView tvComments;
        public TextView tvMilestone;
    }
}
