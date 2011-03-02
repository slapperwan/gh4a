package com.gh4a.adapter;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;

public class CompareAdapter extends RootAdapter<String[]> {

    public CompareAdapter(Context context, List<String[]> objects) {
        super(context, objects);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_gravatar_2, null);
            viewHolder = new ViewHolder();
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);

            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final String[] sha = mObjects.get(position);
        if (sha != null && sha.length > 0) {
            ImageDownloader.getInstance().download(
                    StringUtils.md5Hex(sha[1]), viewHolder.ivGravatar);
            if (!StringUtils.isBlank(sha[3])) {
                viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        /** Open user activity */
                        Gh4Application context = (Gh4Application) v.getContext()
                                .getApplicationContext();
                        context.openUserInfoActivity(v.getContext(), sha[3],
                                null);
                    }
                });
            }

            viewHolder.tvDesc.setText(sha[2]);

            Resources res = v.getResources();
            String extraData = String.format(res.getString(R.string.more_data), 
                    !StringUtils.isBlank(sha[3]) ? sha[3] : "", "Commit " + sha[0].substring(0, 7));

            viewHolder.tvExtra.setText(extraData);
        }
        return v;
    }
    
    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {
        
        /** The iv gravatar. */
        public ImageView ivGravatar;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvExtra;
    }

}
