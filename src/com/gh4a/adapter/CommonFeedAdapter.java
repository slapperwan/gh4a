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
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.holder.Feed;
import com.gh4a.utils.GravatarUtils;

public class CommonFeedAdapter extends RootAdapter<Feed> implements OnClickListener {
    private boolean mShowGravatar;
    private boolean mShowExtra;
    private int mRowLayout;
    private AQuery aq;
    
    public CommonFeedAdapter(Context context) {
        super(context);
        mShowGravatar = true;//default true
        mShowExtra = true;//default true
        aq = new AQuery(context);
    }
    
    public CommonFeedAdapter(Context context, boolean showGravatar, boolean showExtra, int rowLayout) {
        this(context);
        mShowGravatar = showGravatar;
        mShowExtra = showExtra;
        mRowLayout = rowLayout;
    }
    
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            int layoutId = mRowLayout != 0 ? mRowLayout : R.layout.row_gravatar_3;
            v = LayoutInflater.from(mContext).inflate(layoutId, parent, false);
            
            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            Typeface regular = app.regular;
            
            viewHolder = new ViewHolder();
            
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            if (viewHolder.ivGravatar != null) {
                viewHolder.ivGravatar.setOnClickListener(this);
            }
            
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvTitle.setTypeface(boldCondensed);
            
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(regular);
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.tvExtra.setTextAppearance(mContext, R.style.default_text_micro);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final Feed feed = mObjects.get(position);
        
        aq.recycle(v);

        if (viewHolder.ivGravatar != null) {
            if (mShowGravatar) {
                aq.id(viewHolder.ivGravatar).image(GravatarUtils.getGravatarUrl(feed.getGravatarId()), 
                        true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);
                viewHolder.ivGravatar.setTag(feed);
                viewHolder.ivGravatar.setVisibility(View.VISIBLE);
            }
            else {
                viewHolder.ivGravatar.setVisibility(View.GONE);
            }
        }

        String title = feed.getTitle();
        viewHolder.tvTitle.setText(title);
        viewHolder.tvTitle.setVisibility(title != null ? View.VISIBLE : View.GONE);

        viewHolder.tvDesc.setText(feed.getPreview());
        viewHolder.tvDesc.setSingleLine(title != null || mShowExtra);

        if (mShowExtra) {
            String published = feed.getPublished() != null
                    ? DateFormat.getMediumDateFormat(mContext).format(feed.getPublished()) : null;
                    viewHolder.tvExtra.setText(feed.getAuthor() + (published != null ? " | " + published : ""));
                    viewHolder.tvExtra.setVisibility(View.VISIBLE);
        }
        else {
            viewHolder.tvExtra.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Feed feed = (Feed) v.getTag();
            /** Open user activity */
            Gh4Application.get(mContext).openUserInfoActivity(mContext, feed.getAuthor(), null);
        }
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;

    }
}
