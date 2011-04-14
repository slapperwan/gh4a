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
import android.graphics.Color;
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
import com.github.api.v2.schema.Job;

public class JobAdapter extends RootAdapter<Job> {

    public JobAdapter(Context context, List<Job> objects) {
        super(context, objects);
    }
    
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_job, null);
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final Job job = mObjects.get(position);
        if (job != null) {
            if (!StringUtils.isBlank(job.getCompanyLogo())) {
                ImageDownloader.getInstance().downloadByUrl(job.getCompanyLogo(), viewHolder.ivGravatar);
                if (!StringUtils.isBlank(job.getCompanyUrl())) {
                    viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {
        
                        @Override
                        public void onClick(View v) {
                            /** Open user activity */
                            Gh4Application context = (Gh4Application) v.getContext()
                                    .getApplicationContext();
                            context.openBrowser(v.getContext(), job.getCompanyUrl());
                        }
                    });
                }
            }
            else {
                viewHolder.ivGravatar.setImageDrawable(null);
            }
            
            viewHolder.tvTitle.setText(job.getTitle());
            viewHolder.tvDesc.setText(job.getCompany());
            viewHolder.tvExtra.setText(job.getType().value() + " | " + job.getLocation());
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
