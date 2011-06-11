package com.gh4a.adapter;

import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.db.Bookmark;

public class BookmarkAdapter extends RootAdapter<Bookmark> {

    public BookmarkAdapter(Context context, List<Bookmark> objects) {
        super(context, objects);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_bookmark, null);
        }
        Bookmark bookmark = mObjects.get(position);
        if (bookmark != null) {
            TextView tvFormattedName = (TextView) v.findViewById(R.id.tv_title);
            ImageView buttonAdd = (ImageView) v.findViewById(R.id.iv_add);
            if (position == 0) {
                tvFormattedName.setText("Bookmark " + bookmark.getName());
                tvFormattedName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                tvFormattedName.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT );
                para.setMargins(0, 0, 0, 0); //left,top,right, bottom
                para.gravity = Gravity.CENTER_VERTICAL;
                tvFormattedName.setLayoutParams(para);
                buttonAdd.setVisibility(View.VISIBLE);
            }
            else {
                tvFormattedName.setText("[" + bookmark.getObjectType() + "] " + bookmark.getName());
                buttonAdd.setVisibility(View.GONE);
            }
        }
        return v;
    }

}
