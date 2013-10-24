package com.gh4a.adapter;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.R;

public class DiscussionCategoryAdapter extends RootAdapter<JSONObject> {

    public DiscussionCategoryAdapter(Context context) {
        super(context);
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

        JSONObject o = mObjects.get(position);
        if (o != null) {
            try {
                viewHolder.tvTitle.setText(o.getString("title"));
                viewHolder.tvDesc.setText(o.getString("desc"));
                viewHolder.tvExtra.setVisibility(View.GONE);
            }
            catch (JSONException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
            }
        }
        return v;
    }
    
    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;

    }
}
