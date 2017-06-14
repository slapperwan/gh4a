package com.gh4a.adapter.timeline;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.User;

import java.util.HashSet;
import java.util.Set;

class ReviewViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineReview>
        implements View.OnClickListener {

    private final Context mContext;
    private final String mRepoOwner;
    private final String mRepoName;
    private final int mIssueNumber;
    private final boolean mIsPullRequest;
    private final boolean mDisplayReviewDetails;

    private final ImageView mAvatarView;
    private final TextView mMessageView;
    private final TextView mBodyView;
    private final TextView mDetailsView;
    private final Button mShowDetailsButton;
    private final View mAvatarContainer;

    public ReviewViewHolder(View itemView, String repoOwner, String repoName, int issueNumber,
            boolean isPullRequest, boolean displayReviewDetails) {
        super(itemView);

        mContext = itemView.getContext();
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
        mIsPullRequest = isPullRequest;
        mDisplayReviewDetails = displayReviewDetails;

        mAvatarView = (ImageView) itemView.findViewById(R.id.iv_gravatar);
        mMessageView = (TextView) itemView.findViewById(R.id.tv_message);
        mBodyView = (TextView) itemView.findViewById(R.id.tv_desc);
        mDetailsView = (TextView) itemView.findViewById(R.id.tv_details);
        mShowDetailsButton = (Button) itemView.findViewById(R.id.btn_show_details);
        mShowDetailsButton.setOnClickListener(this);
        mAvatarContainer = itemView.findViewById(R.id.avatar_container);
        mAvatarContainer.setOnClickListener(this);

        ImageView eventIconView = (ImageView) itemView.findViewById(R.id.iv_event_icon);
        // TODO: Eye icon
        int iconResId = UiUtils.resolveDrawable(mContext, R.attr.issueEventAssignedIcon);
        eventIconView.setImageResource(iconResId);
    }

    @Override
    public void bind(TimelineItem.TimelineReview item) {
        Review review = item.review;
        mShowDetailsButton.setTag(item);

        AvatarHandler.assignAvatar(mAvatarView, review.getUser());
        mAvatarContainer.setTag(review.getUser());

        formatTitle(review);

        if (!TextUtils.isEmpty(review.getBody())) {
            mBodyView.setText(review.getBody());
            mBodyView.setVisibility(View.VISIBLE);
        } else {
            mBodyView.setVisibility(View.GONE);
        }

        if (mDisplayReviewDetails && !item.getDiffHunks().isEmpty()) {
            StringBuilder builder = new StringBuilder("Code comments in ");
            Set<String> usedNames = new HashSet<>();

            boolean isOutdated = true;

            for (TimelineItem.Diff diff : item.getDiffHunks()) {
                CommitComment commitComment = diff.getInitialComment();

                if (commitComment.getPosition() != -1) {
                    isOutdated = false;
                }

                String filename = commitComment.getPath();

                if (!usedNames.contains(filename)) {
                    builder.append("\n").append(filename);
                    usedNames.add(filename);
                }
            }

            if (isOutdated) {
                builder.append("\n\nAll comments are outdated");
            }

            mDetailsView.setText(builder.toString());

            mDetailsView.setVisibility(View.VISIBLE);
            mShowDetailsButton.setVisibility(View.VISIBLE);
        } else {
            mDetailsView.setVisibility(View.GONE);
            mShowDetailsButton.setVisibility(View.GONE);
        }
    }

    private void formatTitle(Review review) {
        String login = review.getUser().getLogin();
        CharSequence time = review.getSubmittedAt() != null
                ? StringUtils.formatRelativeTime(mContext, review.getSubmittedAt(), true) : "";

        switch (review.getState()) {
            case Review.STATE_APPROVED:
                mMessageView.setText(login + " approved these changes " + time);
                break;
            case Review.STATE_CHANGES_REQUESTED:
                mMessageView.setText(login + " requested changes" + time);
                break;
            case Review.STATE_DISMISSED:
            case Review.STATE_COMMENTED:
                mMessageView.setText(login + " reviewed " + time);
                break;
            case Review.STATE_PENDING:
                mMessageView.setText(login + " started a review " + time);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.avatar_container) {
            User user = (User) v.getTag();
            Intent intent = UserActivity.makeIntent(mContext, user);
            if (intent != null) {
                mContext.startActivity(intent);
            }
        } else if (v.getId() == R.id.btn_show_details) {
            TimelineItem.TimelineReview review = (TimelineItem.TimelineReview) v.getTag();

            Intent intent = ReviewActivity.makeIntent(mContext, mRepoOwner, mRepoName,
                    mIssueNumber, mIsPullRequest, review);
            mContext.startActivity(intent);
        }
    }
}
