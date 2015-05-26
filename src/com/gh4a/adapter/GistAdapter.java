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

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.User;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.StringUtils;

public class GistAdapter extends RootAdapter<Gist> {
    private String mOwnerLogin;

    public GistAdapter(Context context, String owner) {
        super(context);
        mOwnerLogin = owner;
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_gist, parent, false);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvCreator = (TextView) v.findViewById(R.id.tv_creator);
        viewHolder.tvTimestamp = (TextView) v.findViewById(R.id.tv_timestamp);
        viewHolder.tvSha = (TextView) v.findViewById(R.id.tv_sha);
        viewHolder.tvFiles = (TextView) v.findViewById(R.id.tv_files);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Gist gist) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        User user = gist.getUser();
        boolean isSelf = user != null && TextUtils.equals(user.getLogin(), mOwnerLogin);

        if (isSelf) {
            viewHolder.tvCreator.setVisibility(View.GONE);
        } else {
            viewHolder.tvCreator.setText(CommitUtils.getUserLogin(mContext, user));
            viewHolder.tvCreator.setVisibility(View.VISIBLE);
        }

        viewHolder.tvTimestamp.setText(
                StringUtils.formatRelativeTime(mContext, gist.getCreatedAt(), false));
        viewHolder.tvTitle.setText(TextUtils.isEmpty(gist.getDescription())
                ? mContext.getString(R.string.gist_no_description) : gist.getDescription());
        viewHolder.tvSha.setText(gist.getId());
        viewHolder.tvFiles.setText(String.valueOf(gist.getFiles().size()));
    }

    private static class ViewHolder {
        public TextView tvCreator;
        public TextView tvTimestamp;
        public TextView tvTitle;
        public TextView tvSha;
        public TextView tvFiles;
    }
}
