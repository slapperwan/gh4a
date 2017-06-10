package com.gh4a.adapter.timeline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LineBackgroundSpan;
import android.view.View;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.loader.TimelineItem;

import org.eclipse.egit.github.core.CommitComment;

class DiffViewHolder extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.Diff> {

    private final TextView mDiffHunkTextView;
    private final int mAddedLineColor;
    private final int mRemovedLineColor;
    private final int mPadding;

    public DiffViewHolder(View itemView) {
        super(itemView);

        mDiffHunkTextView = (TextView) itemView.findViewById(R.id.diff_hunk);

        Context context = itemView.getContext();
        mAddedLineColor = ContextCompat.getColor(context, R.color.diff_add_light);
        mRemovedLineColor = ContextCompat.getColor(context, R.color.diff_remove_light);
        mPadding = context.getResources().getDimensionPixelSize(R.dimen.code_diff_padding);
        // TODO: Dark theme colors
    }

    @Override
    public void bind(TimelineItem.Diff item) {
        TimelineItem.TimelineComment timelineComment = item.comments.get(0);
        CommitComment comment = (CommitComment) timelineComment.comment;

        String[] lines = comment.getDiffHunk().split("\n");

        SpannableStringBuilder builder = new SpannableStringBuilder();
        int start = Math.max(0, lines.length - 4);

        for (int i = start; i < lines.length; i++) {
            int spanStart = builder.length();

            // Append whitespace before and after line to fix padding. We can't use view padding
            // as it wouldn't be colored.
            builder.append("  ").append(lines[i]).append("  ");
            if (i < lines.length - 1) {
                builder.append("\n");
            }

            int spanEnd = builder.length();

            int color = Color.WHITE;
            if (lines[i].startsWith("+")) {
                color = mAddedLineColor;
            } else if (lines[i].startsWith("-")) {
                color = mRemovedLineColor;
            }

            PaddingCodeBlockSpan span = new PaddingCodeBlockSpan(color, mPadding, i == start,
                    i == lines.length - 1);
            builder.setSpan(span, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mDiffHunkTextView.setText(builder);
    }

    private static class PaddingCodeBlockSpan implements LineBackgroundSpan {
        private final int mBackgroundColor;
        private final int mPadding;
        private final boolean mIsFirstLine;
        private final boolean mIsLastLine;

        public PaddingCodeBlockSpan(int backgroundColor, int padding, boolean isFirstLine,
                boolean isLastLine) {
            super();
            mBackgroundColor = backgroundColor;
            mPadding = padding;
            mIsFirstLine = isFirstLine;
            mIsLastLine = isLastLine;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline,
                int bottom, CharSequence text, int start, int end, int lnum) {
            final int paintColor = p.getColor();
            p.setColor(mBackgroundColor);
            c.drawRect(left,
                    top - (mIsFirstLine ? mPadding : 0),
                    right,
                    bottom + (mIsLastLine ? mPadding : 0),
                    p);
            p.setColor(paintColor);
        }
    }
}
