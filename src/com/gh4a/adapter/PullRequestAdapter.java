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

import org.eclipse.egit.github.core.PullRequest;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.GravatarUtils;

public class PullRequestAdapter extends RootAdapter<PullRequest> {

    public PullRequestAdapter(Context context, List<PullRequest> objects) {
        super(context, objects);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_gravatar_2, null);
            
            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            Typeface regular = app.regular;
            
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(boldCondensed);
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.tvExtra.setTypeface(regular);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final PullRequest pullRequest = mObjects.get(position);
        if (pullRequest != null) {
            AQuery aq = new AQuery(convertView);
            if (pullRequest.getUser() != null) {
                aq.id(viewHolder.ivGravatar).image(GravatarUtils.getGravatarUrl(pullRequest.getUser().getGravatarId()), 
                        true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);
                
                viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {
    
                    @Override
                    public void onClick(View v) {
                        /** Open user activity */
                        Gh4Application context = (Gh4Application) v.getContext()
                                .getApplicationContext();
                        context.openUserInfoActivity(v.getContext(), pullRequest.getUser()
                                .getLogin(), null);
                    }
                });
            }
            else {
                aq.id(viewHolder.ivGravatar).image(R.drawable.default_avatar);
            }

            viewHolder.tvDesc.setText(pullRequest.getTitle());
            Resources res = v.getResources();
            
            String extraData = res.getString(R.string.more_issue_data, 
                    pullRequest.getUser() != null ? pullRequest.getUser().getLogin() : "",
                    pt.format(pullRequest.getCreatedAt()));
            
            viewHolder.tvExtra.setText(extraData);
        }
        return v;
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
    }
}
