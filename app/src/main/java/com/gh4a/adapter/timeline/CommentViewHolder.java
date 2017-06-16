package com.gh4a.adapter.timeline;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.User;

import java.util.Date;

class CommentViewHolder
        extends TimelineItemAdapter.TimelineItemViewHolder<TimelineItem.TimelineComment>
        implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private final Context mContext;
    private final HttpImageGetter mImageGetter;
    private final Callback mCallback;
    private final String mRepoOwner;

    private final ImageView ivGravatar;
    private final StyleableTextView tvDesc;
    private final StyleableTextView tvExtra;
    private final TextView tvTimestamp;
    private final TextView tvEditTimestamp;
    private final ImageView ivMenu;
    private final PopupMenu mPopupMenu;

    public interface Callback {
        boolean canQuote();

        void quoteText(CharSequence text);

        boolean onMenItemClick(TimelineItem.TimelineComment comment, MenuItem menuItem);
    }

    public CommentViewHolder(View view, HttpImageGetter imageGetter, String repoOwner,
            Callback callback) {
        super(view);

        mContext = view.getContext();
        mImageGetter = imageGetter;
        mCallback = callback;
        mRepoOwner = repoOwner;

        ivGravatar = (ImageView) view.findViewById(R.id.iv_gravatar);
        tvDesc = (StyleableTextView) view.findViewById(R.id.tv_desc);
        tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        tvExtra = (StyleableTextView) view.findViewById(R.id.tv_extra);
        tvTimestamp = (TextView) view.findViewById(R.id.tv_timestamp);
        tvEditTimestamp = (TextView) view.findViewById(R.id.tv_edit_timestamp);
        ivMenu = (ImageView) view.findViewById(R.id.iv_menu);
        ivMenu.setOnClickListener(this);

        mPopupMenu = new PopupMenu(view.getContext(), ivMenu);
        mPopupMenu.getMenuInflater().inflate(R.menu.comment_menu, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(this);
    }

    @Override
    public void bind(TimelineItem.TimelineComment item) {
        User user = item.comment.getUser();
        Date createdAt = item.comment.getCreatedAt();
        Date updatedAt = item.comment.getUpdatedAt();

        AvatarHandler.assignAvatar(ivGravatar, user);
        ivGravatar.setTag(user);

        tvTimestamp.setText(StringUtils.formatRelativeTime(mContext, createdAt, true));
        if (createdAt.equals(updatedAt) || item.getCommitComment() != null) {
            // Unlike issue comments, the update timestamp for commit comments also changes
            // when e.g. the line number changes due to the diff the comment was made on
            // becoming outdated. As we can't distinguish those updates from comment body
            // updates, hide the edit timestamp for all commit comments.
            tvEditTimestamp.setVisibility(View.GONE);
        } else {
            tvEditTimestamp.setText(StringUtils.formatRelativeTime(mContext, updatedAt, true));
            tvEditTimestamp.setVisibility(View.VISIBLE);
        }

        // Body
        mImageGetter.bind(tvDesc, item.comment.getBodyHtml(), item.comment.getId());

        // Extra view
        String login = ApiHelpers.getUserLogin(mContext, item.comment.getUser());
        SpannableString userName = new SpannableString(login);
        userName.setSpan(new StyleSpan(Typeface.BOLD), 0, userName.length(), 0);
        tvExtra.setText(userName);

        if (mCallback.canQuote()) {
            tvDesc.setCustomSelectionActionModeCallback(
                    new UiUtils.QuoteActionModeCallback(tvDesc) {
                @Override
                public void onTextQuoted(CharSequence text) {
                    mCallback.quoteText(text);
                }
            });
        } else {
            tvDesc.setCustomSelectionActionModeCallback(null);
        }

        ivMenu.setTag(item);

        String ourLogin = Gh4Application.get().getAuthLogin();
        boolean canEdit = ApiHelpers.loginEquals(user, ourLogin) ||
                ApiHelpers.loginEquals(mRepoOwner, ourLogin);

        Menu menu = mPopupMenu.getMenu();
        menu.findItem(R.id.edit).setVisible(canEdit);
        menu.findItem(R.id.delete).setVisible(canEdit);
        menu.findItem(R.id.view_in_file).setVisible(
                item.file != null && item.getCommitComment() != null &&
                        item.getCommitComment().getPosition() != -1);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_menu) {
            mPopupMenu.show();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        TimelineItem.TimelineComment comment = (TimelineItem.TimelineComment) ivMenu.getTag();
        return mCallback.onMenItemClick(comment, menuItem);
    }
}
