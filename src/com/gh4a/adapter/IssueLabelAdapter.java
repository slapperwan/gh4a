package com.gh4a.adapter;

import org.eclipse.egit.github.core.Label;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;

public class IssueLabelAdapter extends RootAdapter<Label> implements OnClickListener {
    public IssueLabelAdapter(Context context) {
        super(context);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        final Label label = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.row_issue_label, parent, false);

            holder = new ViewHolder();
            holder.color = convertView.findViewById(R.id.view_color);
            holder.label = (TextView) convertView.findViewById(R.id.tv_title);
            holder.editor = (EditText) convertView.findViewById(R.id.et_label);
            convertView.setTag(holder);

            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            holder.label.setTypeface(app.condensed);

            ViewGroup colors = (ViewGroup) convertView.findViewById(R.id.colors);
            int count = colors.getChildCount();
            for (int i = 0; i < count; i++) {
                colors.getChildAt(i).setOnClickListener(this);
            }
            colors.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        assignColor(holder, label.getColor());

        holder.label.setText(label.getName());

        return convertView;
    }

    protected static class ViewHolder {
        public View color;
        public TextView label;
        public EditText editor;
    }

    private void assignColor(ViewHolder holder, String colorString) {
        int color = Color.parseColor("#" + colorString);
        boolean dark = Color.red(color) + Color.green(color) + Color.blue(color) < 383;

        holder.color.setBackgroundColor(color);
        holder.editor.setBackgroundColor(color);
        holder.editor.setTag(colorString);
        holder.editor.setTextColor(mContext.getResources().getColor(
                dark ? R.color.abs__primary_text_holo_dark : R.color.abs__primary_text_holo_light));
    }

    @Override
    public void onClick(View v) {
        ViewHolder holder = (ViewHolder) ((View) v.getParent()).getTag();
        assignColor(holder, (String) v.getTag());
    }
}