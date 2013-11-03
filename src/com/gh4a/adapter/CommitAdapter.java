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

import org.eclipse.egit.github.core.RepositoryCommit;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.GravatarHandler;

public class CommitAdapter extends RootAdapter<RepositoryCommit> implements OnClickListener {
    public CommitAdapter(Context context) {
        super(context);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;
        if (v == null) {
            v = LayoutInflater.from(mContext).inflate(R.layout.row_commit, null);
            
            Typeface boldCondensed = Gh4Application.get(mContext).boldCondensed;
            
            viewHolder = new ViewHolder();
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(boldCondensed);
            
            viewHolder.tvSha = (TextView) v.findViewById(R.id.tv_sha);
            viewHolder.tvSha.setTypeface(Typeface.MONOSPACE);
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.ivGravatar.setOnClickListener(this);

            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final RepositoryCommit commit = mObjects.get(position);

        GravatarHandler.assignGravatar(viewHolder.ivGravatar, commit.getAuthor());
        viewHolder.ivGravatar.setTag(commit);

        viewHolder.tvSha.setText(commit.getSha().substring(0, 10));
        viewHolder.tvDesc.setText(commit.getCommit().getMessage());

        Resources res = v.getResources();
        String extraData = String.format(res.getString(R.string.more_commit_data),
                CommitUtils.getAuthorName(mContext, commit),
                Gh4Application.pt.format(commit.getCommit().getAuthor().getDate()));

        viewHolder.tvExtra.setText(extraData);

        return v;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            RepositoryCommit commit = (RepositoryCommit) v.getTag();
            /** Open user activity */
            String login = CommitUtils.getAuthorLogin(mContext, commit);
            if (login != null) {
                Gh4Application.get(mContext).openUserInfoActivity(mContext, login, null);
            }
        }
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        public TextView tvSha;
    }
}