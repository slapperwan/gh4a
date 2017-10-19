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
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.model.Feed;
import com.gh4a.utils.AvatarHandler;

public class CommonFeedAdapter extends RootAdapter<Feed, CommonFeedAdapter.ViewHolder> {
    private final boolean mShowExtra;

    public CommonFeedAdapter(Context context, boolean showExtra) {
        super(context);
        mShowExtra = showExtra;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_feed, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Feed feed) {
        String title = feed.getTitle();
        holder.tvTitle.setText(title);
        holder.tvTitle.setVisibility(title != null ? View.VISIBLE : View.GONE);

        holder.tvDesc.setText(feed.getPreview());
        holder.tvDesc.setGravity(mShowExtra ? Gravity.TOP : Gravity.CENTER_VERTICAL);

        if (mShowExtra && feed.getUserId() > 0) {
            AvatarHandler.assignAvatar(holder.ivGravatar,
                    feed.getAuthor(), feed.getUserId(), feed.getAvatarUrl());
            holder.ivGravatar.setTag(feed);
            holder.ivGravatar.setVisibility(View.VISIBLE);
        } else {
            holder.ivGravatar.setVisibility(View.GONE);
        }

        if (mShowExtra) {
            String published = feed.getPublished() != null
                    ? DateFormat.getMediumDateFormat(mContext).format(feed.getPublished()) : "";
            holder.tvExtra.setText(feed.getAuthor());
            holder.tvTimestamp.setText(published);
            holder.tvExtra.setVisibility(View.VISIBLE);
            holder.tvTimestamp.setVisibility(View.VISIBLE);
        } else {
            holder.tvExtra.setVisibility(View.GONE);
            holder.tvTimestamp.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Feed feed = (Feed) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext, feed.getAuthor());
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
            ivGravatar = view.findViewById(R.id.iv_gravatar);
            tvTitle = view.findViewById(R.id.tv_title);
            tvDesc = view.findViewById(R.id.tv_desc);
            tvExtra = view.findViewById(R.id.tv_extra);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
        }

        private final ImageView ivGravatar;
        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvExtra;
        private final TextView tvTimestamp;
    }
}
