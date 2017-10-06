package com.gh4a.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.ReleaseAsset;

public class ReleaseAssetAdapter extends RootAdapter<ReleaseAsset, ReleaseAssetAdapter.ViewHolder> {
    public ReleaseAssetAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_download, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, ReleaseAsset asset) {
        holder.tvTitle.setText(asset.name());
        if (!StringUtils.isBlank(asset.label())) {
            holder.tvDesc.setVisibility(View.VISIBLE);
            holder.tvDesc.setText(asset.label());
        } else {
            holder.tvDesc.setVisibility(View.GONE);
        }

        holder.tvCreatedAt.setText(mContext.getString(R.string.download_created,
                StringUtils.formatRelativeTime(mContext, asset.createdAt(), true)));
        holder.tvSize.setText(Formatter.formatFileSize(mContext, asset.size()));
        holder.tvDownloads.setText(String.valueOf(asset.downloadCount()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_title);
            tvDesc = view.findViewById(R.id.tv_desc);
            tvCreatedAt = view.findViewById(R.id.tv_created_at);
            tvSize = view.findViewById(R.id.tv_size);
            tvDownloads = view.findViewById(R.id.tv_downloads);
        }

        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvSize;
        private final TextView tvDownloads;
        private final TextView tvCreatedAt;
    }
}
