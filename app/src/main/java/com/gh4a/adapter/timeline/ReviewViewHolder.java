package com.gh4a.adapter.timeline;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.PopupMenu;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;
import com.gh4a.widget.TimestampToastSpan;

import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.model.User;

import java.util.HashMap;
import java.util.Map;

class ReviewViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineReview>
        implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private final Context mContext;
    private final HttpImageGetter mImageGetter;
    private final String mRepoOwner;
    private final String mRepoName;
    private final int mIssueNumber;
    private final boolean mDisplayReviewDetails;
    private final Callback mCallback;

    private final ImageView mAvatarView;
    private final StyleableTextView mMessageView;
    private final TextView mBodyView;
    private final Button mShowDetailsButton;
    private final View mAvatarContainer;
    private final ImageView ivMenu;
    private final PopupMenu mPopupMenu;
    private final ViewGroup mDetailsContainer;
    private final View mDetailsDivider;
    private final View mDetailsHeader;
    private final ImageView mEventIconView;

    private final UiUtils.QuoteActionModeCallback mQuoteActionModeCallback;

    public interface Callback {
        boolean canQuote();

        void quoteText(CharSequence text);
    }

    public ReviewViewHolder(View itemView, HttpImageGetter imageGetter,
            String repoOwner, String repoName, int issueNumber,
            boolean displayReviewDetails, Callback callback) {
        super(itemView);

        mContext = itemView.getContext();
        mImageGetter = imageGetter;
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
        mDisplayReviewDetails = displayReviewDetails;
        mCallback = callback;

        mAvatarView = itemView.findViewById(R.id.iv_gravatar);
        mMessageView = itemView.findViewById(R.id.tv_message);
        mBodyView = itemView.findViewById(R.id.tv_desc);
        mShowDetailsButton = itemView.findViewById(R.id.btn_show_details);
        mShowDetailsButton.setOnClickListener(this);
        mAvatarContainer = itemView.findViewById(R.id.avatar_container);
        mAvatarContainer.setOnClickListener(this);
        ivMenu = itemView.findViewById(R.id.iv_menu);
        ivMenu.setOnClickListener(this);
        mDetailsContainer = itemView.findViewById(R.id.details_container);
        mDetailsDivider = itemView.findViewById(R.id.details_container_divider);
        mDetailsHeader = itemView.findViewById(R.id.details_container_header);

        mPopupMenu = new PopupMenu(mContext, ivMenu);
        mPopupMenu.getMenuInflater().inflate(R.menu.review_menu, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(this);

        mEventIconView = itemView.findViewById(R.id.iv_event_icon);
        mQuoteActionModeCallback = new UiUtils.QuoteActionModeCallback(mBodyView) {
            @Override
            public void onTextQuoted(CharSequence text) {
                mCallback.quoteText(text);
            }
        };
    }

    @Override
    public void bind(TimelineItem.TimelineReview item) {
        Review review = item.review();
        mShowDetailsButton.setTag(review);

        AvatarHandler.assignAvatar(mAvatarView, review.user());
        mAvatarContainer.setTag(review.user());

        formatTitle(review);

        boolean hasBody = !TextUtils.isEmpty(review.body());
        if (hasBody) {
            mImageGetter.bind(mBodyView, review.bodyHtml(), review.id());
            mBodyView.setVisibility(View.VISIBLE);
        } else {
            mBodyView.setVisibility(View.GONE);
        }

        if (mCallback.canQuote()) {
            mBodyView.setCustomSelectionActionModeCallback(mQuoteActionModeCallback);
        } else {
            mBodyView.setCustomSelectionActionModeCallback(null);
        }

        boolean hasDiffs = !item.getDiffHunks().isEmpty();
        if (mDisplayReviewDetails && hasDiffs) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            Map<String, FileDetails> files = new HashMap<>();

            int viewIndex = 0;
            for (TimelineItem.Diff diffHunk : item.getDiffHunks()) {
                ReviewComment commitComment = diffHunk.getInitialComment();
                String filename = commitComment.path();
                int commentCount = diffHunk.comments.size();
                boolean isOutdated = commitComment.position() == -1;

                if (files.containsKey(filename)) {
                    FileDetails details = files.get(filename);
                    details.isOutdated = details.isOutdated && isOutdated;
                    details.count += commentCount;
                    continue;
                }

                View row = mDetailsContainer.getChildAt(viewIndex);
                if (row == null) {
                    row = inflater.inflate(R.layout.row_timeline_review_file_details,
                            mDetailsContainer, false);
                    mDetailsContainer.addView(row);
                    row.setOnClickListener(this);
                }
                row.setTag(review);
                row.setTag(R.id.review_comment_id, commitComment.id());

                files.put(filename, new FileDetails(row, isOutdated, commentCount));

                viewIndex += 1;
            }

            for (Map.Entry<String, FileDetails> detailsEntry : files.entrySet()) {
                FileDetails fileDetails = detailsEntry.getValue();
                TextView tvFile = fileDetails.row.findViewById(R.id.tv_file);
                tvFile.setText("• " + detailsEntry.getKey());

                if (fileDetails.isOutdated) {
                    tvFile.setPaintFlags(tvFile.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    tvFile.setPaintFlags(tvFile.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                TextView tvFileComments =
                        fileDetails.row.findViewById(R.id.tv_file_comments);
                tvFileComments.setText(String.valueOf(fileDetails.count));

                fileDetails.row.setVisibility(View.VISIBLE);
            }

            for (int i = viewIndex; i < mDetailsContainer.getChildCount(); i++) {
                mDetailsContainer.getChildAt(i).setVisibility(View.GONE);
            }

            mDetailsContainer.setVisibility(View.VISIBLE);
            mShowDetailsButton.setVisibility(View.VISIBLE);
            mDetailsHeader.setVisibility(View.VISIBLE);
        } else {
            mDetailsContainer.setVisibility(View.GONE);
            mShowDetailsButton.setVisibility(View.GONE);
            mDetailsHeader.setVisibility(View.GONE);
        }

        if (hasBody && mDisplayReviewDetails && hasDiffs) {
            mDetailsDivider.setVisibility(View.VISIBLE);
        } else {
            mDetailsDivider.setVisibility(View.GONE);
        }

        ivMenu.setVisibility(mDisplayReviewDetails ? View.VISIBLE : View.GONE);
        ivMenu.setTag(review);

        mEventIconView.setImageResource(getEventIconResId(review));
    }

    @DrawableRes
    private int getEventIconResId(Review review) {
        @AttrRes int iconResAttr = R.attr.timelineEventReviewed;
        switch (review.state()) {
            case Approved:
                iconResAttr = R.attr.timelineEventApproved;
                break;
            case ChangesRequested:
                iconResAttr = R.attr.timelineEventRequestedChanges;
                break;
        }
        return UiUtils.resolveDrawable(mContext, iconResAttr);
    }

    private void formatTitle(final Review review) {
        int textResId;
        switch (review.state()) {
            case Approved:
                textResId = R.string.pull_request_event_review_approved;
                break;
            case ChangesRequested:
                textResId = R.string.pull_request_event_review_requested_changes;
                break;
            case Dismissed:
            case Commented:
            default:
                textResId = R.string.pull_request_event_review_reviewed;
                break;
            case Pending:
                textResId = R.string.pull_request_event_review_started_review;
                break;
        }

        String login = review.user().login();
        String textBase = mContext.getString(textResId, login);
        SpannableStringBuilder text = StringUtils.applyBoldTags(textBase,
                mMessageView.getTypefaceValue());

        CharSequence time = review.submittedAt() != null
                ? StringUtils.formatRelativeTime(mContext, review.submittedAt(), true) : "";

        int pos = text.toString().indexOf("[time]");
        if (pos >= 0) {
            text.replace(pos, pos + 6, time);
            if (review.submittedAt() != null) {
                text.setSpan(new TimestampToastSpan(review.submittedAt()), pos,
                        pos + time.length(), 0);
            }
        }
        mMessageView.setText(text);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.avatar_container: {
                User user = (User) v.getTag();
                Intent intent = UserActivity.makeIntent(mContext, user);
                if (intent != null) {
                    mContext.startActivity(intent);
                }
                break;
            }
            case R.id.btn_show_details: {
                Review review = (Review) v.getTag();
                mContext.startActivity(ReviewActivity.makeIntent(mContext, mRepoOwner, mRepoName,
                        mIssueNumber, review, null));
                break;
            }
            case R.id.iv_menu:
                mPopupMenu.show();
                break;
            case R.id.review_file_details: {
                Review review = (Review) v.getTag();
                long commentId = (long) v.getTag(R.id.review_comment_id);
                mContext.startActivity(ReviewActivity.makeIntent(mContext, mRepoOwner, mRepoName,
                        mIssueNumber, review, new IntentUtils.InitialCommentMarker(commentId)));
                break;
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Review review = (Review) ivMenu.getTag();

        switch (item.getItemId()) {
            case R.id.share:
                IntentUtils.share(mContext, "Pull Request #" + mIssueNumber + " - Review",
                        review.htmlUrl());
                return true;

            case R.id.browser:
                IntentUtils.launchBrowser(mContext, Uri.parse(review.htmlUrl()));
                return true;
        }
        return false;
    }

    private static class FileDetails {
        public final View row;
        public boolean isOutdated;
        public int count;

        public FileDetails(View row, boolean isOutdated, int count) {
            this.row = row;
            this.isOutdated = isOutdated;
            this.count = count;
        }
    }
}
