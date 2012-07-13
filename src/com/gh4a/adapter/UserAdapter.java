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

import org.eclipse.egit.github.core.User;

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
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;

public class UserAdapter extends RootAdapter<User> {

    private int mRowLayout;
    private boolean mShowExtraData;
    
    public UserAdapter(Context context, List<User> objects) {
        super(context, objects);
    }

    public UserAdapter(Context context, List<User> objects, int rowLayout, boolean showExtraData) {
        super(context, objects);
        mRowLayout = rowLayout;
        mShowExtraData = showExtraData;
    }
    
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;
        Gh4Application app = (Gh4Application) mContext.getApplicationContext();
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(mRowLayout, null);

            
            Typeface boldCondensed = app.boldCondensed;
            Typeface regular = app.regular;
            Typeface italic = app.italic;
            
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvTitle.setTypeface(boldCondensed);
            
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            if (viewHolder.tvDesc != null) {
                viewHolder.tvDesc.setTypeface(regular);
            }
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            if (viewHolder.tvExtra != null) {
                viewHolder.tvExtra.setTypeface(italic);
            }
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final User user = mObjects.get(position);

        if (user != null) {

            if (viewHolder.ivGravatar != null) {
                if (!StringUtils.isBlank(user.getGravatarId())) {
                    ImageDownloader.getInstance().download(user.getGravatarId(), viewHolder.ivGravatar);    
                }
                else if (!StringUtils.isBlank(user.getEmail())) {
                    ImageDownloader.getInstance().download(StringUtils.md5Hex(user.getEmail()), viewHolder.ivGravatar);
                }
                else if (!StringUtils.isBlank(user.getAvatarUrl())) { 
                    ImageDownloader.getInstance().downloadByUrl(user.getAvatarUrl(), viewHolder.ivGravatar);
                }
                else {
                    ImageDownloader.getInstance().download(null, viewHolder.ivGravatar);
                }
                
                viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        /** Open user activity */
                        if (!StringUtils.isBlank(user.getLogin())) {
                            Gh4Application context = (Gh4Application) v.getContext()
                                    .getApplicationContext();
                            context.openUserInfoActivity(v.getContext(), user.getLogin(), user
                                    .getLogin());
                        }
                    }
                });
            }

            if (viewHolder.tvTitle != null) {
                viewHolder.tvTitle.setText(StringUtils.formatName(user.getLogin(), user.getName()));
            }
            
            if (viewHolder.tvDesc != null) {
                viewHolder.tvDesc.setText(StringUtils.formatName(user.getLogin(), user.getName()));
            }

            if (mShowExtraData && viewHolder.tvExtra != null) {
                Resources res = v.getResources();
                String extraData = String.format(res.getString(R.string.user_extra_data), user
                            .getFollowers(), user.getPublicRepos());
                viewHolder.tvExtra.setText(extraData);
            }
        }
        return v;
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
    }

}