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
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.git.GitUser;
import com.vdurmont.emoji.EmojiParser;

public class CommitAdapter extends RootAdapter<Commit, CommitAdapter.ViewHolder> {
    public CommitAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_commit, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Commit commit) {
        User author = commit.author();
        if (author != null) {
            AvatarHandler.assignAvatar(holder.ivGravatar, author);
        } else {
            GitUser commitAuthor = commit.commit().author();
            String email = commitAuthor != null ? commitAuthor.email() : null;
            holder.ivGravatar.setImageDrawable(new AvatarHandler.DefaultAvatarDrawable(null, email));
        }
        holder.ivGravatar.setTag(commit);

        String message = commit.commit().message();
        int pos = message.indexOf('\n');
        if (pos > 0) {
            message = message.substring(0, pos);
        }
        message = EmojiParser.parseToUnicode(message);

        holder.tvDesc.setText(message);
        holder.tvSha.setText(commit.sha().substring(0, 10));
        holder.ivDescriptionIndicator.setVisibility(pos > 0 ? View.VISIBLE : View.GONE);

        int comments = commit.commit().commentCount();
        if (comments > 0) {
            holder.tvComments.setText(String.valueOf(comments));
            holder.tvComments.setVisibility(View.VISIBLE);
        } else {
            holder.tvComments.setVisibility(View.GONE);
        }

        holder.tvExtra.setText(ApiHelpers.getAuthorName(mContext, commit));
        holder.tvTimestamp.setText(
                StringUtils.formatRelativeTime(mContext, commit.commit().author().date(), false));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Commit commit = (Commit) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext, ApiHelpers.getAuthorLogin(commit));
            if (intent != null) {
                mContext.startActivity(intent);
            }
        } else {
            super.onClick(v);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            tvSha = view.findViewById(R.id.tv_sha);
            tvSha.setTypeface(Typeface.MONOSPACE);

            tvDesc = view.findViewById(R.id.tv_desc);
            tvExtra = view.findViewById(R.id.tv_extra);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvComments = view.findViewById(R.id.tv_comments);

            ivGravatar = view.findViewById(R.id.iv_gravatar);
            ivDescriptionIndicator = view.findViewById(R.id.iv_description_indicator);
        }

        private final ImageView ivGravatar;
        private final TextView tvDesc;
        private final TextView tvExtra;
        private final TextView tvTimestamp;
        private final TextView tvSha;
        private final TextView tvComments;
        private final ImageView ivDescriptionIndicator;
    }
}