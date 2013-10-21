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

import android.content.Context;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.holder.Feed;

public class DiscussionCommentFeedAdapter extends RootAdapter<Feed> {

    public DiscussionCommentFeedAdapter(Context context, List<Feed> objects) {
        super(context, objects);
    }
    
    
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_gravatar_comment, null);
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final Feed feed = mObjects.get(position);
        if (feed != null) {
            viewHolder.ivGravatar.setVisibility(View.GONE);
            viewHolder.tvDesc.setText(Html.fromHtml(feed.getContent()));
            
            viewHolder.tvExtra.setText(feed.getAuthor() 
                    +  (feed.getPublished() != null ? " | "
                    + DateFormat.getMediumDateFormat(mContext).format(feed.getPublished()) : ""));
        }
        return v;
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;

    }
}
