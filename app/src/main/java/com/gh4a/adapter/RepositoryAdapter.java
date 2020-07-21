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
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.Repository;
import com.vdurmont.emoji.EmojiParser;

import java.util.Locale;

public class RepositoryAdapter extends RootAdapter<Repository, RepositoryAdapter.ViewHolder>
        implements Filterable {
    public RepositoryAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_repo, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Repository repository) {
        holder.tvTitle.setText(ApiHelpers.formatRepoName(mContext, repository));

        if (!StringUtils.isBlank(repository.description())) {
            holder.tvDesc.setVisibility(View.VISIBLE);
            holder.tvDesc.setText(
                    EmojiParser.parseToUnicode(StringUtils.doTeaser(repository.description())));
        } else {
            holder.tvDesc.setVisibility(View.GONE);
        }

        holder.tvLanguage.setText(repository.language() != null
                ? repository.language() : mContext.getString(R.string.unknown));
        holder.tvForks.setText(String.valueOf(repository.forksCount()));
        holder.tvStars.setText(String.valueOf(repository.stargazersCount()));
        holder.tvSize.setText(Formatter.formatFileSize(mContext, 1024L * repository.size()));
        holder.tvPrivate.setVisibility(repository.isPrivate() ? View.VISIBLE : View.GONE);
        holder.tvFork.setVisibility(repository.isFork() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected boolean isFiltered(CharSequence filter, Repository repo) {
        String lcFilter = filter.toString().toLowerCase(Locale.getDefault());
        String name = repo.name().toLowerCase(Locale.getDefault());
        return name.contains(lcFilter);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnTouchListener {
        private ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_title);
            tvDesc = view.findViewById(R.id.tv_desc);
            tvLanguage = view.findViewById(R.id.tv_language);
            tvForks = view.findViewById(R.id.tv_forks);
            tvStars = view.findViewById(R.id.tv_stars);
            tvSize = view.findViewById(R.id.tv_size);
            tvPrivate = view.findViewById(R.id.tv_private);
            tvFork = view.findViewById(R.id.tv_fork);

            view.findViewById(R.id.attributes).setOnClickListener(this);
            view.findViewById(R.id.scrollView).setOnTouchListener(this);
        }

        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvLanguage;
        private final TextView tvForks;
        private final TextView tvStars;
        private final TextView tvSize;
        private final TextView tvPrivate;
        private final TextView tvFork;

        @Override
        public void onClick(View v) {
            // Workaround to make it possible to open repositories when clicking inside of
            // attributes ScrollView
            itemView.performClick();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return false;
        }
    }
}
