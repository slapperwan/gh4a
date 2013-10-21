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

import org.eclipse.egit.github.core.RepositoryCommit;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.GravatarUtils;

public class CommitAdapter extends RootAdapter<RepositoryCommit> {
    
    private AQuery aq;
    
    public CommitAdapter(Context context, List<RepositoryCommit> objects) {
        super(context, objects);
        aq = new AQuery((BaseSherlockFragmentActivity) context);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;
        Gh4Application app = (Gh4Application) mContext.getApplicationContext();
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_commit, null);
            
            Typeface boldCondensed = app.boldCondensed;
            
            viewHolder = new ViewHolder();
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(boldCondensed);
            
            viewHolder.tvSha = (TextView) v.findViewById(R.id.tv_sha);
            viewHolder.tvSha.setTypeface(Typeface.MONOSPACE);
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);

            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final RepositoryCommit commit = mObjects.get(position);
        if (commit != null) {
            
            aq.recycle(convertView);
            aq.id(viewHolder.ivGravatar).image(GravatarUtils.getGravatarUrl(
                    CommitUtils.getAuthorGravatarId(mContext, commit)),
                    true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);
            
            viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    Gh4Application context = (Gh4Application)
                            v.getContext().getApplicationContext();
                    if (CommitUtils.getAuthorLogin(context, commit) != null) {
                        context.openUserInfoActivity(context,
                                CommitUtils.getAuthorLogin(context, commit), null);
                    }
                }
            });

            viewHolder.tvSha.setText(commit.getSha().substring(0, 10));
            viewHolder.tvDesc.setText(commit.getCommit().getMessage());

            Resources res = v.getResources();
            String extraData = String.format(res.getString(R.string.more_commit_data),
                    CommitUtils.getAuthorName(mContext, commit),
                    Gh4Application.pt.format(commit.getCommit().getAuthor().getDate()));

            viewHolder.tvExtra.setText(extraData);
        }
        return v;
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        public TextView tvSha;
    }
}