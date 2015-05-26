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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

public class PullRequestAdapter extends RootAdapter<PullRequest> implements View.OnClickListener {
    public PullRequestAdapter(Context context) {
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
        viewHolder.tvComments = (TextView) v.findViewById(R.id.tv_comments);

        v.findViewById(R.id.labels).setVisibility(View.GONE);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, PullRequest pullRequest) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();
        final User user = pullRequest.getUser();

        AvatarHandler.assignAvatar(viewHolder.ivGravatar, user);
        viewHolder.ivGravatar.setTag(pullRequest.getUser());

        viewHolder.tvNumber.setText("#" + pullRequest.getNumber());
        viewHolder.tvDesc.setText(pullRequest.getTitle());
        viewHolder.tvCreator.setText(CommitUtils.getUserLogin(mContext, user));
        viewHolder.tvTimestamp.setText(
                StringUtils.formatRelativeTime(mContext, pullRequest.getCreatedAt(), true));

        int comments = pullRequest.getComments() + pullRequest.getReviewComments();
        if (comments > 0) {
            viewHolder.tvComments.setVisibility(View.VISIBLE);
            viewHolder.tvComments.setText(String.valueOf(comments));
        } else {
            viewHolder.tvComments.setVisibility(View.GONE);
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
        }
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvCreator;
        public TextView tvTimestamp;
        public TextView tvDesc;
        public TextView tvNumber;
        public TextView tvComments;
    }
}