package com.gh4a.adapter.timeline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineBackgroundSpan;
import android.view.View;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.CommitComment;

class DiffViewHolder extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.Diff>
        implements View.OnClickListener {

    private final int mAddedLineColor;
    private final int mRemovedLineColor;
    private final int mAddedLineNumberColor;
    private final int mRemovedLineNumberColor;
    private final int mSecondaryTextColor;
    private final int mPadding;

    private final TextView mDiffHunkTextView;
    private final TextView mFileTextView;
    private final String mRepoOwner;
    private final String mRepoName;
    private final int mIssueNumber;

    public DiffViewHolder(View itemView, String repoOwner, String repoName, int issueNumber) {
        super(itemView);

        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;

        Context context = itemView.getContext();
        mAddedLineColor = ContextCompat.getColor(context, R.color.diff_add_light);
        mRemovedLineColor = ContextCompat.getColor(context, R.color.diff_remove_light);
        mAddedLineNumberColor = ContextCompat.getColor(context, R.color.diff_add_line_number_light);
        mRemovedLineNumberColor = ContextCompat.getColor(context, R.color.diff_remove_line_number_light);
        mSecondaryTextColor = UiUtils.resolveColor(context, android.R.attr.textColorSecondary);
        mPadding = context.getResources().getDimensionPixelSize(R.dimen.code_diff_padding);
        // TODO: Dark theme colors

        mDiffHunkTextView = (TextView) itemView.findViewById(R.id.diff_hunk);
        mFileTextView = (TextView) itemView.findViewById(R.id.tv_file);
        mFileTextView.setOnClickListener(this);
    }

    @Override
    public void bind(TimelineItem.Diff item) {
        TimelineItem.TimelineComment timelineComment = item.comments.get(0);
        CommitComment comment = (CommitComment) timelineComment.comment;

        boolean isOutdated = comment.getPosition() == -1;
        mFileTextView.setText(comment.getPath() + (isOutdated ? " (Outdated)" : ""));
        mFileTextView.setTag(timelineComment);

        mFileTextView.setClickable(!isOutdated);

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
        int start = Math.max(0, lines.length - 4);

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("-")) {
                leftLine += 1;
            } else if (lines[i].startsWith("+")) {
                rightLine += 1;
            } else {
                leftLine += 1;
                rightLine += 1;
            }

            if (i < start) {
                continue;
            }

            int spanStart = builder.length();

            String leftLineText = leftLine > 0 ? String.valueOf(leftLine) : "";
            appendLineNumber(builder, maxLineLength, leftLineText);

            String rightLineText = rightLine > 0 ? String.valueOf(rightLine) : "";
            appendLineNumber(builder, maxLineLength, rightLineText);

            // Add additional padding between line numbers and code
            builder.append("  ");

            int lineNumberLength = builder.length() - spanStart;

            builder.setSpan(new ForegroundColorSpan(mSecondaryTextColor), spanStart,
                    builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            builder.append("  ").append(lines[i]).append("  ");
            if (i < lines.length - 1) {
                builder.append("\n");
            }

            int color = Color.WHITE;
            int lineNumberColor = Color.WHITE;
            if (lines[i].startsWith("+")) {
                color = mAddedLineColor;
                lineNumberColor = mAddedLineNumberColor;
            } else if (lines[i].startsWith("-")) {
                color = mRemovedLineColor;
                lineNumberColor = mRemovedLineNumberColor;
            }

            DiffLineSpan span = new DiffLineSpan(color, lineNumberColor, mPadding, i == start,
                    i == lines.length - 1, lineNumberLength);
            builder.setSpan(span, spanStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mDiffHunkTextView.setText(builder);
    }

    private void appendLineNumber(SpannableStringBuilder builder, int maxLength, String number) {
        // Add padding at the start of text
        builder.append("    ");

        // Right align the number if necessary
        for (int i = 0; i < maxLength - number.length(); i++) {
            builder.append("  ");
        }

        builder.append(number);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_file) {
            TimelineItem.TimelineComment timelineComment =
                    (TimelineItem.TimelineComment) v.getTag();
            CommitComment commitComment = (CommitComment) timelineComment.comment;

            // TODO: Get commit files
            v.getContext().startActivity(
                    PullRequestDiffViewerActivity.makeIntent(mContext,
                            mRepoOwner, mRepoName, mIssueNumber,
                            commitComment.getCommitId(), commitComment.getPath(),
                            timelineComment.file.getPatch(), null, commitComment.getPosition(),
                            -1, -1, false,
                            new IntentUtils.InitialCommentMarker(commitComment.getId()))
            );
        }
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
