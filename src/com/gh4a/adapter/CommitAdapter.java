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

import java.util.Calendar;
import java.util.List;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.RepositoryCommit;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;

/**
 * The Commit adapter.
 */
public class CommitAdapter extends RootAdapter<RepositoryCommit> {

    /**
     * Instantiates a new commit adapter.
     * 
     * @param context the context
     * @param objects the objects
     */
    public CommitAdapter(Context context, List<RepositoryCommit> objects) {
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
        ViewHolder viewHolder;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_commit, null);
            viewHolder = new ViewHolder();
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvSha = (TextView) v.findViewById(R.id.tv_sha);

            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final RepositoryCommit commit = mObjects.get(position);
        if (commit != null) {
            if (!StringUtils.isBlank(commit.getAuthor().getLogin())) {
                ImageDownloader.getInstance().download(
                        commit.getAuthor().getGravatarId(), viewHolder.ivGravatar);
                viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        /** Open user activity */
                        Gh4Application context = (Gh4Application) v.getContext()
                                .getApplicationContext();
                        context.openUserInfoActivity(v.getContext(), commit.getAuthor().getLogin(),
                                null);
                    }
                });
            }
            else {
                ImageDownloader.getInstance().download(
                        StringUtils.md5Hex(commit.getCommitter().getEmail()), viewHolder.ivGravatar);
                viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        /** Open user activity */
                        Gh4Application context = (Gh4Application) v.getContext()
                                .getApplicationContext();
                        context.openUserInfoActivity(v.getContext(), commit.getCommitter().getLogin(),
                                null);
                    }
                });
            }

            viewHolder.tvSha.setText(commit.getSha().substring(0, 7));
            viewHolder.tvDesc.setText(commit.getCommit().getMessage());

            Calendar cal = Calendar.getInstance();
            cal.setTime(commit.getCommit().getCommitter().getDate());
            int timezoneOffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 3600000;
            cal.add(Calendar.HOUR, timezoneOffset);

            Resources res = v.getResources();
            String extraData = String.format(res.getString(R.string.more_data), !StringUtils
                    .isBlank(commit.getAuthor().getLogin()) ? commit.getAuthor().getLogin()
                    : commit.getCommitter().getLogin(), 
                    pt.format(cal.getTime()));

            viewHolder.tvExtra.setText(extraData);
        }
        return v;
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
        
        /** The sha. */
        public TextView tvSha;
    }
}