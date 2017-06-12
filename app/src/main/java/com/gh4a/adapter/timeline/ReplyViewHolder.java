package com.gh4a.adapter.timeline;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.UiUtils;

class ReplyViewHolder extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.Reply>
        implements View.OnClickListener {

    public interface Callback {
        void reply(long replyToId, String text);
    }

    private final Callback mCallback;

    private final Button mReplyButton;

    public ReplyViewHolder(View itemView, Callback callback) {
        super(itemView);

        mCallback = callback;

        mReplyButton = (Button) itemView.findViewById(R.id.btn_reply);
        mReplyButton.setOnClickListener(this);
    }

    @Override
    public void bind(TimelineItem.Reply item) {
        mReplyButton.setTag(item.timelineComment);
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.btn_reply) {
            TimelineItem.TimelineComment timelineComment =
                    (TimelineItem.TimelineComment) v.getTag();

            final long replyToId = timelineComment.comment.getId();

            LayoutInflater inflater = LayoutInflater.from(v.getContext());
            View commentDialog = inflater.inflate(R.layout.commit_comment_dialog, null);

            final TextView code = (TextView) commentDialog.findViewById(R.id.line);
            code.setVisibility(View.GONE);

            final EditText body = (EditText) commentDialog.findViewById(R.id.body);

            final DialogInterface.OnClickListener saveCb = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = body.getText().toString();
                    mCallback.reply(replyToId, text);
                }
            };

            AlertDialog d = new AlertDialog.Builder(v.getContext())
                    .setCancelable(true)
                    .setTitle("Reply to review comments")
                    .setView(commentDialog)
                    .setPositiveButton(R.string.reply, saveCb)
                    .setNegativeButton(R.string.cancel, null)
                    .show();

            body.addTextChangedListener(new UiUtils.ButtonEnableTextWatcher(
                    body, d.getButton(DialogInterface.BUTTON_POSITIVE)));
        }
    }
}
