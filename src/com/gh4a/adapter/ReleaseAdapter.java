package com.gh4a.adapter;

import static android.text.format.DateUtils.FORMAT_NUMERIC_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import org.eclipse.egit.github.core.Release;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

public class ReleaseAdapter extends RootAdapter<Release> {
    public ReleaseAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_release, null);
        Gh4Application app = Gh4Application.get(mContext);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvTitle.setTypeface(app.condensed);

        viewHolder.tvType = (TextView) v.findViewById(R.id.tv_type);
        viewHolder.tvCreatedAt  = (TextView) v.findViewById(R.id.tv_created_at);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Release release) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        viewHolder.tvTitle.setText(release.getName());
        viewHolder.tvType.setText(formatReleaseType(release));
        viewHolder.tvCreatedAt.setText(mContext.getString(R.string.download_created,
                StringUtils.formatRelativeTime(mContext, release.getCreatedAt(), true)));
    }

    private String formatReleaseType(Release release) {
        if (release.isDraft()) {
            return mContext.getString(R.string.release_type_draft);
        }
        if (release.isPreRelease()) {
            return mContext.getString(R.string.release_type_prerelease);
        }
        return mContext.getString(R.string.release_type_final);
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvType;
        public TextView tvCreatedAt;
    }
}