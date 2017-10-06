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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.User;

public class UserAdapter extends RootAdapter<User, UserAdapter.ViewHolder> {
    public UserAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_user, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, User user) {
        AvatarHandler.assignAvatar(holder.ivGravatar, user);
        holder.ivGravatar.setTag(user);

        holder.tvTitle.setText(StringUtils.formatName(user.login(), user.name()));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            User user = (User) v.getTag();
            mContext.startActivity(UserActivity.makeIntent(mContext, user));
        } else {
            super.onClick(v);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            ivGravatar = view.findViewById(R.id.iv_gravatar);
            tvTitle = view.findViewById(R.id.tv_title);
        }

        private final TextView tvTitle;
        private final ImageView ivGravatar;
    }
}