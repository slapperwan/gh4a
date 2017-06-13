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
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.StringUtils;
import com.vdurmont.emoji.EmojiParser;

import org.eclipse.egit.github.core.Repository;

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
        holder.tvTitle.setText(repository.getOwner().getLogin() + "/" + repository.getName());

        if (!StringUtils.isBlank(repository.getDescription())) {
            holder.tvDesc.setVisibility(View.VISIBLE);
            holder.tvDesc.setText(
                    EmojiParser.parseToUnicode(StringUtils.doTeaser(repository.getDescription())));
        } else {
            holder.tvDesc.setVisibility(View.GONE);
        }

        holder.tvLanguage.setText(repository.getLanguage() != null
                ? repository.getLanguage() : mContext.getString(R.string.unknown));
        holder.tvForks.setText(String.valueOf(repository.getForks()));
        holder.tvStars.setText(String.valueOf(repository.getWatchers()));
        holder.tvSize.setText(Formatter.formatFileSize(mContext, 1024L * repository.getSize()));
        holder.tvPrivate.setVisibility(repository.isPrivate() ? View.VISIBLE : View.GONE);
        holder.tvFork.setVisibility(repository.isFork() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected boolean isFiltered(CharSequence filter, Repository repo) {
        String lcFilter = filter.toString().toLowerCase(Locale.getDefault());
        String name = repo.getName().toLowerCase(Locale.getDefault());
        return name.contains(lcFilter);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnTouchListener {
        private ViewHolder(View view) {
            super(view);
            tvTitle = (TextView) view.findViewById(R.id.tv_title);
            tvDesc = (TextView) view.findViewById(R.id.tv_desc);
            tvLanguage = (TextView) view.findViewById(R.id.tv_language);
            tvForks = (TextView) view.findViewById(R.id.tv_forks);
            tvStars = (TextView) view.findViewById(R.id.tv_stars);
            tvSize = (TextView) view.findViewById(R.id.tv_size);
            tvPrivate = (TextView) view.findViewById(R.id.tv_private);
            tvFork = (TextView) view.findViewById(R.id.tv_fork);

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
