package com.gh4a.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.Gist;

public class GistAdapter  extends RootAdapter<Gist> {

    public GistAdapter(Context context, List<Gist> objects) {
        super(context, objects);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_simple_3, null);

            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        Gist gist = mObjects.get(position);

        if (gist != null) {
            viewHolder.tvTitle.setText("Gist " + gist.getRepo());
            if (StringUtils.isBlank(gist.getDescription())) {
                viewHolder.tvDesc.setVisibility(View.GONE);
            }
            else {
                viewHolder.tvDesc.setText(gist.getDescription());
                viewHolder.tvDesc.setVisibility(View.VISIBLE);
            }
            String extra = v.getResources().getString(R.string.more_data,
                    pt.format(gist.getCreatedAt()), gist.getFiles().size() + " files");
            viewHolder.tvExtra.setText(extra);
        }
        return v;
    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {

        /** The tv title. */
        public TextView tvTitle;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvExtra;

    }
}
