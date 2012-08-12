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

import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.holder.Feed;
import com.gh4a.utils.GravatarUtils;

public class CommonFeedAdapter extends RootAdapter<Feed> {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
    private boolean mShowGravatar;
    private boolean mShowExtra;
    private int mRowLayout;
    private AQuery aq;
    
    public CommonFeedAdapter(Context context, List<Feed> objects) {
        super(context, objects);
        mShowGravatar = true;//default true
        mShowExtra = true;//default true
        aq = new AQuery((BaseSherlockFragmentActivity) context);
    }
    
    public CommonFeedAdapter(Context context, List<Feed> objects, boolean showGravatar, boolean showExtra) {
        super(context, objects);
        mShowGravatar = showGravatar;
        mShowExtra = showExtra;
        aq = new AQuery(context);
    }
    
    public CommonFeedAdapter(Context context, List<Feed> objects, boolean showGravatar, boolean showExtra, int rowLayout) {
        super(context, objects);
        mShowGravatar = showGravatar;
        mShowExtra = showExtra;
        mRowLayout = rowLayout;
        aq = new AQuery(context);
    }
    
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            if (mRowLayout != 0) {
                v = vi.inflate(mRowLayout, null);
            }
            else {
                v = vi.inflate(R.layout.row_gravatar_3, null);
            }
            
            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            Typeface regular = app.regular;
            
            viewHolder = new ViewHolder();
            
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            
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
        if (feed != null) {
            
            aq.recycle(convertView);
            
            if (viewHolder.ivGravatar != null) {
                if (mShowGravatar) {
                    
                    aq.id(viewHolder.ivGravatar).image(GravatarUtils.getGravatarUrl(feed.getGravatarId()), 
                            true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);
                    viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {
        
                        @Override
                        public void onClick(View v) {
                            /** Open user activity */
                            Gh4Application context = (Gh4Application) v.getContext()
                                    .getApplicationContext();
                            context.openUserInfoActivity(v.getContext(), feed.getAuthor(), null);
                        }
                    });
                    viewHolder.ivGravatar.setVisibility(View.VISIBLE);
                }
                else {
                    viewHolder.ivGravatar.setVisibility(View.GONE);
                }
            }
            
            viewHolder.tvTitle.setText(feed.getTitle());

            viewHolder.tvDesc.setText(feed.getContent().replaceAll("<(.|\n)*?>",""));
            viewHolder.tvDesc.setSingleLine(true);
            
            if (mShowExtra) {
                viewHolder.tvExtra.setText(feed.getAuthor() 
                        +  (feed.getPublished() != null ? " | " + sdf.format(feed.getPublished()) : ""));
                viewHolder.tvExtra.setVisibility(View.VISIBLE);
            }
            else {
                viewHolder.tvExtra.setVisibility(View.GONE);
            }
        }
        return v;
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;

    }
}
