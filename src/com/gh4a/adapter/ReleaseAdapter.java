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

public class ReleaseAdapter extends RootAdapter<Release> {
    public ReleaseAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_simple_3, null);
        Gh4Application app = Gh4Application.get(mContext);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvTitle.setTypeface(app.boldCondensed);

        v.findViewById(R.id.tv_desc).setVisibility(View.GONE);

        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.tvExtra.setTextAppearance(mContext, R.style.default_text_micro);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View v, Release release) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();

        viewHolder.tvTitle.setText(release.getName());

        CharSequence created = DateUtils.getRelativeTimeSpanString(release.getCreatedAt().getTime(),
                System.currentTimeMillis(), MINUTE_IN_MILLIS,
                FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE);
        viewHolder.tvExtra.setText(mContext.getString(R.string.release_extradata,
                formatReleaseType(release), created));
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
        public TextView tvExtra;
    }
}
