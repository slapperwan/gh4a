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
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.model.Trend;
import com.vdurmont.emoji.EmojiParser;

public class TrendAdapter extends RootAdapter<Trend, TrendAdapter.ViewHolder> {
    private final @StringRes int mStarsTemplate;

    public TrendAdapter(Context context, @StringRes int starsTemplate) {
        super(context);
        mStarsTemplate = starsTemplate;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_trend, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Trend trend) {
        String owner = trend.getRepoOwner();
        String name = trend.getRepoName();

        SpannableStringBuilder title = new SpannableStringBuilder();
        title.append(owner).append("/").append(name);
        title.setSpan(new StyleSpan(Typeface.BOLD), 0, owner.length(), 0);
        holder.tvTitle.setText(title);

        String desc = trend.getDescription();
        holder.tvDesc.setText(desc != null ? EmojiParser.parseToUnicode(desc) : null);

        holder.tvStars.setText(mContext.getString(mStarsTemplate,
                trend.getNewStars(), trend.getStars()));
        holder.tvForks.setText(String.valueOf(trend.getForks()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_title);
            tvDesc = view.findViewById(R.id.tv_desc);
            tvStars = view.findViewById(R.id.tv_stars);
            tvForks = view.findViewById(R.id.tv_forks);
        }

        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvStars;
        private final TextView tvForks;
    }
}
