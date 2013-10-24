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

import org.eclipse.egit.github.core.Milestone;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

public class MilestoneAdapter extends RootAdapter<Milestone> {

    public MilestoneAdapter(Context context) {
        super(context);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_simple_3, null);

            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            Typeface regular = app.regular;
            
            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvTitle.setTypeface(boldCondensed);
            
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(regular);
            viewHolder.tvDesc.setMaxLines(2);
            viewHolder.tvDesc.setEllipsize(TruncateAt.END);
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.tvExtra.setTextAppearance(mContext, R.style.default_text_micro);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        Milestone milestone = mObjects.get(position);
        if (milestone != null) {

            if (viewHolder.tvTitle != null) {
                viewHolder.tvTitle.setText(milestone.getTitle());
            }

            if (viewHolder.tvDesc != null) {
                if (!StringUtils.isBlank(milestone.getDescription())) {
                    viewHolder.tvDesc.setVisibility(View.VISIBLE);
                    viewHolder.tvDesc.setText(milestone.getDescription());
                }
                else {
                    viewHolder.tvDesc.setVisibility(View.GONE);
                }
            }

            if (viewHolder.tvExtra != null) {
                String extraData;
                if (milestone.getDueOn() != null) {
                    extraData = mContext.getString(R.string.milestone_extradata_due,
                            milestone.getClosedIssues(), milestone.getOpenIssues(),
                            DateFormat.getMediumDateFormat(mContext).format(milestone.getDueOn()));
                } else {
                    extraData = mContext.getString(R.string.milestone_extradata,
                            milestone.getClosedIssues(), milestone.getOpenIssues());
                }

                viewHolder.tvExtra.setText(extraData);
            }
        }
        return v;
    }

    private static class ViewHolder {
        
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;
    }
}