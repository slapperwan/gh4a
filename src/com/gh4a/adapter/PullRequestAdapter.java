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
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

public class PullRequestAdapter extends RootAdapter<PullRequest> implements View.OnClickListener {
    public PullRequestAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_gravatar_1, parent, false);
        ViewHolder viewHolder = new ViewHolder();

        Gh4Application app = (Gh4Application) mContext.getApplicationContext();
        Typeface boldCondensed = app.boldCondensed;
        Typeface regular = app.regular;

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvDesc.setTypeface(boldCondensed);

        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.tvExtra.setTypeface(regular);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, PullRequest pullRequest) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();
        final User user = pullRequest.getUser();

        AvatarHandler.assignAvatar(viewHolder.ivGravatar, user);
        viewHolder.ivGravatar.setTag(pullRequest.getUser());

        viewHolder.tvDesc.setText(pullRequest.getTitle());
        viewHolder.tvExtra.setText(mContext.getString(R.string.more_issue_data,
                user != null ? user.getLogin() : "",
                StringUtils.formatRelativeTime(mContext, pullRequest.getCreatedAt(), true)));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            User user = (User) v.getTag();
            IntentUtils.openUserInfoActivity(mContext, user);
        }
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
    }
}