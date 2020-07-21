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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.Gist;
import com.meisolsson.githubsdk.model.User;

public class GistAdapter extends RootAdapter<Gist, GistAdapter.ViewHolder> {
    private final String mOwnerLogin;

    public GistAdapter(Context context, String owner) {
        super(context);
        mOwnerLogin = owner;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_gist, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Gist gist) {
        User user = gist.owner();
        boolean isSelf = user != null && TextUtils.equals(user.login(), mOwnerLogin);

        if (isSelf) {
            holder.tvCreator.setVisibility(View.GONE);
        } else {
            holder.tvCreator.setText(ApiHelpers.getUserLoginWithType(mContext, user));
            holder.tvCreator.setVisibility(View.VISIBLE);
        }

        holder.tvTimestamp.setText(
                StringUtils.formatRelativeTime(mContext, gist.createdAt(), false));
        holder.tvTitle.setText(TextUtils.isEmpty(gist.description())
                ? mContext.getString(R.string.gist_no_description) : gist.description());
        holder.tvSha.setText(gist.id());
        holder.tvFiles.setText(String.valueOf(gist.files().size()));
        holder.tvPrivate.setVisibility(gist.isPublic() ? View.GONE : View.VISIBLE);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_title);
            tvCreator = view.findViewById(R.id.tv_creator);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvSha = view.findViewById(R.id.tv_sha);
            tvFiles = view.findViewById(R.id.tv_files);
            tvPrivate = view.findViewById(R.id.tv_private);
        }

        private final TextView tvCreator;
        private final TextView tvTimestamp;
        private final TextView tvTitle;
        private final TextView tvSha;
        private final TextView tvFiles;
        private final TextView tvPrivate;
    }
}
