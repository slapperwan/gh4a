package com.gh4a.adapter.timeline;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.ReviewCommentsActivity;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.Review;

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

    private final ImageView mAvatarView;
    private final TextView mMessageView;
    private final TextView mBodyView;
    private final TextView mDetailsView;
    private final Button mShowDetailsButton;

    public ReviewViewHolder(View itemView, String repoOwner, String repoName, int issueNumber,
            boolean isPullRequest) {
        super(itemView);

        mContext = itemView.getContext();
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
        mIsPullRequest = isPullRequest;

        mAvatarView = (ImageView) itemView.findViewById(R.id.iv_gravatar);
        mMessageView = (TextView) itemView.findViewById(R.id.tv_message);
        mBodyView = (TextView) itemView.findViewById(R.id.tv_desc);
        mDetailsView = (TextView) itemView.findViewById(R.id.tv_details);
        mShowDetailsButton = (Button) itemView.findViewById(R.id.btn_show_details);
        mShowDetailsButton.setOnClickListener(this);
    }

    @Override
    public void bind(TimelineItem.TimelineReview item) {
        Review review = item.review;
        mShowDetailsButton.setTag(item);

        AvatarHandler.assignAvatar(mAvatarView, review.getUser());
        mAvatarView.setTag(review.getUser());

        formatTitle(review);

        if (!TextUtils.isEmpty(review.getBody())) {
            mBodyView.setText(review.getBody());
            mBodyView.setVisibility(View.VISIBLE);
        } else {
            mBodyView.setVisibility(View.GONE);
        }

        if (!item.chunks.isEmpty()) {
            StringBuilder builder = new StringBuilder("Code comments in ");
            Set<String> usedNames = new HashSet<>();

            boolean isOutdated = true;

            for (TimelineItem.Diff diff : item.chunks.values()) {
                TimelineItem.TimelineComment timelineComment = diff.getInitialTimelineComment();

                CommitComment commitComment = timelineComment.getCommitComment();
                if (commitComment != null && commitComment.getPosition() != -1) {
                    isOutdated = false;
                }
                CommitFile file = timelineComment.file;

                if (file != null) {
                    String filename = file.getFilename();
                    if (!usedNames.contains(filename)) {
                        builder.append(filename).append(", ");
                        usedNames.add(filename);
                    }
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
        TimelineItem.TimelineReview review = (TimelineItem.TimelineReview) v.getTag();

        mContext.startActivity(ReviewCommentsActivity.makeIntent(mContext, mRepoOwner, mRepoName,
                mIssueNumber, mIsPullRequest, review));
    }
}
