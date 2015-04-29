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
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.holder.Feed;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;

public class CommonFeedAdapter extends RootAdapter<Feed> implements View.OnClickListener {
    private boolean mShowExtra;

    public CommonFeedAdapter(Context context, boolean showExtra) {
        super(context);
        mShowExtra = showExtra;
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_gravatar_3, parent, false);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.tvTimestamp = (TextView) v.findViewById(R.id.tv_timestamp);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Feed feed) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        String title = feed.getTitle();
        viewHolder.tvTitle.setText(title);
        viewHolder.tvTitle.setVisibility(title != null ? View.VISIBLE : View.GONE);

        viewHolder.tvDesc.setText(feed.getPreview());
        viewHolder.tvDesc.setGravity(mShowExtra ? Gravity.TOP : Gravity.CENTER_VERTICAL);

        if (mShowExtra && feed.getUserId() > 0) {
            AvatarHandler.assignAvatar(viewHolder.ivGravatar,
                    feed.getUserId(), feed.getAvatarUrl());
            viewHolder.ivGravatar.setTag(feed);
            viewHolder.ivGravatar.setVisibility(View.VISIBLE);
        } else {
            viewHolder.ivGravatar.setVisibility(View.GONE);
        }

        if (mShowExtra) {
            String published = feed.getPublished() != null
                    ? DateFormat.getMediumDateFormat(mContext).format(feed.getPublished()) : "";
            viewHolder.tvExtra.setText(feed.getAuthor());
            viewHolder.tvTimestamp.setText(published);
            viewHolder.tvExtra.setVisibility(View.VISIBLE);
            viewHolder.tvTimestamp.setVisibility(View.VISIBLE);
        } else {
            viewHolder.tvExtra.setVisibility(View.GONE);
            viewHolder.tvTimestamp.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Feed feed = (Feed) v.getTag();
            Intent intent = IntentUtils.getUserActivityIntent(mContext, feed.getAuthor());
            if (intent != null) {
                mContext.startActivity(intent);
            }
        }
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;
        public TextView tvTimestamp;
    }
}
