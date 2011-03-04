package com.gh4a.adapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.holder.YourActionFeed;
import com.gh4a.utils.ImageDownloader;
import com.github.api.v2.schema.UserFeed;

public class YourActionsAdapter extends RootAdapter<YourActionFeed> {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    public YourActionsAdapter(Context context, List<YourActionFeed> objects) {
        super(context, objects);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.feed_row, null);

            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvCreatedAt = (TextView) v.findViewById(R.id.tv_created_at);
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        YourActionFeed entry = mObjects.get(position);
        if (entry != null) {

            ImageDownloader.getInstance().download(entry.getGravatarId(), viewHolder.ivGravatar);
            
            viewHolder.tvTitle.setText(entry.getTitle());
//            String noHtmlString = entry.getContent().replaceAll("\\<.*?>","");
//            noHtmlString = noHtmlString.replaceAll("[\\n]{2,}", "")
//                    .replaceAll("[\r\n]{2,}", "")
//                    .replaceAll("[\\s]{2,}", "\n").trim()
//                    .replaceAll("[^\n][\\s]+$", "")
//                    .replaceAll("&#47;", "/")
//                    .replaceAll("&raquo;", "");
//            
//            noHtmlString = formatDesc(entry.getEvent(), noHtmlString, (RelativeLayout) v);
//            
//            if (noHtmlString != null) {
//                viewHolder.tvDesc.setText(noHtmlString);
//            }
            String content = formatDesc(entry.getEvent(), entry.getContent(), (RelativeLayout) v);
            viewHolder.tvDesc.setText(content);
            try {
                viewHolder.tvCreatedAt.setText(pt.format(sdf.parse(entry.getPublished())));
            }
            catch (ParseException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
            }
        }
        return v;
    }

    private String formatDesc(String event, String desc, RelativeLayout baseView) {
        LinearLayout ll = (LinearLayout) baseView.findViewById(R.id.ll_push_desc);
        ll.removeAllViews();
        TextView generalDesc = (TextView) baseView.findViewById(R.id.tv_desc);
        ll.setVisibility(View.GONE);
        generalDesc.setVisibility(View.VISIBLE);
        
        if (UserFeed.Type.PUSH_EVENT.value().equals(event)) {
            generalDesc.setVisibility(View.GONE);
            ll.setVisibility(View.VISIBLE);
            
            String[] commitDesc = desc.split("\n");
            for (String str : commitDesc) {
                String[] part = str.split(" ");
                if (part[0].matches("[0-9a-zA-Z]{7}") || str.contains("more commits")) {//only start with sha
                    TextView tvCommitMsg = new TextView(baseView.getContext());
                    tvCommitMsg.setText(str.toString());
                    tvCommitMsg.setSingleLine(true);
                    tvCommitMsg.setTextAppearance(baseView.getContext(), R.style.default_text_medium);
                    ll.addView(tvCommitMsg);
                }
            }
            return null;
        }
        
        return desc;
    }
    
    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {
        
        /** The iv gravatar. */
        public ImageView ivGravatar;
        
        /** The tv title. */
        public TextView tvTitle;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvCreatedAt;
    }
    

}
