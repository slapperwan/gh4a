package com.gh4a.adapter;

<<<<<<< HEAD
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.holder.YourActionFeed;
import com.gh4a.utils.ImageDownloader;

public class YourActionsAdapter extends RootAdapter<YourActionFeed> {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
=======
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

>>>>>>> upstream/master
    public YourActionsAdapter(Context context, List<YourActionFeed> objects) {
        super(context, objects);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
<<<<<<< HEAD
            v = vi.inflate(R.layout.feed_row, null);

            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvCreatedAt = (TextView) v.findViewById(R.id.tv_created_at);
=======
            v = vi.inflate(R.layout.row_simple_3, null);

            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
>>>>>>> upstream/master
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        YourActionFeed entry = mObjects.get(position);
        if (entry != null) {

<<<<<<< HEAD
            ImageDownloader.getInstance().download(entry.getGravatarId(), viewHolder.ivGravatar);
            
            viewHolder.tvTitle.setText(entry.getTitle());
            String noHtmlString = entry.getContent().replaceAll("\\<.*?>","");
            noHtmlString = noHtmlString.replaceAll("[\\n]{2,}", "")
                    .replaceAll("[\r\n]{2,}", "")
                    .replaceAll("[\\s]{2,}", "\n").trim()
                    .replaceAll("[^\n][\\s]+$", "")
                    .replaceAll("&#47;", "/")
                    .replaceAll("&raquo;", "");
            viewHolder.tvDesc.setText(noHtmlString);
            try {
                viewHolder.tvCreatedAt.setText(pt.format(sdf.parse(entry.getPublished())));
            }
            catch (ParseException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
            }
=======
            viewHolder.tvTitle.setText(entry.getTitle());
            viewHolder.tvDesc.setText(Html.fromHtml(entry.getContent()));
            viewHolder.tvExtra.setText(entry.getAuthor());
>>>>>>> upstream/master
            
        }
        return v;
    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {
        
<<<<<<< HEAD
        /** The iv gravatar. */
        public ImageView ivGravatar;
        
=======
>>>>>>> upstream/master
        /** The tv title. */
        public TextView tvTitle;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
<<<<<<< HEAD
        public TextView tvCreatedAt;
=======
        public TextView tvExtra;
>>>>>>> upstream/master
    }
    

}
