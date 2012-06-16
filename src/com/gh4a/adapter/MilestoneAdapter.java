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

import org.eclipse.egit.github.core.Milestone;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.StringUtils;

/**
 * The Repository adapter.
 */
public class MilestoneAdapter extends RootAdapter<Milestone> {

    /** The row layout. */
    protected int mRowLayout;

    /**
     * Instantiates a new repository adapter.
     * 
     * @param context the context
     * @param objects the objects
     */
    public MilestoneAdapter(Context context, List<Milestone> objects) {
        super(context, objects);
    }

    /**
     * Instantiates a new repository adapter.
     * 
     * @param context the context
     * @param objects the objects
     * @param rowLayout the row layout
     */
    public MilestoneAdapter(Context context, List<Milestone> objects, int rowLayout) {
        super(context, objects);
        mRowLayout = rowLayout;
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
            v = vi.inflate(R.layout.row_simple_3, null);

            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
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
                String extraData = "State " + milestone.getState() + " | " + milestone.getClosedIssues() + " closed"
                        + " | " + milestone.getOpenIssues() + " open ";
                viewHolder.tvExtra.setText(extraData);
            }
        }
        return v;
    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {
        
        /** The tv title. */
        public TextView tvTitle;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvExtra;
    }
}