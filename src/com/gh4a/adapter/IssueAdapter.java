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
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.Issue;

/**
 * The Issue adapter.
 */
public class IssueAdapter extends RootAdapter<Issue> {

    /**
     * Instantiates a new issue adapter.
     * 
     * @param context the context
     * @param objects the objects
     */
    public IssueAdapter(Context context, List<Issue> objects) {
        super(context, objects);
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.adapter.RootAdapter#doGetView(int, android.view.View,
     * android.view.ViewGroup)
     */
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_gravatar_2, null);
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final Issue issue = mObjects.get(position);
        if (issue != null) {
            ImageDownloader.getInstance().download(issue.getGravatarId(), viewHolder.ivGravatar);
            viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    Gh4Application context = (Gh4Application) v.getContext()
                            .getApplicationContext();
                    context.openUserInfoActivity(v.getContext(), issue.getUser(), null);
                }
            });

            viewHolder.tvDesc.setText(StringUtils.doTeaser(issue.getTitle()));

            Resources res = v.getResources();
            String extraData = String.format(res.getString(R.string.more_data_3), issue.getUser(),
                    pt.format(issue.getCreatedAt()), issue.getComments() + " "
                            + res.getQuantityString(R.plurals.issue_comment, issue.getComments()));

            viewHolder.tvExtra.setText(extraData);
        }
        return v;
    }

    /**
     * Adds the object.
     * 
     * @param issue the issue
     */
    public void add(Issue issue) {
        mObjects.add(issue);
    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {

        /** The iv gravatar. */
        public ImageView ivGravatar;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvExtra;

    }
}