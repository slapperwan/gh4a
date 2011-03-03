package com.gh4a.adapter;

import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.holder.YourActionFeed;

public class YourActionsAdapter extends RootAdapter<YourActionFeed> {

    public YourActionsAdapter(Context context, List<YourActionFeed> objects) {
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

        YourActionFeed entry = mObjects.get(position);
        if (entry != null) {

            viewHolder.tvTitle.setText(entry.getTitle());
            viewHolder.tvDesc.setText(Html.fromHtml(entry.getContent()));
            viewHolder.tvExtra.setText(entry.getAuthor());
            
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
