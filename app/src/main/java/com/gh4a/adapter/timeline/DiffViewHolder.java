package com.gh4a.adapter.timeline;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.LineBackgroundSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.CommitComment;

class DiffViewHolder extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.Diff>
        implements View.OnClickListener {
    private static final float[] DIFF_SIZE_MULTIPLIERS = new float[] {
            0.667F, 0.833F, 1F, 1.5F, 2F
    };

    private final int mAddedLineBackgroundColor;
    private final int mRemovedLineBackgroundColor;
    private final int mAddedLineNumberBackgroundColor;
    private final int mRemovedLineNumberBackgroundColor;
    private final int mSecondaryTextColor;
    private final int mDefaultBackgroundColor;
    private final int mDefaultLineNumberBackgroundColor;
    private final int mAccentColor;
    private final int mPadding;

    private final TextView mDiffHunkTextView;
    private final TextView mFileTextView;
    private final String mRepoOwner;
    private final String mRepoName;
    private final int mIssueNumber;
    private final float mInitialDiffTextSize;

    public DiffViewHolder(View itemView, String repoOwner, String repoName, int issueNumber) {
        super(itemView);

        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;

        Context context = itemView.getContext();
        mAddedLineBackgroundColor = UiUtils.resolveColor(context, R.attr.colorDiffAddBackground);
        mRemovedLineBackgroundColor = UiUtils.resolveColor(context, R.attr.colorDiffRemoveBackground);
        mAddedLineNumberBackgroundColor =
                UiUtils.resolveColor(context, R.attr.colorDiffAddLineNumberBackground);
        mRemovedLineNumberBackgroundColor =
                UiUtils.resolveColor(context, R.attr.colorDiffRemoveLineNumberBackground);
        mSecondaryTextColor = UiUtils.resolveColor(context, android.R.attr.textColorSecondary);
        mDefaultBackgroundColor = ContextCompat.getColor(context, R.color.diff_default_background);
        mDefaultLineNumberBackgroundColor =
                ContextCompat.getColor(context, R.color.diff_default_line_number_background);
        mAccentColor = UiUtils.resolveColor(context, R.attr.colorAccent);
        mPadding = context.getResources().getDimensionPixelSize(R.dimen.code_diff_padding);

        mDiffHunkTextView = itemView.findViewById(R.id.diff_hunk);
        mDiffHunkTextView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        mInitialDiffTextSize = mDiffHunkTextView.getTextSize();
        mFileTextView = itemView.findViewById(R.id.tv_file);
        mFileTextView.setOnClickListener(this);
    }

    @Override
    public void bind(TimelineItem.Diff item) {
        CommitComment comment = item.getInitialComment();

        mFileTextView.setTag(item.getInitialTimelineComment());
        mFileTextView.setText(comment.getPath());

        boolean isOutdated = comment.getPosition() == -1;
        mFileTextView.setPaintFlags(isOutdated
                ? mFileTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : mFileTextView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        mFileTextView.setClickable(!isOutdated);
        mFileTextView.setTextColor(isOutdated ? mSecondaryTextColor : mAccentColor);

        String[] lines = comment.getDiffHunk().split("\n");

        int leftLine = 0;
        int rightLine = 0;
        int[] lineNumbers = StringUtils.extractDiffHunkLineNumbers(lines[0]);
        if (lineNumbers != null) {
            leftLine = lineNumbers[0];
            rightLine = lineNumbers[1];
        }

        int maxLine = Math.max(rightLine, leftLine) + lines.length;
        int maxLineLength = String.valueOf(maxLine).length();

        SpannableStringBuilder builder = new SpannableStringBuilder();
        int start = Math.max(1, lines.length - 4);

        for (int i = 1; i < lines.length; i++) {
            boolean isLeftLine = false;
            boolean isRightLine = false;
            if (lines[i].startsWith("-")) {
                leftLine += 1;
                isLeftLine = true;
            } else if (lines[i].startsWith("+")) {
                rightLine += 1;
                isRightLine = true;
            } else {
                leftLine += 1;
                rightLine += 1;
            }

            if (i < start) {
                continue;
            }

            int spanStart = builder.length();

            String leftLineText = !isRightLine && leftLine > 0 ? String.valueOf(leftLine) : "";
            appendLineNumber(builder, maxLineLength, leftLineText, leftLine, item, false);

            String rightLineText = !isLeftLine && rightLine > 0 ? String.valueOf(rightLine) : "";
            appendLineNumber(builder, maxLineLength, rightLineText, rightLine, item, true);

            // Add additional padding between line numbers and code
            builder.append(" ");

            int lineNumberLength = builder.length() - spanStart;

            builder.append(" ").append(lines[i]).append(" ");
            if (i < lines.length - 1) {
                builder.append("\n");
            }

            int backgroundColor = mDefaultBackgroundColor;
            int lineNumberBackgroundColor = mDefaultLineNumberBackgroundColor;
            if (lines[i].startsWith("+")) {
                backgroundColor = mAddedLineBackgroundColor;
                lineNumberBackgroundColor = mAddedLineNumberBackgroundColor;
            } else if (lines[i].startsWith("-")) {
                backgroundColor = mRemovedLineBackgroundColor;
                lineNumberBackgroundColor = mRemovedLineNumberBackgroundColor;
            }

            DiffLineSpan span = new DiffLineSpan(backgroundColor, lineNumberBackgroundColor, mPadding, i == start,
                    i == lines.length - 1, lineNumberLength);
            builder.setSpan(span, spanStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mDiffHunkTextView.setText(builder);
        mDiffHunkTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                mInitialDiffTextSize * getDiffSizeMultiplier());
    }

    private float getDiffSizeMultiplier() {
        Context context = itemView.getContext();
        SharedPreferences prefs = context.getSharedPreferences(SettingsFragment.PREF_NAME,
                Context.MODE_PRIVATE);
        int textSizeSetting = prefs.getInt(SettingsFragment.KEY_TEXT_SIZE, 2);
        return textSizeSetting >= 0 && textSizeSetting < DIFF_SIZE_MULTIPLIERS.length
                ? DIFF_SIZE_MULTIPLIERS[textSizeSetting] : 1F;
    }

    private void appendLineNumber(SpannableStringBuilder builder, int maxLength, String numberText,
            final int number, final TimelineItem.Diff diff, final boolean isRightNumber) {
        int start = builder.length();

        // Add padding at the start of text
        builder.append("  ");

        // Right align the number if necessary
        for (int i = 0; i < maxLength - numberText.length(); i++) {
            builder.append(" ");
        }

        builder.append(numberText);

        if (!TextUtils.isEmpty(numberText)) {
            builder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    showPopupMenu(diff, number, isRightNumber);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setColor(mSecondaryTextColor);
                }
            }, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.tv_file) {
            TimelineItem.TimelineComment timelineComment =
                    (TimelineItem.TimelineComment) view.getTag();
            Intent intent = timelineComment.makeDiffIntent(mContext);

            if (intent != null) {
                view.getContext().startActivity(intent);
            }
        }
    }

    private String createUrl(TimelineItem.Diff diff, int line, boolean isRightLine) {
        CommitComment comment = diff.getInitialComment();
        return "https://github.com/" + mRepoOwner + "/" + mRepoName + "/pull/" + mIssueNumber +
                "#discussion-diff-" + comment.getId() + (isRightLine ? "R" : "L") + line;
    }

    private void showPopupMenu(final TimelineItem.Diff diff, final int line,
            final boolean isRightLine) {
        PopupMenu popupMenu = new PopupMenu(mContext, mDiffHunkTextView);

        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.review_diff_hunk_menu, menu);

        menu.findItem(R.id.view_in_file).setVisible(diff.getInitialComment().getPosition() != -1);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.share:
                        IntentUtils.share(mContext, "Line", createUrl(diff, line, isRightLine));
                        return true;
                    case R.id.view_in_file:
                        Intent intent = diff.getInitialTimelineComment()
                                .makeDiffIntent(mContext, line, isRightLine);
                        mContext.startActivity(intent);
                        return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private static class DiffLineSpan implements LineBackgroundSpan {
        private final int mBackgroundColor;
        private final int mLineNumberBackgroundColor;
        private final int mPadding;
        private final boolean mIsFirstLine;
        private final boolean mIsLastLine;
        private final int mLineNumberLength;

        public DiffLineSpan(int backgroundColor, int numberBackgroundColor, int padding,
                boolean isFirstLine, boolean isLastLine, int lineNumberLength) {
            super();
            mBackgroundColor = backgroundColor;
            mLineNumberBackgroundColor = numberBackgroundColor;
            mPadding = padding;
            mIsFirstLine = isFirstLine;
            mIsLastLine = isLastLine;
            mLineNumberLength = lineNumberLength;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline,
                int bottom, CharSequence text, int start, int end, int lnum) {
            final int paintColor = p.getColor();
            float width = p.measureText(text, start, start + mLineNumberLength);
            int bgTop = top - (mIsFirstLine ? mPadding : 0);
            int bgBottom = bottom + (mIsLastLine ? mPadding : 0);

            p.setColor(mLineNumberBackgroundColor);
            c.drawRect(left, bgTop, left + width, bgBottom, p);

            p.setColor(mBackgroundColor);
            c.drawRect(left + width, bgTop, right, bgBottom, p);

            p.setColor(paintColor);
        }
    }
}
