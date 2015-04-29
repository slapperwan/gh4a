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
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.holder.Trend;

public class TrendAdapter extends RootAdapter<Trend> {
    public TrendAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_trend, null);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Trend trend) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        String owner = trend.getRepoOwner();
        String name = trend.getRepoName();
        if (owner != null && name != null) {
            SpannableStringBuilder title = new SpannableStringBuilder();
            title.append(owner).append("/").append(name);
            title.setSpan(new StyleSpan(Typeface.BOLD), 0, owner.length(), 0);
            viewHolder.tvTitle.setText(title);
        } else {
            viewHolder.tvTitle.setText(trend.getTitle());
        }
        viewHolder.tvDesc.setText(trend.getDescription());
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvDesc;
    }
}
