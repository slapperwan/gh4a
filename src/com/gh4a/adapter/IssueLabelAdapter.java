package com.gh4a.adapter;

import org.eclipse.egit.github.core.Label;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.gh4a.ColorPickerDialog;
import com.gh4a.ColorPickerDialog.OnColorChangedListener;
import com.gh4a.utils.UiUtils;
import com.gh4a.Gh4Application;
import com.gh4a.R;

public class IssueLabelAdapter extends RootAdapter<Label> implements View.OnClickListener {
    public IssueLabelAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_issue_label, parent, false);
        ViewHolder holder = new ViewHolder();

        holder.color = v.findViewById(R.id.view_color);
        holder.label = (TextView) v.findViewById(R.id.tv_title);
        holder.editor = (EditText) v.findViewById(R.id.et_label);
        holder.collapsedContainer = v.findViewById(R.id.collapsed);
        holder.expandedContainer = v.findViewById(R.id.expanded);

        Gh4Application app = Gh4Application.get(mContext);
        holder.label.setTypeface(app.condensed);
        holder.editor.setTypeface(app.condensed);

        ViewGroup colors = (ViewGroup) v.findViewById(R.id.colors);
        int count = colors.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = colors.getChildAt(i);
            child.setOnClickListener(this);
            if (child.getId() == R.id.custom) {
                holder.customColorButton = (TextView) child;
            }
        }
        colors.setTag(holder);

        v.setTag(holder);
        return v;
    }

    @Override
    protected void bindView(View v, Label label) {
        ViewHolder holder = (ViewHolder) v.getTag();

        assignColor(holder, label.getColor());
        holder.label.setText(label.getName());
    }

    private void assignColor(ViewHolder holder, String colorString) {
        int color = Color.parseColor("#" + colorString);
        int textColor = UiUtils.textColorForBackground(mContext, color);

        holder.color.setBackgroundColor(color);
        holder.editor.setBackgroundColor(color);
        holder.customColorButton.setBackgroundColor(color);
        holder.customColorButton.setTextColor(textColor);
        holder.editor.setTextColor(textColor);
        holder.editor.setTag(colorString);
    }

    public void setExpanded(View v, boolean expanded) {
        ViewHolder holder = (ViewHolder) v.getTag();
        if (holder != null) {
            holder.collapsedContainer.setVisibility(expanded ? View.GONE : View.VISIBLE);
            holder.expandedContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (expanded) {
                holder.editor.requestFocus();
            }
        }
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

    protected static class ViewHolder {
        public View color;
        public TextView label;
        public EditText editor;
        public TextView customColorButton;
        public View collapsedContainer;
        public View expandedContainer;
    }
}