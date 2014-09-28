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
        View v = inflater.inflate(R.layout.row_simple_3, null);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Trend trend) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        String[] repo = trend.getRepo();
        if (repo != null) {
            viewHolder.tvTitle.setText(repo[0] + "/" + repo[1]);
        } else {
            viewHolder.tvTitle.setText(trend.getTitle());
        }
        viewHolder.tvDesc.setText(trend.getDescription());
        viewHolder.tvExtra.setVisibility(View.GONE);
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;
    }
}
