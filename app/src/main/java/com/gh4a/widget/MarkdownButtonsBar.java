package com.gh4a.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.gh4a.R;
import com.gh4a.utils.MarkdownUtils;

public class MarkdownButtonsBar extends FrameLayout implements View.OnClickListener,
        View.OnTouchListener {
    private EditText mEditText;
    private ToggleableBottomSheetBehavior mBottomSheetBehavior;

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
        initializeButtons(view, R.id.md_h1, R.id.md_h2, R.id.md_h3, R.id.md_bold, R.id.md_italic,
                R.id.md_strikethrough, R.id.md_bullet_list, R.id.md_number_list, R.id.md_task_list,
                R.id.md_divider, R.id.md_code, R.id.md_quote, R.id.md_link, R.id.md_image);
    }

    private void initializeButtons(View view, int... ids) {
        for (int id : ids) {
            View button = view.findViewById(id);
            button.setOnClickListener(this);
            button.setOnTouchListener(this);
        }
    }

    public void setEditText(EditText editText) {
        mEditText = editText;
    }

    public void setBottomSheetBehavior(ToggleableBottomSheetBehavior behavior) {
        mBottomSheetBehavior = behavior;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (mBottomSheetBehavior == null) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mBottomSheetBehavior.setEnabled(false);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mBottomSheetBehavior.setEnabled(true);
                break;
        }
        return false;
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
