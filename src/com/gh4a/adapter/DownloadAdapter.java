package com.gh4a.adapter;

import org.eclipse.egit.github.core.Download;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.StringUtils;

public class DownloadAdapter extends RootAdapter<Download> {
    public DownloadAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_download, null);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvCreatedAt = (TextView) v.findViewById(R.id.tv_created_at);
        viewHolder.tvSize = (TextView) v.findViewById(R.id.tv_size);
        viewHolder.tvDownloads = (TextView) v.findViewById(R.id.tv_downloads);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Download download) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        viewHolder.tvTitle.setText(download.getName());
        if (!StringUtils.isBlank(download.getDescription())) {
            viewHolder.tvDesc.setVisibility(View.VISIBLE);
            viewHolder.tvDesc.setText(download.getDescription());
        } else {
            viewHolder.tvDesc.setVisibility(View.GONE);
        }

        viewHolder.tvCreatedAt.setText(mContext.getString(R.string.download_created,
                StringUtils.formatRelativeTime(mContext, download.getCreatedAt(), true)));
        viewHolder.tvSize.setText(Formatter.formatFileSize(mContext, download.getSize()));
        viewHolder.tvDownloads.setText(String.valueOf(download.getDownloadCount()));
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvSize;
        public TextView tvDownloads;
        public TextView tvCreatedAt;
    }
}
