package com.gh4a.adapter.timeline;

import android.view.View;
import android.widget.Button;

import com.gh4a.R;
import com.gh4a.model.TimelineItem;

class ReplyViewHolder extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.Reply>
        implements View.OnClickListener {

    public interface Callback {
        long getSelectedCommentId();
        void reply(long replyToId);
    }

    private final Callback mCallback;

    private final Button mReplyButton;

    public ReplyViewHolder(View itemView, Callback callback) {
        super(itemView);

        mCallback = callback;

        mReplyButton = itemView.findViewById(R.id.btn_reply);
        mReplyButton.setOnClickListener(this);
    }

    @Override
    public void bind(TimelineItem.Reply item) {
        boolean selected = item.timelineComment.comment().id() == mCallback.getSelectedCommentId();
        mReplyButton.setTag(item.timelineComment);
        mReplyButton.setText(selected ? R.string.reply_selected : R.string.reply);
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.btn_reply) {
            TimelineItem.TimelineComment timelineComment =
                    (TimelineItem.TimelineComment) v.getTag();
            mCallback.reply(timelineComment.comment().id());
        }
    }
}
