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

import com.gh4a.ColorPickerDialog;
import com.gh4a.ColorPickerDialog.OnColorChangedListener;
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
                View child = colors.getChildAt(i);
                child.setOnClickListener(this);
                if (child.getId() == R.id.custom) {
                    holder.customColorButton = (TextView) child;
                }
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
        public TextView customColorButton;
    }

    private void assignColor(ViewHolder holder, String colorString) {
        int color = Color.parseColor("#" + colorString);
        boolean dark = Color.red(color) + Color.green(color) + Color.blue(color) < 383;
        int textColor = mContext.getResources().getColor(
                dark ? R.color.abs__primary_text_holo_dark : R.color.abs__primary_text_holo_light);

        holder.color.setBackgroundColor(color);
        holder.editor.setBackgroundColor(color);
        holder.customColorButton.setBackgroundColor(color);
        holder.customColorButton.setTextColor(textColor);
        holder.editor.setTextColor(textColor);
        holder.editor.setTag(colorString);
    }

    @Override
    public void onClick(View v) {
        final ViewHolder holder = (ViewHolder) ((View) v.getParent()).getTag();
        if (v == holder.customColorButton) {
            String color = (String) holder.editor.getTag();
            ColorPickerDialog dialog = new ColorPickerDialog(mContext, color, new OnColorChangedListener() {
                @Override
                public void colorChanged(String color) {
                    assignColor(holder, color);
                }
            });
            dialog.show();
        } else {
            assignColor(holder, (String) v.getTag());
        }
    }
}