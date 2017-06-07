package com.gh4a.adapter.timeline;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;

class ReviewViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineReview> {

    private final ImageView mAvatarView;
    private final TextView mMessageView;
    private final TextView mBodyView;

    public ReviewViewHolder(View itemView) {
        super(itemView);

        mAvatarView = (ImageView) itemView.findViewById(R.id.iv_gravatar);
        mMessageView = (TextView) itemView.findViewById(R.id.tv_message);
        mBodyView = (TextView) itemView.findViewById(R.id.tv_desc);
    }

    @Override
    public void bind(TimelineItem.TimelineReview item) {
        AvatarHandler.assignAvatar(mAvatarView, item.review.getUser());
        mAvatarView.setTag(item.review.getUser());

        CharSequence time =
                StringUtils.formatRelativeTime(mContext, item.review.getSubmittedAt(), true);
        mMessageView.setText(item.review.getUser().getLogin() + " reviewed this " + time);

        if (!TextUtils.isEmpty(item.review.getBody())) {
            mBodyView.setText(item.review.getBody());
            mBodyView.setVisibility(View.VISIBLE);
        } else {
            mBodyView.setVisibility(View.GONE);
        }
    }
}
