package com.gh4a.adapter;

import org.eclipse.egit.github.core.Contributor;

import android.content.Context;
import android.content.Intent;
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

public class ContributorAdapter extends RootAdapter<Contributor, ContributorAdapter.ViewHolder> {
    public ContributorAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_gravatar_twoline, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Contributor contributor) {
        AvatarHandler.assignAvatar(holder.ivGravatar, contributor.getLogin(),
                contributor.getId(), contributor.getAvatarUrl());
        holder.ivGravatar.setTag(contributor);

        holder.tvTitle.setText(StringUtils.formatName(contributor.getLogin(), contributor.getName()));
        holder.tvExtra.setText(mContext.getResources().getQuantityString(R.plurals.contributor_extra_data,
                contributor.getContributions(), contributor.getContributions()));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Contributor contributor = (Contributor) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext,
                    contributor.getLogin(), contributor.getName());
            if (intent != null) {
                mContext.startActivity(intent);
            }
        } else {
            super.onClick(v);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            ivGravatar = (ImageView) view.findViewById(R.id.iv_gravatar);
            tvTitle = (TextView) view.findViewById(R.id.tv_title);
            tvExtra = (TextView) view.findViewById(R.id.tv_extra);
        }

        private final TextView tvTitle;
        private final ImageView ivGravatar;
        private final TextView tvExtra;
    }
}