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

import org.eclipse.egit.github.core.CommitFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class ReviewViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineReview>
        implements View.OnClickListener {

    private final Context mContext;

    private final ImageView mAvatarView;
    private final TextView mMessageView;
    private final TextView mBodyView;
    private final TextView mDetailsView;
    private final Button mShowDetailsButton;

    public ReviewViewHolder(View itemView) {
        super(itemView);

        mContext = itemView.getContext();

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

        if (!item.chunks.isEmpty()) {
            String text = "Code comments in ";
            Set<String> usedNames = new HashSet<>();
            for (TimelineItem.Diff diff : item.chunks.values()) {
                if (!diff.comments.isEmpty()) {
                    CommitFile file = diff.comments.get(0).file;
                    if (file != null) {
                        String filename = file.getFilename();
                        if (!usedNames.contains(filename)) {
                            text += filename + ", ";
                            usedNames.add(filename);
                        }
                    }
                }
            }
            mDetailsView.setText(text);

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

        Collection<TimelineItem.Diff> chunks = review.chunks.values();
        mContext.startActivity(ReviewCommentsActivity.makeIntent(mContext, "Tunous", true,
                new ArrayList<>(chunks)));
//
//        StringBuilder builder = new StringBuilder();
//        for (TimelineItem.Diff chunk : review.chunks.values()) {
//            builder.append("\nDIFF\n\n");
//
//            int size = chunk.comments.size();
//            for (int i = 0; i < size; i++) {
//                TimelineItem.TimelineComment comment = chunk.comments.get(i);
//                builder.append(comment.comment.getBody());
//                if (i < size - 1) {
//                    builder.append("\n\n");
//                }
//            }
//        }
//        new AlertDialog.Builder(v.getContext())
//                .setMessage(builder.toString())
//                .setNegativeButton("Close", null)
//                .show();
    }
}
