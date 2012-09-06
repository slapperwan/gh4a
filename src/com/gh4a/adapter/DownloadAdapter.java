package com.gh4a.adapter;

import static android.text.format.DateUtils.FORMAT_NUMERIC_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import java.util.List;

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

    public DownloadAdapter(Context context, List<Download> objects) {
        super(context, objects);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_simple_3, null);

            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            
            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvTitle.setTypeface(boldCondensed);
            
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.tvExtra.setTextAppearance(mContext, R.style.default_text_micro);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        Download download = mObjects.get(position);
        if (download != null) {

            viewHolder.tvTitle.setText(download.getName());
            if (!StringUtils.isBlank(download.getDescription())) {
                viewHolder.tvDesc.setVisibility(View.VISIBLE);
                viewHolder.tvDesc.setText(download.getDescription());
            }
            else {
                viewHolder.tvDesc.setVisibility(View.GONE);
            }

            long now = System.currentTimeMillis();
            
            String extraData = Formatter.formatFileSize(mContext, download.getSize())
                    + "  " + download.getDownloadCount() + " downloads"
                    + "  " + DateUtils.getRelativeTimeSpanString(download.getCreatedAt().getTime(), now,
                                MINUTE_IN_MILLIS, FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE);
            viewHolder.tvExtra.setText(extraData);
        }
        return v;
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;
    }
}
