package com.gh4a.adapter.timeline;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.User;

import java.util.HashSet;
import java.util.Set;

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
    private final TextView mMessageView;
    private final TextView mBodyView;
    private final TextView mDetailsView;
    private final Button mShowDetailsButton;
    private final View mAvatarContainer;
    private final ImageView ivMenu;
    private final PopupMenu mPopupMenu;

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

        mAvatarView = (ImageView) itemView.findViewById(R.id.iv_gravatar);
        mMessageView = (TextView) itemView.findViewById(R.id.tv_message);
        mBodyView = (TextView) itemView.findViewById(R.id.tv_desc);
        mDetailsView = (TextView) itemView.findViewById(R.id.tv_details);
        mShowDetailsButton = (Button) itemView.findViewById(R.id.btn_show_details);
        mShowDetailsButton.setOnClickListener(this);
        mAvatarContainer = itemView.findViewById(R.id.avatar_container);
        mAvatarContainer.setOnClickListener(this);
        ivMenu = (ImageView) itemView.findViewById(R.id.iv_menu);
        ivMenu.setOnClickListener(this);

        mPopupMenu = new PopupMenu(mContext, ivMenu);
        mPopupMenu.getMenuInflater().inflate(R.menu.review_menu, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(this);

        ImageView eventIconView = (ImageView) itemView.findViewById(R.id.iv_event_icon);
        // TODO: Eye icon
        int iconResId = UiUtils.resolveDrawable(mContext, R.attr.issueEventAssignedIcon);
        eventIconView.setImageResource(iconResId);
    }

    @Override
    public void bind(TimelineItem.TimelineReview item) {
        Review review = item.review;
        mShowDetailsButton.setTag(review);

        AvatarHandler.assignAvatar(mAvatarView, review.getUser());
        mAvatarContainer.setTag(review.getUser());

        formatTitle(review);

        if (!TextUtils.isEmpty(review.getBody())) {
            mImageGetter.bind(mBodyView, review.getBodyHtml(), review.getId());
            mBodyView.setVisibility(View.VISIBLE);
        } else {
            mBodyView.setVisibility(View.GONE);
        }

        if (mCallback.canQuote()) {
            mBodyView.setCustomSelectionActionModeCallback(
                    new UiUtils.QuoteActionModeCallback(mBodyView) {
                        @Override
                        public void onTextQuoted(CharSequence text) {
                            mCallback.quoteText(text);
                        }
                    });
        } else {
            mBodyView.setCustomSelectionActionModeCallback(null);
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

        ivMenu.setVisibility(mDisplayReviewDetails ? View.VISIBLE : View.GONE);
        ivMenu.setTag(review);
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
        switch (v.getId()) {
            case R.id.avatar_container: {
                User user = (User) v.getTag();
                Intent intent = UserActivity.makeIntent(mContext, user);
                if (intent != null) {
                    mContext.startActivity(intent);
                }
                break;
            }
            case R.id.btn_show_details:
                Review review = (Review) v.getTag();
                mContext.startActivity(ReviewActivity.makeIntent(mContext, mRepoOwner, mRepoName,
                        mIssueNumber, review, null));
                break;
            case R.id.iv_menu:
                mPopupMenu.show();
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Review review = (Review) ivMenu.getTag();

        switch (item.getItemId()) {
            case R.id.share:
                IntentUtils.share(mContext, "Pull Request #" + mIssueNumber + " - Review",
                        review.getHtmlUrl());
                return true;

            case R.id.browser:
                IntentUtils.launchBrowser(mContext, Uri.parse(review.getHtmlUrl()));
                return true;
        }
        return false;
    }
}
