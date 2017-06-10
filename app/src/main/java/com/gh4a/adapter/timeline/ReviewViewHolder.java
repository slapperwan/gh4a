package com.gh4a.adapter.timeline;

import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.CommitComment;

import java.util.List;

class ReviewViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineReview>
        implements View.OnClickListener {

    private final ImageView mAvatarView;
    private final TextView mMessageView;
    private final TextView mBodyView;
    private final TextView mDetailsView;
    private final Button mShowDetailsButton;

    public ReviewViewHolder(View itemView) {
        super(itemView);

        mAvatarView = (ImageView) itemView.findViewById(R.id.iv_gravatar);
        mMessageView = (TextView) itemView.findViewById(R.id.tv_message);
        mBodyView = (TextView) itemView.findViewById(R.id.tv_desc);
        mDetailsView = (TextView) itemView.findViewById(R.id.tv_details);
        mShowDetailsButton = (Button) itemView.findViewById(R.id.btn_show_details);
        mShowDetailsButton.setOnClickListener(this);
    }

    @Override
    public void bind(TimelineItem.TimelineReview item) {
        mShowDetailsButton.setTag(item);

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

        if (!item.comments.isEmpty()) {
            mDetailsView.setText(item.comments.size() + " comments");
            mDetailsView.setVisibility(View.VISIBLE);
            mShowDetailsButton.setVisibility(View.VISIBLE);
        } else {
            mDetailsView.setVisibility(View.GONE);
            mShowDetailsButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        TimelineItem.TimelineReview review = (TimelineItem.TimelineReview) v.getTag();

        StringBuilder builder = new StringBuilder();
        List<CommitComment> comments = review.comments;
        int size = comments.size();
        for (int i = 0; i < size; i++) {
            CommitComment comment = comments.get(i);
            builder.append(comment.getBody());
            if (i < size - 1) {
                builder.append("\n\n");
            }
        }
        new AlertDialog.Builder(v.getContext())
                .setMessage(builder.toString())
                .setNegativeButton("Close", null)
                .show();
    }
}
