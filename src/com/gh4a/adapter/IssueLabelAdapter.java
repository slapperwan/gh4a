package com.gh4a.adapter;

import org.eclipse.egit.github.core.Label;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.gh4a.ColorPickerDialog;
import com.gh4a.ColorPickerDialog.OnColorChangedListener;
import com.gh4a.utils.TypefaceCache;
import com.gh4a.utils.UiUtils;
import com.gh4a.R;
import com.gh4a.widget.StyleableTextView;

public class IssueLabelAdapter extends RootAdapter<IssueLabelAdapter.EditableLabel> implements
        View.OnClickListener {
    public static class EditableLabel extends Label {
        public String editedName;
        public String editedColor;
        public final boolean newlyAdded;
        public boolean isEditing;

        public EditableLabel(String color) {
            super();
            newlyAdded = true;
            isEditing = true;
            editedColor = color;
        }
        public EditableLabel(Label label) {
            newlyAdded = false;
            isEditing = false;
            setColor(label.getColor());
            setName(label.getName());
            setUrl(label.getUrl());
        }
    }

    public IssueLabelAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_issue_label, parent, false);
        final ViewHolder holder = new ViewHolder();

        holder.color = v.findViewById(R.id.view_color);
        holder.label = (StyleableTextView) v.findViewById(R.id.tv_title);
        holder.editor = (EditText) v.findViewById(R.id.et_label);
        holder.collapsedContainer = v.findViewById(R.id.collapsed);
        holder.expandedContainer = v.findViewById(R.id.expanded);

        holder.editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (holder.lastAssignedLabel != null) {
                    holder.lastAssignedLabel.editedName = s.toString();
                }
            }
        });

        Typeface labelTf = TypefaceCache.getTypeface(mContext, holder.label.getTypefaceValue());
        holder.editor.setTypeface(labelTf);

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
    protected void bindView(View v, EditableLabel label) {
        ViewHolder holder = (ViewHolder) v.getTag();

        holder.lastAssignedLabel = label;

        holder.collapsedContainer.setVisibility(label.isEditing ? View.GONE : View.VISIBLE);
        holder.expandedContainer.setVisibility(label.isEditing ? View.VISIBLE : View.GONE);
        if (label.isEditing) {
            holder.editor.requestFocus();
        }

        if (label.newlyAdded) {
            holder.editor.setHint(R.string.issue_label_new);
        } else {
            holder.editor.setHint(null);
        }

        assignColor(holder, label.editedColor != null ? label.editedColor : label.getColor());
        holder.label.setText(label.getName());
        holder.editor.setText(label.editedName != null ? label.editedName : label.getName());
    }

    private void assignColor(ViewHolder holder, String colorString) {
        int color = Color.parseColor("#" + colorString);
        int textColor = UiUtils.textColorForBackground(mContext, color);

        holder.color.setBackgroundColor(color);
        holder.editor.setBackgroundColor(color);
        holder.customColorButton.setBackgroundColor(color);
        holder.customColorButton.setTextColor(textColor);
        holder.editor.setTextColor(textColor);

        holder.lastAssignedLabel.editedColor = colorString;
    }

    @Override
    public void onClick(View v) {
        final ViewHolder holder = (ViewHolder) ((View) v.getParent()).getTag();
        if (v == holder.customColorButton) {
            final String color = holder.lastAssignedLabel.editedColor;
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

    private static class ViewHolder {
        public EditableLabel lastAssignedLabel;

        public View color;
        public StyleableTextView label;
        public EditText editor;
        public TextView customColorButton;
        public View collapsedContainer;
        public View expandedContainer;
    }
}