package com.gh4a.adapter;

import static android.text.format.DateUtils.FORMAT_NUMERIC_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import org.eclipse.egit.github.core.Download;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

public class DownloadAdapter extends RootAdapter<Download> {
    public DownloadAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_simple_3, null);
        ViewHolder viewHolder = new ViewHolder();

        Typeface boldCondensed = Gh4Application.get(mContext).boldCondensed;

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvTitle.setTypeface(boldCondensed);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);

        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.tvExtra.setTextAppearance(mContext, R.style.default_text_micro);

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

        CharSequence created = DateUtils.getRelativeTimeSpanString(download.getCreatedAt().getTime(),
                System.currentTimeMillis(), MINUTE_IN_MILLIS,
                FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE);
        viewHolder.tvExtra.setText(mContext.getString(R.string.download_extradata,
                Formatter.formatFileSize(mContext, download.getSize()),
                download.getDownloadCount(), created));
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;
    }
}
