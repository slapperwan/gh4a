package com.gh4a.adapter;

import org.eclipse.egit.github.core.Contributor;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

public class ContributorAdapter extends RootAdapter<Contributor> implements View.OnClickListener {
    public ContributorAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_gravatar_2, parent, false);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Contributor contributor) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        AvatarHandler.assignAvatar(viewHolder.ivGravatar,
                contributor.getId(), contributor.getAvatarUrl());
        viewHolder.ivGravatar.setTag(contributor);

        viewHolder.tvTitle.setText(StringUtils.formatName(contributor.getLogin(), contributor.getName()));
        viewHolder.tvExtra.setText(mContext.getResources().getQuantityString(R.plurals.contributor_extra_data,
                contributor.getContributions(), contributor.getContributions()));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Contributor contributor = (Contributor) v.getTag();
            Intent intent = IntentUtils.getUserActivityIntent(mContext,
                    contributor.getLogin(), contributor.getName());
            if (intent != null) {
                mContext.startActivity(intent);
            }
        }
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public ImageView ivGravatar;
        public TextView tvExtra;
    }
}