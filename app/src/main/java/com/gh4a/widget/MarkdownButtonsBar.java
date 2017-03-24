package com.gh4a.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

import com.gh4a.R;
import com.gh4a.utils.MarkdownUtils;

public class MarkdownButtonsBar extends FrameLayout implements View.OnClickListener {
    private EditText mEditText;

    public MarkdownButtonsBar(Context context) {
        super(context);
        initialize();
    }

    public MarkdownButtonsBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public MarkdownButtonsBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        View view = View.inflate(getContext(), R.layout.markdown_buttons_bar, this);
        view.findViewById(R.id.md_h1).setOnClickListener(this);
        view.findViewById(R.id.md_h2).setOnClickListener(this);
        view.findViewById(R.id.md_h3).setOnClickListener(this);
        view.findViewById(R.id.md_bold).setOnClickListener(this);
        view.findViewById(R.id.md_italic).setOnClickListener(this);
        view.findViewById(R.id.md_strikethrough).setOnClickListener(this);
        view.findViewById(R.id.md_bullet_list).setOnClickListener(this);
        view.findViewById(R.id.md_number_list).setOnClickListener(this);
        view.findViewById(R.id.md_task_list).setOnClickListener(this);
        view.findViewById(R.id.md_divider).setOnClickListener(this);
        view.findViewById(R.id.md_code).setOnClickListener(this);
        view.findViewById(R.id.md_quote).setOnClickListener(this);
        view.findViewById(R.id.md_link).setOnClickListener(this);
        view.findViewById(R.id.md_image).setOnClickListener(this);
    }

    public void setEditText(EditText editText) {
        mEditText = editText;
    }

    @Override
    public void onClick(View v) {
        if (mEditText == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.md_h1:
                MarkdownUtils.addHeader(mEditText, 1);
                break;
            case R.id.md_h2:
                MarkdownUtils.addHeader(mEditText, 2);
                break;
            case R.id.md_h3:
                MarkdownUtils.addHeader(mEditText, 3);
                break;
            case R.id.md_bold:
                MarkdownUtils.addBold(mEditText);
                break;
            case R.id.md_italic:
                MarkdownUtils.addItalic(mEditText);
                break;
            case R.id.md_strikethrough:
                MarkdownUtils.addStrikeThrough(mEditText);
                break;
            case R.id.md_bullet_list:
                MarkdownUtils.addList(mEditText, MarkdownUtils.LIST_TYPE_BULLETS);
                break;
            case R.id.md_number_list:
                MarkdownUtils.addList(mEditText, MarkdownUtils.LIST_TYPE_NUMBERS);
                break;
            case R.id.md_task_list:
                MarkdownUtils.addList(mEditText, MarkdownUtils.LIST_TYPE_TASKS);
                break;
            case R.id.md_divider:
                MarkdownUtils.addDivider(mEditText);
                break;
            case R.id.md_code:
                MarkdownUtils.addCode(mEditText);
                break;
            case R.id.md_quote:
                MarkdownUtils.addQuote(mEditText);
                break;
            case R.id.md_link:
                MarkdownUtils.addLink(mEditText);
                break;
            case R.id.md_image:
                MarkdownUtils.addImage(mEditText);
                break;
        }
    }
}
